package com.orgatex.vp.sphinx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** Root object for sphinx-needs JSON file format. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
  @JsonIgnoreProperties(ignoreUnknown = true)
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
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Creator {
    @JsonProperty("name")
    private String name = "Visual Paradigm Sphinx-Needs Plugin";

    @JsonProperty("program")
    private String program;

    @JsonProperty("version")
    private String version = "1.0.0";
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
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
    private List<String> tags = new ArrayList<>();

    @JsonProperty("links")
    private List<String> links = new ArrayList<>();

    // Custom link types for use case relationships
    @JsonProperty("extends")
    private List<String> extendsLinks = new ArrayList<>();

    @JsonProperty("includes")
    private List<String> includesLinks = new ArrayList<>();

    @JsonProperty("associates")
    private List<String> associatesLinks = new ArrayList<>();

    @JsonProperty("priority")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String priority;

    // Additional VP-specific fields
    @JsonProperty("element_type")
    private String elementType;

    @JsonProperty("vp_model_id")
    private String vpModelId;

    public Need(String id, String title, String type) {
      this.id = id;
      this.title = title;
      this.type = type;
    }

    public Need() {}
  }
}
