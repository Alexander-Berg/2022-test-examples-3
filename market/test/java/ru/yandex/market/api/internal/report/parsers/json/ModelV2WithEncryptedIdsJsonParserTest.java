package ru.yandex.market.api.internal.report.parsers.json;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.Test;

import ru.yandex.market.api.common.VerticalNamespace;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.RecommendedItemsWithEncryptedIds;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by fettsery on 23.08.18.
 */
public class ModelV2WithEncryptedIdsJsonParserTest extends BaseTest {
    @Inject
    private ReportParserFactory factory;


    @Test
    public void testParseWithPages() {

        OfferV2JsonParser offerParser = mock(OfferV2JsonParser.class);
        when(offerParser.getParsed()).thenReturn(null);

        RecommendedItemsWithEncryptedIdsJsonParser parser = new RecommendedItemsWithEncryptedIdsJsonParser(
                factory, Collections.emptySet(), VerticalNamespace.DEFAULT);

        RecommendedItemsWithEncryptedIds result = parser.parse(ResourceHelpers.getResource("report-models-with-pages.json"));

        assertEquals(3, result.getPages().size());
        assertEquals("K5XZrtinXl4yRLtPTj5OwsGwDhSbrCYC6cuuy3bvYeDz_u95Cl0XIs9pj59_x_jOE3QISLN7ZA5oIkS3uiJ2jQdYqpsNSoTff6dJ8AeGPQwgBRIYAScmSyU8FgNJazJzmzJuv4g3wUcu4kC1_pqTZUyjmJeMzvyTO-OgIhi7IU0,",
            result.getPages().get(1));

        assertEquals(10, result.getDepartments().size());
        assertEquals("Бытовая техника", result.getDepartments().get(0).getTitle());
        assertEquals("https://m.market.yandex.ru/catalog/54419?hid=198118", result.getDepartments().get(0).getUrl());
        assertEquals("Компьютеры", result.getDepartments().get(1).getTitle());
        assertEquals("https://m.market.yandex.ru/catalog/54425?hid=91009", result.getDepartments().get(1).getUrl());
    }
}
