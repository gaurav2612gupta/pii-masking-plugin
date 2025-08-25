# PII Masking Plugin Demo Guide

## 🔐 Overview
This OpenSearch plugin automatically detects and masks PII (Personally Identifiable Information) in documents during indexing. It supports emails, SSNs, credit cards, and phone numbers with configurable masking rules and comprehensive audit logging.

## 🚀 Quick Start Demo

### Step 1: Install and Start OpenSearch with Plugin

```bash
# Build the plugin
./gradlew build

# The plugin zip will be in: build/distributions/pii-masking-*.zip
# Install it to your OpenSearch cluster:
# bin/opensearch-plugin install file:///path/to/pii-masking-*.zip

# Start OpenSearch cluster
# The plugin runs automatically as an ingest processor
```

### Step 2: Verify Plugin Installation

```bash
curl -X GET "localhost:9200/_cat/plugins?v&h=component"
```
**Expected Output:**
```
component
pii-masking
```

## 🎯 Core Demo Workflow

### Step 3: Create Ingest Pipeline

**IMPORTANT**: The plugin works as an ingest processor, so you need to create a pipeline first:

```bash
curl -X PUT "localhost:9200/_ingest/pipeline/pii-pipeline" \
-H 'Content-Type: application/json' \
-d '{
  "description": "Pipeline that masks PII in documents",
  "processors": [
    {
      "pii-masking": {}
    }
  ]
}'
```

**Expected Output:**
```json
{"acknowledged":true}
```

### Step 4: Test PII Detection with Pipeline

**Index a document with PII using the pipeline:**
```bash
curl -X POST "localhost:9200/logs/_doc?pipeline=pii-pipeline&pretty" \
-H 'Content-Type: application/json' \
-d '{
  "timestamp": "2025-08-25T10:00:00Z",
  "message": "Ashish is logging in as ashish.kumar@example.com, SSN 415-45-6789",
  "user": {
    "email": "ashish.kumar@example.com"
  }
}'
```

### Step 5: View Masked Results

**Search the indexed documents:**
```bash
curl -X GET "localhost:9200/logs/_search?pretty"
```

**Expected Output (PII Automatically Masked):**
```json
{
  "hits": {
    "hits": [{
      "_source": {
        "timestamp": "2025-08-25T10:00:00Z",
        "message": "Login from ****@example.com, SSN ***-**-****",
        "user": {
          "email": "****@example.com"
        }
      }
    }]
  }
}
```

### Step 6: Check Audit Logs

Audit information is logged to OpenSearch logs. Check your OpenSearch log files for entries like:

```
[2025-08-25T10:00:00Z] PII masked - Index: logs, DocId: xyz, Field: message, Type: email, Original: john.doe@example.com, Masked: ****@example.com
[2025-08-25T10:00:00Z] PII masked - Index: logs, DocId: xyz, Field: user.email, Type: email, Original: john.doe@example.com, Masked: ****@example.com
```

## ⚙️ Advanced Configuration

### Default PII Detection Rules

The plugin comes pre-configured with these patterns:

| PII Type | Pattern | Mask | Fields Checked |
|----------|---------|------|----------------|
| Email | `[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}` | `****@example.com` | `message`, `user.email`, `details` |
| SSN | `\b\d{3}-\d{2}-\d{4}\b` | `***-**-****` | `message`, `user.email`, `details` |
| Credit Card | `\b(?:\d[ -]*?){13,16}\b` | `****-****-****-****` | `message`, `user.email`, `details` |
| Phone | `\b\d{3}-\d{3}-\d{4}\b` | `***-***-****` | `message`, `user.email`, `details` |

### Test Different PII Types

**Email Detection:**
```bash
curl -X POST "localhost:9200/test/_doc?pipeline=pii-pipeline&pretty" \
-H 'Content-Type: application/json' \
-d '{
  "message": "Contact support at help@company.com for assistance"
}'
```

**SSN Detection:**
```bash
curl -X POST "localhost:9200/test/_doc?pipeline=pii-pipeline&pretty" \
-H 'Content-Type: application/json' \
-d '{
  "message": "Employee SSN: 555-12-3456 needs verification"
}'
```

**Credit Card Detection:**
```bash
curl -X POST "localhost:9200/test/_doc?pipeline=pii-pipeline&pretty" \
-H 'Content-Type: application/json' \
-d '{
  "message": "Payment processed with card 4532 1234 5678 9012"
}'
```

**Phone Number Detection:**
```bash
curl -X POST "localhost:9200/test/_doc?pipeline=pii-pipeline&pretty" \
-H 'Content-Type: application/json' \
-d '{
  "message": "Call us at 555-123-4567 for support"
}'
```

## 🧪 Test Edge Cases

### Multiple PII Types in One Document
```bash
curl -X POST "localhost:9200/mixed/_doc?pipeline=pii-pipeline&pretty" \
-H 'Content-Type: application/json' \
-d '{
  "message": "User john@example.com with SSN 123-45-6789 called 555-123-4567",
  "details": "Credit card 4532123456789012 on file"
}'
```

### No PII Detection
```bash
curl -X POST "localhost:9200/clean/_doc?pipeline=pii-pipeline&pretty" \
-H 'Content-Type: application/json' \
-d '{
  "message": "System startup completed successfully",
  "status": "healthy"
}'
```

## 📋 Demo Script for Presentations

### 1. **Setup Slide**: 
"I'll demonstrate a PII masking plugin that automatically protects sensitive data during indexing."

### 2. **Problem Slide**: 
"Show this raw document with PII:"
```json
{
  "message": "Login from john.doe@example.com, SSN 123-45-6789"
}
```

### 3. **Solution Slide**: 
"Create pipeline and index document with automatic masking:"
```bash
# Create pipeline first (if not already created)
curl -X PUT "localhost:9200/_ingest/pipeline/pii-pipeline" -H 'Content-Type: application/json' -d '{"processors":[{"pii-masking":{}}]}'

# Index with PII using pipeline
curl -X POST "localhost:9200/logs/_doc?pipeline=pii-pipeline" -H 'Content-Type: application/json' -d '{"message": "Login from john.doe@example.com, SSN 123-45-6789"}'

# Retrieve masked result
curl -X GET "localhost:9200/logs/_search?pretty"
```

### 4. **Result Slide**: 
"PII is automatically masked before storage:"
```json
{
  "message": "Login from ****@example.com, SSN ***-**-****"
}
```

### 5. **Audit Slide**: 
"All masking activities are logged for compliance."

## 🔧 Troubleshooting

### Plugin Not Working?
1. **Check plugin installation**: `curl localhost:9200/_cat/plugins`
2. **Check OpenSearch logs** for any plugin errors
3. **Verify document is going to correct fields**: The plugin only checks `message`, `user.email`, and `details` by default

### No Masking Occurring?
1. **Field names**: Make sure your data is in the monitored fields (`message`, `user.email`, `details`)
2. **Pattern matching**: The default patterns are strict - verify your data matches the regex
3. **Plugin enabled**: The plugin is enabled by default

### Performance Considerations
- The plugin processes every document during indexing
- Regex patterns are compiled once and cached for efficiency
- Impact is minimal for typical workloads

## 🎤 OpenSearchCon Talk Points

### Key Messages:
1. **"Zero Configuration Required"**: Works out of the box with common PII patterns
2. **"Automatic Protection"**: No application changes needed - works at the OpenSearch level
3. **"Compliance Ready"**: Built-in audit logging for regulatory requirements
4. **"Configurable & Extensible"**: Can be customized for specific organizational needs

### Demo Flow:
1. Start with the "horror story" - show raw PII data
2. Index the document → Show it gets automatically masked
3. Highlight audit logging for compliance
4. Show it works for multiple PII types
5. Emphasize zero application changes required

### Q&A Preparation:
- **Performance**: "Minimal overhead, regex patterns are pre-compiled"
- **False Positives**: "Patterns are designed to be conservative; can be fine-tuned"
- **Why not upstream**: "OpenSearch level provides universal protection regardless of application"
- **Strict mode**: "Can block documents entirely if PII detected (not enabled by default)"

## 🏗️ Architecture

The plugin implements an OpenSearch **Ingest Processor** that:
1. **Intercepts** every document during indexing
2. **Scans** configured fields for PII patterns
3. **Masks** detected PII with configurable replacement text
4. **Logs** all masking activities for audit trails
5. **Allows** the processed document to continue to indexing

This approach ensures **zero application changes** are required - protection happens automatically at the OpenSearch layer.

---

**Ready to protect your data!** 🛡️
