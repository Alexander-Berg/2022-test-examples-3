package ru.yandex.market.api.internal.common;

import org.junit.Test;

import static org.junit.Assert.*;

public class PartnerInfoTest {

    @Test
    public void createTest() {
        String clid = "clid-test";
        String vid = "vid-test";
        Long mclid = 765L;
        Long distrType = 456L;

        PartnerInfo partnerInfo = PartnerInfo.create(clid, vid, mclid, distrType, null);

        assertNotNull(partnerInfo);
        assertEquals(clid, partnerInfo.getClid());
        assertEquals(vid, partnerInfo.getVid());
        assertEquals(mclid, partnerInfo.getMclid());
        assertEquals(distrType, partnerInfo.getDistrType());
        assertEquals(
                "{\"clid\":[\"clid-test\"],\"vid\":\"vid-test\",\"mclid\":\"765\",\"distr_type\":\"456\"}",
                partnerInfo.getPof()
        );
    }

}
