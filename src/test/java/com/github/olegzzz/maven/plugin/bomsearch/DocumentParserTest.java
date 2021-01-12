package com.github.olegzzz.maven.plugin.bomsearch;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(Enclosed.class)
public class DocumentParserTest {

  public static class SearchForBoms {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private DocumentLoader loader;

    @InjectMocks
    private DocumentParser parser;

    @Test
    public void returns_boms_If_available() {

      Elements elements = new Elements();
      Element element1 = new Element("a");
      element1.attr("title", "some-bom");
      element1.attr("href", "some-bom/");
      element1.val("some-bom");
      elements.add(element1);
      Document doc = mock(Document.class);
      when(doc.select(anyString())).thenReturn(elements);

      when(loader.loadGroupByUri(anyString())).thenReturn(doc);
      List<ArtifactId> boms = parser.parseArtifactsIds("foo/bar");

      assertTrue(boms.contains(new ArtifactId("some-bom")));

    }

    @Test
    public void returns_empty_list_If_no_boms_available() {
      Elements elements = new Elements();
      Element element1 = new Element("<a title=\"dependency\" href=\"dependency/\">dependency</a>");
      elements.add(element1);
      Document doc = mock(Document.class);
      when(doc.select(anyString())).thenReturn(elements);

      List<ArtifactId> boms = parser.parseArtifactsIds("foo/bar");

      assertTrue(boms.isEmpty());
    }
  }

}