package ru.yandex.market.content.receiving.http.service;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.dao.SkipCwDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class PartnerContentServiceImplTest extends BaseDbCommonTest {
    private PartnerContentServiceImpl partnerContentService;

    @Before
    public void setUp() throws Exception {
        SkipCwDao skipCwDao = new SkipCwDao(configuration);
        partnerContentService = new PartnerContentServiceImpl();
        partnerContentService.setSkipCwDao(skipCwDao);
    }

    @Test
    public void testAddBusinessIdToSkipCw() {
        PartnerContent.StatusResponse response = addId(1);

        assertEquals(PartnerContent.Status.OK, response.getStatus());
    }

    @Test
    public void testRemoveBusinessIdFromSkipCw() {
        PartnerContent.StatusResponse response = removeId(1);

        assertEquals(PartnerContent.Status.FAIL,  response.getStatus());
    }

    @Test
    public void testGetBusinessIdToSkipCw() {
        PartnerContent.BusinessIdsToSkipCwResponse ids = getIds();

        assertEquals(0, ids.getBusinessIdCount());
    }

    @Test
    public void testSkipCwFullCycle() {
        int firstId = 1;
        int secondId = 2;

        addId(firstId);
        addId(secondId);
        PartnerContent.BusinessIdsToSkipCwResponse ids = getIds();

        assertThat(ids.getBusinessIdList()).containsOnly(firstId, secondId);

        removeId(firstId);
        ids = getIds();

        assertThat(ids.getBusinessIdList()).containsOnly(secondId);
    }

    private PartnerContent.StatusResponse addId(int businessId) {
        return partnerContentService.addBusinessIdToSkipCw(
                PartnerContent.BusinessIdToSkipCwRequest.newBuilder()
                        .setBusinessId(businessId)
                        .build()
        );
    }

    private PartnerContent.StatusResponse removeId(int businessId) {
        return partnerContentService.removeBusinessIdFromSkipCw(
                PartnerContent.BusinessIdToSkipCwRequest.newBuilder()
                        .setBusinessId(businessId)
                        .build()
        );
    }

    private PartnerContent.BusinessIdsToSkipCwResponse getIds() {
        return partnerContentService.getBusinessIdToSkipCw(
                PartnerContent.BusinessIdsToSkipCwRequest.newBuilder().build()
        );
    }
}
