package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;
import ru.yandex.market.api.domain.v2.SearchCategoryV2;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by fettsery on 21.08.18.
 */
@WithContext
public class SearchCatergoriesJsonParserTest extends BaseTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.setUrlSchema(UrlSchema.HTTP);
    }

    @Test
    public void shouldParseCategoriesWithPhotos() {
        SearchCategoriesJsonParser parser = new SearchCategoriesJsonParser();

        List<SearchCategoryV2> categories = parser.parse(ResourceHelpers.getResource("search-categories.json"));

        assertEquals(512743, categories.get(1).getId());
        assertEquals(55063, categories.get(1).getNid());
        assertEquals("http://avatars.mds.yandex.net/get-mpic/200316/img_id5017875352806210539.jpeg/orig",
            categories.get(1).getPhotos().get(0).getUrl());

        assertEquals(90796, categories.get(11).getId());
        assertEquals(55070, categories.get(11).getNid());
        assertEquals("http://avatars.mds.yandex.net/get-mpic/397397/img_id5462664459146437598.jpeg/orig",
            categories.get(11).getPhotos().get(1).getUrl());

        assertEquals(91491, categories.get(13).getId());
        assertEquals(54726, categories.get(13).getNid());
        assertEquals("http://avatars.mds.yandex.net/get-mpic/200316/img_id1492513216702663510.jpeg/orig",
            categories.get(13).getPhotos().get(0).getUrl());
    }
}
