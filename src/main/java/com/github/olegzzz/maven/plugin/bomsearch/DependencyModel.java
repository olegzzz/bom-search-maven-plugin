package com.github.olegzzz.maven.plugin.bomsearch;

import java.util.Objects;

/**
 * Lightweight (compared to {@link org.apache.maven.model.Model}) model.
 */
public class DependencyModel {

  public static final String GROUP_DELIMITER = ":";

  private final String group;
  private final String artifact;

  public DependencyModel(String group, String artifact) {
    this.group = group;
    this.artifact = artifact;
  }

  public String getGroup() {
    return group;
  }

  public String getArtifact() {
    return artifact;
  }

  @Override
  public String toString() {
    return String.format("%s%s%s", group, GROUP_DELIMITER, artifact);
  }

  public static DependencyModel fromString(String s) {
    String[] tokens = s.split(GROUP_DELIMITER);
    return new DependencyModel(tokens[0], tokens[1]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DependencyModel bomModel = (DependencyModel) o;

    if (!Objects.equals(group, bomModel.group)) {
      return false;
    }
    return Objects.equals(artifact, bomModel.artifact);
  }

  @Override
  public int hashCode() {
    int result = group != null ? group.hashCode() : 0;
    result = 31 * result + (artifact != null ? artifact.hashCode() : 0);
    return result;
  }
}
