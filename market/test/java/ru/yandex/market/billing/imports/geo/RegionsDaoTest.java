package ru.yandex.market.billing.imports.geo;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;

import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.EARTH;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.EURASIA;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.RUSSIA;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.SPB;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.checkRegions;
import static ru.yandex.market.billing.imports.geo.RegionsDaoTestUtils.clearRegions;

public class RegionsDaoTest extends FunctionalTest {

    @Autowired
    private RegionsDao regionsDao;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    public void beforeEach() {
        clearRegions(dslContext);
    }

    @Test
    public void addOneRegion() {
        List<Region> regions = List.of(SPB);
        regionsDao.add(regions);
        checkRegions(regionsDao.get(), regions);
    }

    @Test
    public void addSeveralRegions() {
        List<Region> regions = List.of(EARTH, EURASIA, RUSSIA, SPB);
        regionsDao.add(regions);
        checkRegions(regionsDao.get(), regions);
    }
}
