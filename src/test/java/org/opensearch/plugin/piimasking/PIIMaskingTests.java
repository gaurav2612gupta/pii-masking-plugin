/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.piimasking;

import org.opensearch.plugin.piimasking.detector.PIIDetector;
import org.opensearch.plugin.piimasking.config.PIIConfiguration;
import org.opensearch.test.OpenSearchTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PIIMaskingTests extends OpenSearchTestCase {
    
    public void testEmailDetection() {
        // Create configuration with email masking rule
        Map<String, PIIConfiguration.MaskingRule> rules = new HashMap<>();
        rules.put("email", new PIIConfiguration.MaskingRule(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", 
            "****@example.com"
        ));
        
        PIIConfiguration config = new PIIConfiguration(
            true, "audit-test", rules, List.of("message"), false
        );
        
        PIIDetector detector = new PIIDetector(config);
        
        // Test email detection and masking
        String testText = "Contact john.doe@example.com for more info";
        PIIDetector.PIIMaskingResult result = detector.detectAndMask(testText);
        
        assertTrue("Should detect PII", result.hasPII());
        assertEquals("Should mask email", "Contact ****@example.com for more info", result.getMaskedText());
        assertEquals("Should detect one email", 1, result.getDetections().size());
        assertEquals("Should identify email type", "email", result.getDetections().get(0).getType());
    }
    
    public void testSSNDetection() {
        // Create configuration with SSN masking rule
        Map<String, PIIConfiguration.MaskingRule> rules = new HashMap<>();
        rules.put("ssn", new PIIConfiguration.MaskingRule(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b", 
            "***-**-****"
        ));
        
        PIIConfiguration config = new PIIConfiguration(
            true, "audit-test", rules, List.of("message"), false
        );
        
        PIIDetector detector = new PIIDetector(config);
        
        // Test SSN detection and masking
        String testText = "SSN: 123-45-6789";
        PIIDetector.PIIMaskingResult result = detector.detectAndMask(testText);
        
        assertTrue("Should detect PII", result.hasPII());
        assertEquals("Should mask SSN", "SSN: ***-**-****", result.getMaskedText());
        assertEquals("Should detect one SSN", 1, result.getDetections().size());
        assertEquals("Should identify SSN type", "ssn", result.getDetections().get(0).getType());
    }
    
    public void testNoPIIDetection() {
        // Create configuration with email masking rule
        Map<String, PIIConfiguration.MaskingRule> rules = new HashMap<>();
        rules.put("email", new PIIConfiguration.MaskingRule(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", 
            "****@example.com"
        ));
        
        PIIConfiguration config = new PIIConfiguration(
            true, "audit-test", rules, List.of("message"), false
        );
        
        PIIDetector detector = new PIIDetector(config);
        
        // Test text with no PII
        String testText = "This is a normal message with no sensitive data";
        PIIDetector.PIIMaskingResult result = detector.detectAndMask(testText);
        
        assertFalse("Should not detect PII", result.hasPII());
        assertEquals("Should not change text", testText, result.getMaskedText());
        assertEquals("Should have no detections", 0, result.getDetections().size());
    }
}
