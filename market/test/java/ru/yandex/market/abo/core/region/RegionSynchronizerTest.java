package ru.yandex.market.abo.core.region;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author artemmz on 28.08.15.
 */
public class RegionSynchronizerTest extends EmptyTest {
    @Autowired
    RegionSynchronizer regionSynchronizer;

    @Test
    @Disabled
    public void testUpdateRegions() throws Exception {
        regionSynchronizer.updateRegions();
    }
}
