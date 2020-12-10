package olegzzz.github.io;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

public class SearchMojoIT {

  @Rule
  public MojoRule rule = new MojoRule();

  @Test
  public void test_config_params_override() throws Exception {
    File pom = new File("target/test-classes/project-configurations/config-override.xml");
    assertNotNull(pom);
    assertTrue(pom.exists());

    SearchMojo searcMojo = (SearchMojo) rule.lookupMojo("search", pom);
    assertNotNull(searcMojo);
    assertEquals(3, rule.getVariableValueFromObject(searcMojo, "minOccurrence"));
    assertEquals("https://foobar", rule.getVariableValueFromObject(searcMojo, "mavenRepoUrl"));
  }

}
