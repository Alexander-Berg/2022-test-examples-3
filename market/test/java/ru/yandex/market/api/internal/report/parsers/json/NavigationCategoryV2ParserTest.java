package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;
import ru.yandex.market.api.domain.v2.NavigationCategoryV2;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class NavigationCategoryV2ParserTest extends BaseTest {

    @Test
    public void shouldParse() {
        NavigationCategoryV2 categoryV2 = new NavigationCategoryV2Parser()
            .parse(ResourceHelpers.getResource("navnode.json"));

        assertEquals(90639, categoryV2.getId());
        assertEquals("Телевизоры", categoryV2.getName());
        assertEquals("Телевизоры", categoryV2.getFullName());

        assertEquals("https://mdata.yandex.net/i?path=b0609211815_img_id8279025169773608082.jpeg",
            categoryV2.getImage().getUrl());
        assertEquals(200, categoryV2.getImage().getWidth());
        assertEquals(135, categoryV2.getImage().getHeight());
    }
}
