/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.piimasking.processor;

import org.opensearch.ingest.AbstractProcessor;
import org.opensearch.ingest.IngestDocument;
import org.opensearch.ingest.Processor;
import org.opensearch.plugin.piimasking.audit.AuditLogger;
import org.opensearch.plugin.piimasking.config.PIIConfiguration;
import org.opensearch.plugin.piimasking.detector.PIIDetector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ingest processor that detects and masks PII in documents
 */
public class PIIMaskingProcessor extends AbstractProcessor {
    
    public static final String TYPE = "pii-masking";
    
    private static PIIConfiguration globalConfiguration = getDefaultConfiguration();
    private final PIIDetector detector;
    private final AuditLogger auditLogger;
    
    protected PIIMaskingProcessor(String tag, String description, PIIConfiguration configuration) {
        super(tag, description);
        this.detector = new PIIDetector(configuration);
        this.auditLogger = new AuditLogger(configuration);
    }
    
    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {
        if (!globalConfiguration.isEnabled()) {
            return ingestDocument;
        }
        
        String documentId = ingestDocument.getSourceAndMetadata().get("_id") != null ? 
            ingestDocument.getSourceAndMetadata().get("_id").toString() : "unknown";
        String indexName = ingestDocument.getSourceAndMetadata().get("_index") != null ?
            ingestDocument.getSourceAndMetadata().get("_index").toString() : "unknown";
        
        // Process configured fields for PII detection and masking
        for (String fieldPath : globalConfiguration.getFieldsToCheck()) {
            if (ingestDocument.hasField(fieldPath)) {
                Object fieldValue = ingestDocument.getFieldValue(fieldPath, Object.class);
                if (fieldValue instanceof String) {
                    String text = (String) fieldValue;
                    
                    // Detect and mask PII
                    PIIDetector.PIIMaskingResult result = detector.detectAndMask(text);
                    
                    // If strict mode and PII found, throw exception to block document
                    if (globalConfiguration.isStrictMode() && result.hasPII()) {
                        auditLogger.logBlockedDocument(indexName, documentId, 
                            result.getDetections().stream()
                                .map(PIIDetector.PIIDetection::getType)
                                .toList());
                        throw new IllegalArgumentException(
                            "Document contains PII and strict mode is enabled. Document blocked.");
                    }
                    
                    // Update document with masked content
                    if (result.hasPII()) {
                        ingestDocument.setFieldValue(fieldPath, result.getMaskedText());
                        
                        // Log the masking activity
                        auditLogger.logMaskingActivity(indexName, documentId, fieldPath, 
                            result.getDetections());
                    }
                }
            }
        }
        
        return ingestDocument;
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    public static void updateGlobalConfiguration(PIIConfiguration configuration) {
        globalConfiguration = configuration;
    }
    
    public static PIIConfiguration getGlobalConfiguration() {
        return globalConfiguration;
    }
    
    /**
     * Default configuration with common PII patterns
     */
    private static PIIConfiguration getDefaultConfiguration() {
        Map<String, PIIConfiguration.MaskingRule> defaultRules = new HashMap<>();
        defaultRules.put("email", new PIIConfiguration.MaskingRule(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "****@example.com"));
        defaultRules.put("ssn", new PIIConfiguration.MaskingRule(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b", "***-**-****"));
        defaultRules.put("credit_card", new PIIConfiguration.MaskingRule(
            "\\b(?:\\d[ -]*?){13,16}\\b", "****-****-****-****"));
        defaultRules.put("phone", new PIIConfiguration.MaskingRule(
            "\\b\\d{3}-\\d{3}-\\d{4}\\b", "***-***-****"));
        
        return new PIIConfiguration(
            true, // enabled
            "pii-audit-log", // audit index
            defaultRules,
            List.of("message", "user.email", "details"), // fields to check
            false // strict mode
        );
    }
    
    /**
     * Factory for creating PIIMaskingProcessor instances
     */
    public static final class Factory implements Processor.Factory {
        
        private final Processor.Parameters parameters;
        
        public Factory(Processor.Parameters parameters) {
            this.parameters = parameters;
        }
        
        @Override
        public Processor create(Map<String, Processor.Factory> registry, String processorTag,
                               String description, Map<String, Object> config) throws Exception {
            return new PIIMaskingProcessor(processorTag, description, globalConfiguration);
        }
    }
}
