package com.github.olegzzz.maven.plugin.bomsearch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Fail the build if there are BOM dependencies available but not used.
 */
@Mojo(name = "enforce", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class EnforceMojo extends SearchMojo {

  // TODO: support for excludes
  // TODO: support for version check

  @Parameter(property = "bomsearch.lenient", defaultValue = "false")
  private boolean lenient;

  @Override
  public void execute() throws MojoExecutionException {
    super.execute();

    if (skip) {
      return;
    }

    Map<ArtifactGroup, List<ArtifactId>> boms = Optional.ofNullable(readBomList()).orElse(
        Collections.emptyMap());
    if (!boms.isEmpty()) {
      final String msg =
          String.format("There are following BOMs available but not used: %s", flatten(boms));
      getLog().warn(msg);
      if (!lenient) {
        throw new MojoExecutionException(msg);
      }
    }
  }
}
