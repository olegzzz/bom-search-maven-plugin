package com.github.olegzzz.maven.plugin.bomsearch;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractSearchMojo extends AbstractMojo {

  /**
   * Needed for storing the status for the incremental build support.
   */
  @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
  private MojoExecution mojoExecution;

  /**
   * The current build session instance.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;



}
