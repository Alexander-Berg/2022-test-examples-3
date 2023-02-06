package ru.yandex.market.pers.notify.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RegionHelperTest extends MarketMailerMockedDbTest {

    @Autowired
    private RegionHelper regionHelper;

    @Test
    public void testSubjectFederationIdForCity() {
        assertEquals(1, regionHelper.getSubjectFederationForRegion(213).intValue());
    }

    @Test
    public void testSubjectFederationIdForSubjectFederationRegion() {
        assertEquals(1, regionHelper.getSubjectFederationForRegion(1).intValue());
    }

    @Test
    public void testNullForUnknownCity() {
        assertNull(regionHelper.getSubjectFederationForRegion(-12345));
    }

    @Test
    public void testNullForHigherRegionType() {
        assertNull(regionHelper.getSubjectFederationForRegion(10000));
    }
}
