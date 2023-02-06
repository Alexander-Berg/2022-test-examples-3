package ru.yandex.common.mining.bd;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.yandex.common.mining.bd.domex.DomExtractor;
import static ru.yandex.common.util.XmlUtils.nodeListToString;
import static ru.yandex.common.util.html.HtmlUtils.parseStream;

import java.io.FileInputStream;
import java.util.List;

/**
 * Created on 13:13:13 13.02.2007
 *
 * @author Eugene Kirpichov jkff@yandex-team.ru
 */
public class PageNumberDetectorTest {
    DomExtractorsSuggester domExtractorsSuggester = new PageNumberDetector();

    @Test
    public void testPageNumberDetectorOnPaged() throws Exception {
        Document doc = parseStream(getClass().getResourceAsStream("/pager/paged.html"));
        List<DomExtractor> extractors = domExtractorsSuggester.suggest(doc);
        assertEquals(1, extractors.size());
/*
        assertEquals("[<A href='page1.html'>2, <A href='page2.html'>3, <A href='page3.html'>4, <A href='page4.html'>5]",
            nodeListToString(extractors.get(0).extractElements(doc)));
            */
    }

    @Test
    public void testPageNumberDetectorOnSparsePaged() throws Exception {
        Document doc = parseStream(getClass().getResourceAsStream("/pager/sparse_paged.html"));
        List<DomExtractor> extractors = domExtractorsSuggester.suggest(doc);
        assertEquals(1, extractors.size());
/*
        assertEquals("[<A href='page1.html'>, <A href='page2.html'>, <A href='page3.html'>, <A href='page4.html'>]",
            nodeListToString(extractors.get(0).extractElements(doc)));
            */
    }

    @Test
    public void testPageNumberDetectorOnNonPaged() throws Exception {
        Document doc = parseStream(getClass().getResourceAsStream("/pager/decorated-tv.html"));
        List<DomExtractor> extractors = domExtractorsSuggester.suggest(doc);
        assertEquals(0, extractors.size());
    }

    @Test
    public void testPageNumberDetectorOnPageable() throws Exception {
        Document doc = parseStream(getClass().getResourceAsStream("/pager/paged.html"));
        List<DomExtractor> extractors = domExtractorsSuggester.suggest(doc);
        assertEquals(1, extractors.size());

        Document newDoc = parseStream(getClass().getResourceAsStream("/pager/paged_single_page.html"));

        assertEquals(0, extractors.get(0).extractElements(newDoc).size());
    }

    @Test
    public void testPageNumberDetectorOnSamsung() throws Exception {
        Document doc = parseStream(getClass().getResourceAsStream("/pager/samsung_lcd19.htm"));
        List<DomExtractor> extractors = domExtractorsSuggester.suggest(doc);
        assertEquals(1, extractors.size());
        assertEquals(2, extractors.get(0).extractElements(doc).size());
    }

    @Test
    public void testPageNumberDetectorOnFerraRu() throws Exception {
        Document doc = parseStream(getClass().getResourceAsStream("/pager/ferra.ru.html"));
        List<DomExtractor> extractors = domExtractorsSuggester.suggest(doc);
        //assertEquals(1, extractors.size());
        //assertEquals(7, extractors.get(0).extractElements(doc).size());
    }

    @Test
    public void testPageNumberDetectorOnStereoheadRu() throws Exception {
        Document doc = parseStream(getClass().getResourceAsStream("/pager/stereohead.ru.html"));
        List<DomExtractor> extractors = domExtractorsSuggester.suggest(doc);
        //assertEquals(1, extractors.size());
        //assertEquals(8, extractors.get(0).extractElements(doc).size());
    }
}
