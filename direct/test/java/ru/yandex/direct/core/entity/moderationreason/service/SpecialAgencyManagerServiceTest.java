package ru.yandex.direct.core.entity.moderationreason.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpecialAgencyManagerServiceTest {

    @Test
    public void getSpecialManagerType_ReturnCorrect() {
        assertEquals("self_agency",
                SpecialAgencyManagerService.getSpecialManagerType(395174907L));
        assertEquals("self_agency_tr",
                SpecialAgencyManagerService.getSpecialManagerType(277371208L));
        assertEquals("self_agency_ua",
                SpecialAgencyManagerService.getSpecialManagerType(176441802L));
        assertEquals("self_agency_cis",
                SpecialAgencyManagerService.getSpecialManagerType(321056438L));
    }
}
