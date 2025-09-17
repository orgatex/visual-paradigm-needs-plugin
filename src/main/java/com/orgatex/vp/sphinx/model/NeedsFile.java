package com.orgatex.vp.sphinx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Root object for sphinx-needs JSON file format.
 */
@Data
public class NeedsFile {
    @JsonProperty("created")
    private String created;

    @JsonProperty("current_version")
    private String currentVersion = "1.0";

    @JsonProperty("project")
    private String project;

    @JsonProperty("versions")
    private Map<String, VersionData> versions = new HashMap<>();

    public void addVersion(String version, VersionData versionData) {
        versions.put(version, versionData);
    }

    @Data
    public static class VersionData {
        @JsonProperty("created")
        private String created;

        @JsonProperty("creator")
        private Creator creator;

        @JsonProperty("needs")
        private Map<String, Need> needs = new HashMap<>();

        @JsonProperty("needs_amount")
        private int needsAmount;

        public void addNeed(Need need) {
            needs.put(need.getId(), need);
            needsAmount = needs.size();
        }
    }

    @Data
    public static class Creator {
        @JsonProperty("name")
        private String name = "Visual Paradigm Sphinx-Needs Plugin";

        @JsonProperty("version")
        private String version = "1.0.0";
    }

    @Data
    public static class Need {
        @JsonProperty("id")
        private String id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("content")
        private String content;

        @JsonProperty("type")
        private String type;

        @JsonProperty("status")
        private String status;

        @JsonProperty("tags")
        private String tags; // Comma-separated string

        @JsonProperty("links")
        private String links = ""; // Comma-separated string

        // Custom link types for use case relationships
        @JsonProperty("extends")
        private String extendsLinks = ""; // Comma-separated string

        @JsonProperty("includes")
        private String includesLinks = ""; // Comma-separated string

        @JsonProperty("associates")
        private String associatesLinks = ""; // Comma-separated string

        @JsonProperty("priority")
        private String priority;

        // Additional VP-specific fields
        @JsonProperty("element_type")
        private String elementType;

        public Need(String id, String title, String type) {
            this.id = id;
            this.title = title;
            this.type = type;
        }

        public Need() {}
    }
}
