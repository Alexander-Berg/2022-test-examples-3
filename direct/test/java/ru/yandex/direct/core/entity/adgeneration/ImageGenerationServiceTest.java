package ru.yandex.direct.core.entity.adgeneration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgeneration.model.ImageSuggest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.imagesearch.ImageSearchClient;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.richcontent.RichContentClient;
import ru.yandex.direct.richcontent.model.Image;
import ru.yandex.direct.richcontent.model.UrlInfo;
import ru.yandex.direct.validation.result.DefectId;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.adgeneration.model.GenerationDefectIds.BAD_CAMPAIGN_HREF;
import static ru.yandex.direct.core.entity.adgeneration.model.GenerationDefectIds.EMPTY_IMAGE_SEARCH_API_RESPONSE;
import static ru.yandex.direct.core.entity.adgeneration.model.GenerationDefectIds.IMAGE_SEARCH_API_WITHOUT_QUERY;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.BANNER_REGULAR_IMAGE_MIN_SIZE;
import static ru.yandex.direct.core.testing.data.TestImages.createImageDocs;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ImageGenerationServiceTest {

    private ImageGenerationService service;
    @Autowired
    private ImageSearchClient imageSearchClient;
    @Autowired
    private RichContentClient richContentClient;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private static final String URL = "http://example.com";

    @Before
    public void createService() {
        imageSearchClient = mock(ImageSearchClient.class);
        richContentClient = mock(RichContentClient.class);
        service = new ImageGenerationService(
                null, null, imageSearchClient, null, richContentClient, ppcPropertiesSupport);
    }

    @Test
    public void emptyResult() {
        when(imageSearchClient.getImagesByTextsAndDomain(anySet(), any(), anyInt())).thenReturn(emptyMap());
        when(imageSearchClient.getImagesByDomain(any(), anyInt())).thenReturn(emptyList());
        when(richContentClient.getUrlInfo(any())).thenReturn(null);
        Result<Collection<ImageSuggest>> result = service.generateImages(URL, "main", emptySet());
        assertNotNull(result.getResult());
        checkResult(result, new String[]{}, List.of(EMPTY_IMAGE_SEARCH_API_RESPONSE));
    }

    @Test
    public void errorResult() {
        when(imageSearchClient.getImagesByTextsAndDomain(anySet(), any(), anyInt())).thenReturn(emptyMap());
        when(imageSearchClient.getImagesByDomain(any(), anyInt())).thenReturn(emptyList());
        when(richContentClient.getUrlInfo(any())).thenReturn(null);
        Result<Collection<ImageSuggest>> result = service.generateImages("badURL", "main", emptySet());
        assertNull(result.getResult());
        assertThat(
                StreamEx.of(result.getErrors())
                        .map(defect -> defect.getDefect().defectId())
                        .toList(),
                contains(BAD_CAMPAIGN_HREF));
    }

    @Test
    public void badSizeRCA() {
        when(imageSearchClient.getImagesByTextsAndDomain(anySet(), any(), anyInt())).thenReturn(emptyMap());
        when(imageSearchClient.getImagesByDomain(any(), anyInt())).thenReturn(emptyList());
        Image image = new Image();
        image.setUrl("bad");
        image.setHeight(BANNER_REGULAR_IMAGE_MIN_SIZE);
        image.setWidth(BANNER_REGULAR_IMAGE_MIN_SIZE - 1);
        UrlInfo urlInfo = new UrlInfo();
        urlInfo.setImage(image);
        when(richContentClient.getUrlInfo(any())).thenReturn(urlInfo);
        Result<Collection<ImageSuggest>> result = service.generateImages(URL, "main", emptySet());
        assertNotNull(result.getResult());
        checkResult(result, new String[]{}, List.of(EMPTY_IMAGE_SEARCH_API_RESPONSE));
    }

    @Test
    public void emptySizeRCA() {
        when(imageSearchClient.getImagesByTextsAndDomain(anySet(), any(), anyInt())).thenReturn(emptyMap());
        when(imageSearchClient.getImagesByDomain(any(), anyInt())).thenReturn(emptyList());
        Image image = new Image();
        image.setUrl("bad");
        image.setWidth(BANNER_REGULAR_IMAGE_MIN_SIZE);
        UrlInfo urlInfo = new UrlInfo();
        urlInfo.setImage(image);
        when(richContentClient.getUrlInfo(any())).thenReturn(urlInfo);
        Result<Collection<ImageSuggest>> result = service.generateImages(URL, "main", emptySet());
        assertNotNull(result.getResult());
        checkResult(result, new String[]{}, List.of(EMPTY_IMAGE_SEARCH_API_RESPONSE));
    }

    @Test
    public void searchImageWithoutText() {
        when(imageSearchClient.getImagesByTextsAndDomain(anySet(), any(), anyInt())).thenReturn(emptyMap());
        when(richContentClient.getUrlInfo(any())).thenReturn(null);
        when(imageSearchClient.getImagesByDomain(any(), anyInt())).thenReturn(createImageDocs(List.of(
                Pair.of("https://ok2", BANNER_REGULAR_IMAGE_MIN_SIZE),
                Pair.of("//ok1", BANNER_REGULAR_IMAGE_MIN_SIZE),
                Pair.of("badSize", BANNER_REGULAR_IMAGE_MIN_SIZE - 1),
                Pair.of("www.ok3", BANNER_REGULAR_IMAGE_MIN_SIZE)
        )));
        Result<Collection<ImageSuggest>> result = service.generateImages(URL, "main", emptySet());
        assertNotNull(result.getResult());
        checkResult(result, new String[]{"https://ok2", "http://ok1", "http://www.ok3"}, List.of(IMAGE_SEARCH_API_WITHOUT_QUERY));
    }

    @Test
    public void searchImageWithText() {
        when(imageSearchClient.getImagesByTextsAndDomain(anySet(), any(), anyInt())).thenReturn(Map.of(
                "main", createImageDocs(List.of(
                        Pair.of("https://ok2", BANNER_REGULAR_IMAGE_MIN_SIZE),
                        Pair.of("//ok1", BANNER_REGULAR_IMAGE_MIN_SIZE),
                        Pair.of("badSize", BANNER_REGULAR_IMAGE_MIN_SIZE - 1),
                        Pair.of("www.ok3", BANNER_REGULAR_IMAGE_MIN_SIZE)))
        ));
        when(richContentClient.getUrlInfo(any())).thenReturn(null);
        when(imageSearchClient.getImagesByDomain(any(), anyInt())).thenReturn(emptyList());
        Result<Collection<ImageSuggest>> result = service.generateImages(URL, "main", emptySet());
        assertNotNull(result.getResult());
        checkResult(result, new String[]{"https://ok2", "http://ok1", "http://www.ok3"}, List.of());
    }

    @Test
    public void searchImageWithAdditionalText() {
        when(imageSearchClient.getImagesByTextsAndDomain(anySet(), any(), anyInt())).thenReturn(Map.of(
                "main", createImageDocs(List.of(
                        Pair.of("m_1", BANNER_REGULAR_IMAGE_MIN_SIZE),
                        Pair.of("m_2", BANNER_REGULAR_IMAGE_MIN_SIZE),
                        Pair.of("m_3_t2_3", BANNER_REGULAR_IMAGE_MIN_SIZE),
                        Pair.of("m_4", BANNER_REGULAR_IMAGE_MIN_SIZE))),
                "text1", createImageDocs(List.of(
                        Pair.of("t1_1", 0),
                        Pair.of("t123", BANNER_REGULAR_IMAGE_MIN_SIZE),
                        Pair.of("t1_3", BANNER_REGULAR_IMAGE_MIN_SIZE))),
                "text2", createImageDocs(List.of(
                        Pair.of("t2_1", BANNER_REGULAR_IMAGE_MIN_SIZE),
                        Pair.of("t123", BANNER_REGULAR_IMAGE_MIN_SIZE),
                        Pair.of("m_3_t2_3", BANNER_REGULAR_IMAGE_MIN_SIZE))),
                "text3", createImageDocs(List.of(
                        Pair.of("t123", BANNER_REGULAR_IMAGE_MIN_SIZE),
                        Pair.of("t3_2", BANNER_REGULAR_IMAGE_MIN_SIZE))),
                "text4", createImageDocs(List.of(
                        Pair.of("t4_1", 0),
                        Pair.of("t4_2", 0)))

        ));
        when(richContentClient.getUrlInfo(any())).thenReturn(null);
        when(imageSearchClient.getImagesByDomain(any(), anyInt())).thenReturn(emptyList());
        Result<Collection<ImageSuggest>> result = service.generateImages(URL, "main", emptySet());
        assertNotNull(result.getResult());
        checkResult(result, new String[]{
                "http://m_3_t2_3",
                "http://m_1",
                "http://m_2",
                "http://m_4",
                "http://t123",
                "http://t2_1",
                "http://t3_2",
                "http://t1_3"
        }, List.of());
    }

    private void checkResult(Result<Collection<ImageSuggest>> result, String[] expectedImages,
                             List<DefectId> expectedWarnings) {
        assertArrayEquals(
                StreamEx.of(result.getResult())
                        .map(ImageSuggest::getUrl)
                        .toArray(String.class),
                expectedImages);
        assertEquals(result.getWarnings().size(), expectedWarnings.size());
        assertThat(
                StreamEx.of(result.getWarnings())
                        .map(defect -> defect.getDefect().defectId())
                        .toList(),
                containsInAnyOrder(expectedWarnings.toArray()));
    }
}
