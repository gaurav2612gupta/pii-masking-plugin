/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.piimasking.config;

import org.opensearch.core.ParseField;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ConstructingObjectParser;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for PII masking rules and settings
 */
public class PIIConfiguration implements ToXContentObject, Writeable {
    
    private static final ParseField ENABLED = new ParseField("enabled");
    private static final ParseField AUDIT_INDEX = new ParseField("audit_index");
    private static final ParseField MASKING = new ParseField("masking");
    private static final ParseField FIELDS_TO_CHECK = new ParseField("fields_to_check");
    private static final ParseField STRICT_MODE = new ParseField("strict_mode");
    
    private final boolean enabled;
    private final String auditIndex;
    private final Map<String, MaskingRule> maskingRules;
    private final List<String> fieldsToCheck;
    private final boolean strictMode;
    
    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<PIIConfiguration, Void> PARSER = new ConstructingObjectParser<>(
        "pii_configuration",
        args -> new PIIConfiguration(
            args[0] != null ? (Boolean) args[0] : true,
            args[1] != null ? (String) args[1] : "pii-audit-log",
            args[2] != null ? (Map<String, MaskingRule>) args[2] : new HashMap<>(),
            args[3] != null ? (List<String>) args[3] : List.of("message"),
            args[4] != null ? (Boolean) args[4] : false
        )
    );
    
    static {
        PARSER.declareBoolean(ConstructingObjectParser.optionalConstructorArg(), ENABLED);
        PARSER.declareString(ConstructingObjectParser.optionalConstructorArg(), AUDIT_INDEX);
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), 
            (p, c) -> parseMaskingRules(p), MASKING);
        PARSER.declareStringArray(ConstructingObjectParser.optionalConstructorArg(), FIELDS_TO_CHECK);
        PARSER.declareBoolean(ConstructingObjectParser.optionalConstructorArg(), STRICT_MODE);
    }
    
    public PIIConfiguration(boolean enabled, String auditIndex, Map<String, MaskingRule> maskingRules, 
                           List<String> fieldsToCheck, boolean strictMode) {
        this.enabled = enabled;
        this.auditIndex = auditIndex;
        this.maskingRules = maskingRules;
        this.fieldsToCheck = fieldsToCheck;
        this.strictMode = strictMode;
    }
    
    public PIIConfiguration(StreamInput in) throws IOException {
        this.enabled = in.readBoolean();
        this.auditIndex = in.readString();
        this.maskingRules = in.readMap(StreamInput::readString, MaskingRule::new);
        this.fieldsToCheck = in.readStringList();
        this.strictMode = in.readBoolean();
    }
    
    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeBoolean(enabled);
        out.writeString(auditIndex);
        out.writeMap(maskingRules, StreamOutput::writeString, (o, rule) -> rule.writeTo(o));
        out.writeStringCollection(fieldsToCheck);
        out.writeBoolean(strictMode);
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(ENABLED.getPreferredName(), enabled);
        builder.field(AUDIT_INDEX.getPreferredName(), auditIndex);
        builder.startObject(MASKING.getPreferredName());
        for (Map.Entry<String, MaskingRule> entry : maskingRules.entrySet()) {
            builder.field(entry.getKey(), entry.getValue());
        }
        builder.endObject();
        builder.field(FIELDS_TO_CHECK.getPreferredName(), fieldsToCheck);
        builder.field(STRICT_MODE.getPreferredName(), strictMode);
        builder.endObject();
        return builder;
    }
    
    private static Map<String, MaskingRule> parseMaskingRules(XContentParser parser) throws IOException {
        Map<String, MaskingRule> rules = new HashMap<>();
        while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
            String key = parser.currentName();
            parser.nextToken();
            rules.put(key, MaskingRule.fromXContent(parser));
        }
        return rules;
    }
    
    // Getters
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getAuditIndex() {
        return auditIndex;
    }
    
    public Map<String, MaskingRule> getMaskingRules() {
        return maskingRules;
    }
    
    public List<String> getFieldsToCheck() {
        return fieldsToCheck;
    }
    
    public boolean isStrictMode() {
        return strictMode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PIIConfiguration that = (PIIConfiguration) o;
        return enabled == that.enabled &&
               strictMode == that.strictMode &&
               Objects.equals(auditIndex, that.auditIndex) &&
               Objects.equals(maskingRules, that.maskingRules) &&
               Objects.equals(fieldsToCheck, that.fieldsToCheck);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enabled, auditIndex, maskingRules, fieldsToCheck, strictMode);
    }
    
    /**
     * Represents a masking rule for a specific PII type
     */
    public static class MaskingRule implements ToXContentObject, Writeable {
        private static final ParseField PATTERN = new ParseField("pattern");
        private static final ParseField MASK = new ParseField("mask");
        
        public static final ConstructingObjectParser<MaskingRule, Void> PARSER = new ConstructingObjectParser<>(
            "masking_rule",
            args -> new MaskingRule((String) args[0], (String) args[1])
        );
        
        static {
            PARSER.declareString(ConstructingObjectParser.constructorArg(), PATTERN);
            PARSER.declareString(ConstructingObjectParser.constructorArg(), MASK);
        }
        
        private final String pattern;
        private final String mask;
        
        public MaskingRule(String pattern, String mask) {
            this.pattern = pattern;
            this.mask = mask;
        }
        
        public MaskingRule(StreamInput in) throws IOException {
            this.pattern = in.readString();
            this.mask = in.readString();
        }
        
        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(pattern);
            out.writeString(mask);
        }
        
        public static MaskingRule fromXContent(XContentParser parser) throws IOException {
            return PARSER.parse(parser, null);
        }
        
        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(PATTERN.getPreferredName(), pattern);
            builder.field(MASK.getPreferredName(), mask);
            builder.endObject();
            return builder;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public String getMask() {
            return mask;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MaskingRule that = (MaskingRule) o;
            return Objects.equals(pattern, that.pattern) && Objects.equals(mask, that.mask);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(pattern, mask);
        }
    }
}
