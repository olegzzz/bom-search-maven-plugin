package com.github.olegzzz.maven.plugin.bomsearch;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class JsoupDocumentLoaderTest {

  @Test
  public void returns_null_when_exception() {

    DocumentLoader loader = new JsoupDocumentLoader();
    assertNull(
        loader.loadGroupByUri(String.format("https://foobar%s.com", System.currentTimeMillis())));

  }

}