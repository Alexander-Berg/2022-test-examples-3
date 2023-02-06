package ru.yandex.market.crm.campaign.services.external.crypta;

import org.junit.Test;

import ru.yandex.market.crm.core.domain.crypta.CryptaSegment;
import ru.yandex.market.crm.core.domain.crypta.SegmentState;
import ru.yandex.market.crm.core.domain.crypta.SegmentType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CryptaSegmentJsonParserTest {
    @Test
    public void parseTest() {
        CryptaSegmentJsonParser parser = new CryptaSegmentJsonParser();
        CryptaSegment segment = parser.parse(getClass().getResourceAsStream("export_response.json"));

        assertNotNull(segment);

        assertEquals(SegmentType.HEURISTIC, segment.getType());
        assertEquals(SegmentState.ACTIVE, segment.getState());
        assertEquals(557, segment.getKeywordId());
        assertEquals(22542507, segment.getCoverage());
        assertEquals(9234091, segment.getId());
    }
}
