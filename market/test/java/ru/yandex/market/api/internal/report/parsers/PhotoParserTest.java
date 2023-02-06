package ru.yandex.market.api.internal.report.parsers;

import org.junit.Test;
import org.mockito.InjectMocks;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.model.Photo;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
@WithMocks
public class PhotoParserTest extends BaseTest {

    @InjectMocks
    ReportParserFactory parserFactory;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.setUrlSchema(UrlSchema.HTTP);
    }

    @Test
    public void shouldParsePartialXml() {
        Pair<Photo, Photo> pair = parserFactory.getPhotoParser().parse(
            ResourceHelpers.getResource("photo-part.xml")
        );

        Photo photo = pair.first;
        assertNotNull(photo);
        assertEquals(136, photo.getWidth());
        assertEquals(250, photo.getHeight());
        assertEquals("http://best_thumbnail_url", photo.getUrl());

        Photo bigPhoto = pair.second;
        assertNotNull(bigPhoto);
        assertEquals(1920, bigPhoto.getWidth());
        assertEquals(1080, bigPhoto.getHeight());
        assertEquals("http://big_photo_test_url", bigPhoto.getUrl());
    }
}
