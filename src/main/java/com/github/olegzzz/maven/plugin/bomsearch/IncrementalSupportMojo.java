package com.github.olegzzz.maven.plugin.bomsearch;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.incremental.IncrementalBuildHelper;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.apache.maven.shared.utils.io.FileUtils;

public class IncrementalSupportMojo extends AbstractMojo {

  public static final String BOM_LIST_FILENAME = "bomList.lst";

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;

  /**
   * Needed for storing the status for the incremental build support.
   */
  @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
  private MojoExecution mojoExecution;

  /**
   * Set this to 'true' to disable plugin.
   */
  @Parameter(property = "bomsearch.skip", defaultValue = "false")
  protected boolean skip;

  /**
   * The current build session instance.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter(defaultValue = "true", property = "bomsearch.incremental")
  protected boolean incremental;

  private IncrementalBuildHelper incBuildHelper;

  protected boolean isPomFilesChanged() throws MojoExecutionException {
    DirectoryScanner directoryScanner = incBuildHelper.getDirectoryScanner();
    directoryScanner.setBasedir(project.getBasedir());
    directoryScanner.setIncludes("**/pom.xml");

    return incBuildHelper.inputFileTreeChanged(directoryScanner);
  }

  /**
   * Writes data represented as an iterable into file with name <code>filename</code> using
   * <code>toString</code>.
   *
   * @param fileName name of the status file
   * @param data     data to be written
   */
  protected void writeStatusFile(String fileName, String data) {
    File filename;
    try {
      filename = new File(incBuildHelper.getMojoStatusDirectory(), fileName);
    } catch (Exception e) {
      String msg = "Unable to read status directory.";
      getLog().debug(msg, e);
      getLog().warn(msg);
      return;
    }

    try {
      FileUtils.fileWrite(filename.getAbsolutePath(), data);
    } catch (Exception e) {
      String msg = String.format("Unable to write status file '%s'.", fileName);
      getLog().debug(msg, e);
      getLog().warn(msg);
    }
  }

  @Nullable
  protected List<String> readStatusFile(String fileName) {
    File file;
    try {
      file = new File(incBuildHelper.getMojoStatusDirectory(), fileName);
    } catch (MojoExecutionException e) {
      String msg = "Unable to read status directory.";
      getLog().debug(msg, e);
      getLog().warn(msg);
      return null;
    }
    try {
      List<String> data = FileUtils.loadFile(file);
      if (data.isEmpty()) {
        return null;
      } else {
        return data;
      }
    } catch (IOException e) {
      String msg = String.format("Unable to read build status file '%s'.", fileName);
      getLog().warn(msg);
      getLog().debug(msg, e);
      return null;
    }
  }

  protected void writeBomList(Collection<DependencyModel> boms) {

    StringBuilder sb = new StringBuilder();
    for (DependencyModel d : boms) {
      sb.append(d.toString()).append(System.lineSeparator());
    }

    writeStatusFile(BOM_LIST_FILENAME, sb.toString());
  }

  @Nullable
  protected Collection<DependencyModel> readBomList() {
    List<String> list = readStatusFile(BOM_LIST_FILENAME);
    if (list == null) {
      return null;
    }

    return list
        .stream()
        .map(DependencyModel::fromString)
        .collect(toList());
  }

  @Override
  public void execute() throws MojoExecutionException {
    if (skip) {
      return;
    }
    if (incremental) {
      getLog().debug("Incremental build enabled.");
      if (incBuildHelper == null) {
        incBuildHelper = new IncrementalBuildHelper(mojoExecution, session);
      }
    } else {
      getLog().debug("Incremental build disabled.");
    }
  }

}
