package ru.yandex.market.marketId.service;


import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.marketId.FunctionalTest;
import ru.yandex.market.marketId.model.entity.PartnerEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(before = "../Test.before.csv")
class PartnerServiceTest extends FunctionalTest {

    @Autowired
    private PartnerService partnerService;

    @Test
    @DisplayName("Поиск партнера по ИД и типу")
    void findPartnerByIdAndType() {
        final long partnerId = 1000;
        final String partnerType = "SHOP";
        PartnerEntity expected = new PartnerEntity();
        expected.setPartnerId(partnerId);
        expected.setPartnerType(partnerType);
        Optional<PartnerEntity> actual = partnerService.findByPartnerIdAndPartnerType(partnerId, partnerType);
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    @DisplayName("Поиск партнера по ИД")
    void findPartnerById() {
        final long partnerId = 1000;
        List<PartnerEntity> actual = partnerService.findByPartnerId(partnerId);
        assertFalse(actual.isEmpty());
        assertEquals(2, actual.size());
    }

}
