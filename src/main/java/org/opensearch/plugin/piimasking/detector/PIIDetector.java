/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.piimasking.detector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.plugin.piimasking.config.PIIConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PII detector that uses regex patterns to identify and mask sensitive information
 */
public class PIIDetector {
    
    private static final Logger logger = LogManager.getLogger(PIIDetector.class);
    private final Map<String, Pattern> compiledPatterns;
    private final PIIConfiguration configuration;
    
    public PIIDetector(PIIConfiguration configuration) {
        this.configuration = configuration;
        this.compiledPatterns = compilePatterns(configuration.getMaskingRules());
    }
    
    /**
     * Compile regex patterns for efficient reuse
     */
    private Map<String, Pattern> compilePatterns(Map<String, PIIConfiguration.MaskingRule> rules) {
        Map<String, Pattern> patterns = new HashMap<>();
        for (Map.Entry<String, PIIConfiguration.MaskingRule> entry : rules.entrySet()) {
            try {
                patterns.put(entry.getKey(), Pattern.compile(entry.getValue().getPattern()));
            } catch (Exception e) {
                // Log invalid pattern but continue with other patterns
                logger.warn("Invalid regex pattern for {}: {}", entry.getKey(), e.getMessage());
            }
        }
        return patterns;
    }
    
    /**
     * Detect and mask PII in the given text
     */
    public PIIMaskingResult detectAndMask(String text) {
        if (text == null || text.isEmpty()) {
            return new PIIMaskingResult(text, List.of());
        }
        
        String maskedText = text;
        List<PIIDetection> detections = new ArrayList<>();
        
        for (Map.Entry<String, Pattern> entry : compiledPatterns.entrySet()) {
            String piiType = entry.getKey();
            Pattern pattern = entry.getValue();
            PIIConfiguration.MaskingRule rule = configuration.getMaskingRules().get(piiType);
            
            if (rule == null) {
                continue;
            }
            
            Matcher matcher = pattern.matcher(maskedText);
            StringBuffer sb = new StringBuffer();
            
            while (matcher.find()) {
                String originalValue = matcher.group();
                detections.add(new PIIDetection(piiType, originalValue, rule.getMask()));
                matcher.appendReplacement(sb, Matcher.quoteReplacement(rule.getMask()));
            }
            matcher.appendTail(sb);
            maskedText = sb.toString();
        }
        
        return new PIIMaskingResult(maskedText, detections);
    }
    
    /**
     * Check if text contains PII without masking (for strict mode)
     */
    public boolean containsPII(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (Pattern pattern : compiledPatterns.values()) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Result of PII detection and masking operation
     */
    public static class PIIMaskingResult {
        private final String maskedText;
        private final List<PIIDetection> detections;
        
        public PIIMaskingResult(String maskedText, List<PIIDetection> detections) {
            this.maskedText = maskedText;
            this.detections = detections;
        }
        
        public String getMaskedText() {
            return maskedText;
        }
        
        public List<PIIDetection> getDetections() {
            return detections;
        }
        
        public boolean hasPII() {
            return !detections.isEmpty();
        }
    }
    
    /**
     * Represents a single PII detection
     */
    public static class PIIDetection {
        private final String type;
        private final String originalValue;
        private final String maskedValue;
        
        public PIIDetection(String type, String originalValue, String maskedValue) {
            this.type = type;
            this.originalValue = originalValue;
            this.maskedValue = maskedValue;
        }
        
        public String getType() {
            return type;
        }
        
        public String getOriginalValue() {
            return originalValue;
        }
        
        public String getMaskedValue() {
            return maskedValue;
        }
    }
}
