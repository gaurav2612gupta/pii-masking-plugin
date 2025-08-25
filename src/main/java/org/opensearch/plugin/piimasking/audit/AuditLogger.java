/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.piimasking.audit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.plugin.piimasking.config.PIIConfiguration;
import org.opensearch.plugin.piimasking.detector.PIIDetector;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Handles audit logging for PII masking operations
 * Simplified version for initial implementation
 */
public class AuditLogger {
    
    private static final Logger logger = LogManager.getLogger(AuditLogger.class);
    private final PIIConfiguration configuration;
    
    public AuditLogger(PIIConfiguration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Log PII masking activities (simplified logging to system out for now)
     */
    public void logMaskingActivity(String indexName, String documentId, String fieldName,
                                   List<PIIDetector.PIIDetection> detections) {
        if (!configuration.isEnabled() || detections.isEmpty()) {
            return;
        }
        
        for (PIIDetector.PIIDetection detection : detections) {
            logAuditEntry(indexName, documentId, fieldName, detection.getType(), 
                         detection.getOriginalValue(), detection.getMaskedValue(), "masked");
        }
    }
    
    /**
     * Log document blocking activity (when strict mode blocks a document)
     */
    public void logBlockedDocument(String indexName, String documentId, List<String> piiTypes) {
        if (!configuration.isEnabled() || !configuration.isStrictMode()) {
            return;
        }
        
        logAuditEntry(indexName, documentId, "document", String.join(",", piiTypes), 
                     "document_blocked", "N/A", "blocked");
    }
    
    /**
     * Simple audit logging implementation
     */
    private void logAuditEntry(String indexName, String documentId, String fieldName, 
                              String piiType, String originalValue, String maskedValue, String action) {
        String auditEntry = String.format(Locale.ROOT,
            "[%s] PII %s - Index: %s, DocId: %s, Field: %s, Type: %s, Original: %s, Masked: %s",
            Instant.now().toString(), action, indexName, documentId, fieldName, 
            piiType, originalValue, maskedValue
        );
        logger.info(auditEntry);
        
        // TODO: Replace with proper OpenSearch indexing once client dependencies are resolved
    }
}
