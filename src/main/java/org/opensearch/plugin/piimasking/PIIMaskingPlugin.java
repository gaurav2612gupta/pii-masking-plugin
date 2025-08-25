/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.piimasking;

import org.opensearch.ingest.Processor;
import org.opensearch.plugin.piimasking.processor.PIIMaskingProcessor;
import org.opensearch.plugins.IngestPlugin;
import org.opensearch.plugins.Plugin;

import java.util.Collections;
import java.util.Map;

/**
 * PII Masking Plugin for OpenSearch
 * 
 * This plugin provides PII detection and masking capabilities for documents
 * during ingestion, along with comprehensive audit logging.
 * 
 * Core Features:
 * - Automatic PII detection using regex patterns
 * - Document masking before indexing  
 * - Audit logging of all PII masking activities
 * - Support for emails, SSNs, credit cards, phone numbers
 */
public class PIIMaskingPlugin extends Plugin implements IngestPlugin {
    
    public static final String PROCESSOR_TYPE = "pii-masking";
    
    @Override
    public Map<String, Processor.Factory> getProcessors(Processor.Parameters parameters) {
        return Collections.singletonMap(PROCESSOR_TYPE, new PIIMaskingProcessor.Factory(parameters));
    }
}
