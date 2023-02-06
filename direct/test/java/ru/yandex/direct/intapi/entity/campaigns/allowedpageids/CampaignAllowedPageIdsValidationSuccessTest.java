package ru.yandex.direct.intapi.entity.campaigns.allowedpageids;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CampaignAllowedPageIdsValidationSuccessTest {

    private static final String TEST_ID = "1234";
    private static final List<String> TEST_IDS = Arrays.asList("1234", "345", "123");
    private static final List<Long> TEST_IDS_RESULT = Arrays.asList(1234L, 345L, 123L);

    private CampaignAllowedPageIdsValidation validation = new CampaignAllowedPageIdsValidation();

    @Test
    public void campaignId() {
        assertEquals("1234 -> 1234",
                (long) validation.checkAndParseCampaignId(TEST_ID), 1234L);
    }

    @Test
    public void campaignIds() {
        assertEquals("[1234, \"345\", '123']",
                validation.checkAndParseCampaignIds(TEST_IDS),
                TEST_IDS_RESULT);
    }

    @Test
    public void pageIds() {
        assertEquals("[1234, \"345\", '123']",
                validation.checkAndParseCampaignIds(TEST_IDS),
                TEST_IDS_RESULT);
    }

    @Test
    public void pageIdsEmptyList() {
        assertEquals("empty list",
                validation.checkAndParsePageIds(Collections.emptyList()),
                Collections.emptyList());
    }

    @Test
    public void pageIdsNull() {
        assertEquals("empty list",
                validation.checkAndParsePageIds(null),
                Collections.emptyList());
    }
}
