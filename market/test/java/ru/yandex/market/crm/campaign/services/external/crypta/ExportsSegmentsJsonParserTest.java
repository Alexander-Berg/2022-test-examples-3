package ru.yandex.market.crm.campaign.services.external.crypta;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.core.domain.crypta.CryptaSegment;
import ru.yandex.market.crm.core.domain.crypta.SegmentState;
import ru.yandex.market.crm.core.domain.crypta.SegmentType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExportsSegmentsJsonParserTest {
    @Test
    public void parseTest() {
        ExportsSegmentsJsonParser parser = new ExportsSegmentsJsonParser();
        List<CryptaSegment> segments = parser.parse(getClass().getResourceAsStream("exports_segments_response.json"));

        assertNotNull(segments);

        assertEquals(2, segments.size());

        CryptaSegment segment1 = segments.get(0);
        CryptaSegment segment2 = segments.get(1);

        assertEquals(7856587, segment1.getId());
        assertEquals(175, segment2.getId());

        assertEquals(SegmentType.AUDIENCE, segment1.getType());
        assertEquals(SegmentType.DEFAULT, segment2.getType());

        assertEquals(SegmentState.ACTIVE, segment1.getState());
        assertEquals(SegmentState.ACTIVE, segment2.getState());

        assertEquals("Любители джаза", segment1.getName());
        assertEquals("Любители джаза", segment2.getName());

        assertEquals("По данным Я.Музыки.", segment1.getDescription());
        assertEquals("По данным Я.Музыки.", segment2.getDescription());

        assertEquals("group-6d205b21", segment1.getParentId());
        assertEquals("group-6d205b21", segment2.getParentId());

        assertEquals(1753111, segment1.getCoverage());
        assertEquals(1753419, segment2.getCoverage());

        assertEquals(557, segment1.getKeywordId());
        assertEquals(281, segment2.getKeywordId());
    }
}
