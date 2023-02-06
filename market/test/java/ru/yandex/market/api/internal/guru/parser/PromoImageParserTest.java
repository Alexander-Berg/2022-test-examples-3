package ru.yandex.market.api.internal.guru.parser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;

import java.io.StringReader;
import java.util.function.BiPredicate;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Created by vivg on 24.06.16.
 */
@WithContext
@WithMocks
public class PromoImageParserTest extends UnitTestBase {

    static class ImageMatcher extends ArgumentMatcher<Image> {
        private final String imageName;

        public ImageMatcher(String imageName) {
            this.imageName = imageName;
        }

        @Override
        public boolean matches(Object argument) {
            return (argument instanceof Image) && null != ((Image) argument).getUrl() &&
                ((Image) argument).getUrl().endsWith(imageName);
        }
    }

    private PromoImageParser parser, parser2;

    @Mock
    private BiPredicate<String, Image> selectorMock;

    @Before
    public void setUp() {
        parser = new PromoImageParser();
        parser2 = new PromoImageParser(selectorMock);
    }

    @Test
    public void shouldReturnNullWhenEmpty() throws Exception {
        parser.parseReader(new StringReader("<image />"));
        Assert.assertNull(parser.getParsed());
    }

    @Test
    public void shouldReturnNullWhenEmptyUrl() throws Exception {
        String xml =
            "<image>\n" +
                "  <width>791</width>\n" +
                "  <height>359</height>\n" +
                "  <url></url>\n" +
                "</image>";
        parser.parseReader(new StringReader(xml));
        Image image = parser.getParsed();
        Assert.assertNull(image);
    }

    @Test
    public void shouldReturnNullWhenEmptyUrlInThumbs() throws Exception {
        String xml =
            "<image>\n" +
                "  <thumbnails>\n" +
                "    <thumbnail>\n" +
                "      <width>2</width>\n" +
                "      <height>2</height>\n" +
                "      <ratios>\n" +
                "        <ratio id=\"1\"></ratio>\n" +
                "      </ratios>\n" +
                "    </thumbnail>\n" +
                "  </thumbnails>\n" +
                "</image>";
        parser.parseReader(new StringReader(xml));
        Image image = parser.getParsed();
        Assert.assertNull(image);
    }

    @Test
    public void shouldReturnImageWithNonEmptyUrlOnly() throws Exception {
        String xml =
            "<image>\n" +
                "  <url>//cs-ellpic.yandex.net/test.jpg</url>\n" +
                "</image>";
        parser.parseReader(new StringReader(xml));
        Image image = parser.getParsed();
        Assert.assertNotNull(image);
        Assert.assertEquals("http://cs-ellpic.yandex.net/test.jpg", image.getUrl());
    }

    @Test
    public void shouldReturnImageWithNonEmptyUrlInThumbsOnly() throws Exception {
        String xml =
            "<image>\n" +
                "  <thumbnails>\n" +
                "    <thumbnail>\n" +
                "      <ratios>\n" +
                "        <ratio id=\"1\">//cs-ellpic.yandex.net/test.jpg</ratio>\n" +
                "      </ratios>\n" +
                "    </thumbnail>\n" +
                "  </thumbnails>\n" +
                "</image>";
        parser.parseReader(new StringReader(xml));
        Image image = parser.getParsed();
        Assert.assertNotNull(image);
        Assert.assertEquals("http://cs-ellpic.yandex.net/test.jpg", image.getUrl());
    }

    @Test
    public void checkValuesFromImageTag() throws Exception {
        String xml =
            "<image>\n" +
                "  <width>791</width>\n" +
                "  <height>359</height>\n" +
                "  <url>//cs-ellpic.yandex.net/test.jpg</url>\n" +
                "</image>";
        parser.parseReader(new StringReader(xml));
        Image image = parser.getParsed();
        Assert.assertNotNull(image);
        Assert.assertEquals(791, image.getWidth());
        Assert.assertEquals(359, image.getHeight());
        Assert.assertEquals("http://cs-ellpic.yandex.net/test.jpg", image.getUrl());
    }

    @Test
    public void shouldReturnImageEvenWithZeroWH() throws Exception {
        String xml =
            "<image>\n" +
                "  <width>0</width>\n" +
                "  <height>0</height>\n" +
                "  <url>//cs-ellpic.yandex.net/test.jpg</url>\n" +
                "</image>";
        parser.parseReader(new StringReader(xml));
        Image image = parser.getParsed();
        Assert.assertNotNull(image);
        Assert.assertEquals(0, image.getWidth());
        Assert.assertEquals(0, image.getHeight());
        Assert.assertEquals("http://cs-ellpic.yandex.net/test.jpg", image.getUrl());
    }

    @Test
    public void shouldReturnBiggestImage() throws Exception {
        String xml =
            "<image>\n" +
                "  <width>1</width>\n" +
                "  <height>1</height>\n" +
                "  <url>//cs-ellpic.yandex.net/test.jpg</url>\n" +
                "  <thumbnails>\n" +
                "    <thumbnail>\n" +
                "      <width>2</width>\n" +
                "      <height>2</height>\n" +
                "      <ratios>\n" +
                "        <ratio id=\"1\">//cs-ellpic.yandex.net/test4.jpg</ratio>\n" +
                "        <ratio id=\"2\">//cs-ellpic.yandex.net/test16.jpg</ratio>\n" +
                "      </ratios>\n" +
                "    </thumbnail>\n" +
                "    <thumbnail>\n" +
                "      <width>4</width>\n" +
                "      <height>4</height>\n" +
                "      <ratios>\n" +
                "        <ratio id=\"1\">//cs-ellpic.yandex.net/test16-2.jpg</ratio>\n" +
                "      </ratios>\n" +
                "    </thumbnail>\n" +
                "  </thumbnails>\n" +
                "</image>";
        parser.parseReader(new StringReader(xml));
        Image image = parser.getParsed();
        Assert.assertNotNull(image);
        Assert.assertEquals(4, image.getWidth());
        Assert.assertEquals(4, image.getHeight());
        Assert.assertTrue("http://cs-ellpic.yandex.net/test16.jpg".equals(image.getUrl()) ||
            "http://cs-ellpic.yandex.net/test16-2.jpg".equals(image.getUrl()));
    }

    @Test
    public void shouldReturnSelectedImage() throws Exception {
        when(selectorMock.test(anyString(), argThat(new ImageMatcher("test.jpg")))).thenReturn(true);
        String xml =
            "<image>\n" +
                "  <width>4</width>\n" +
                "  <height>4</height>\n" +
                "  <url>//cs-ellpic.yandex.net/abcd.jpg</url>\n" +
                "  <thumbnails>\n" +
                "    <thumbnail>\n" +
                "      <width>1</width>\n" +
                "      <height>1</height>\n" +
                "      <ratios>\n" +
                "        <ratio id=\"1\">//cs-ellpic.yandex.net/test.jpg</ratio>\n" +
                "        <ratio id=\"2\">//cs-ellpic.yandex.net/test2.jpg</ratio>\n" +
                "      </ratios>\n" +
                "    </thumbnail>\n" +
                "  </thumbnails>\n" +
                "</image>";
        parser2.parseReader(new StringReader(xml));
        Image image = parser2.getParsed();
        Assert.assertNotNull(image);
        Assert.assertEquals(1, image.getWidth());
        Assert.assertEquals(1, image.getHeight());
        Assert.assertEquals("http://cs-ellpic.yandex.net/test.jpg", image.getUrl());
        Mockito.verify(selectorMock).test(eq("/image"), any());
        Mockito.verify(selectorMock, times(2)).test(endsWith("/ratio"), any());
    }
}
