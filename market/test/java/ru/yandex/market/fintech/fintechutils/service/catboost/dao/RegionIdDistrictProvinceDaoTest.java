package ru.yandex.market.fintech.fintechutils.service.catboost.dao;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;
import ru.yandex.market.fintech.fintechutils.service.catboost.model.RegionIdMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionIdDistrictProvinceDaoTest extends AbstractFunctionalTest {

    @Autowired
    private RegionIdDistrictProvinceDao dao;

    @Test
    @DbUnitDataSet(
            before = "RegionIdDistrictProvinceDaoTest.before.csv"
    )
    void testGetRegionIdMapping() {
        Optional<RegionIdMapping> mappingOptional = dao.getRegionIdMappingByUid(10000);
        assertTrue(mappingOptional.isEmpty());


        mappingOptional = dao.getRegionIdMappingByUid(3);
        assertTrue(mappingOptional.isPresent());

        RegionIdMapping mapping = mappingOptional.get();
        assertEquals(3, mapping.getRegionId());
        assertEquals("Москва", mapping.getFederalDistrictName());
        assertEquals("Замоскворечье", mapping.getProvinceName());
    }
}
