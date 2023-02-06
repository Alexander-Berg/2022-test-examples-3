package ru.yandex.market.ff4shops.partner;

import org.junit.jupiter.api.Test;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.ff4shops.api.model.DebugStatus;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.mbi.feature.model.FeatureStatus;
import ru.yandex.market.ff4shops.partner.dao.model.PartnerEntity;
import ru.yandex.market.ff4shops.partner.mapper.PartnerEntityMapper;
import ru.yandex.market.ff4shops.partner.service.model.Partner;

public class PartnerEntityMapperTest extends FunctionalTest {

    private static final  Partner partner = Partner.builder()
            .enabled(true)
                .cpaPartnerInterface(true)
                .id(1L)
                .businessId(11L)
                .featureType(FeatureType.DROPSHIP)
                .pushStocks(true)
                .debugStatus(DebugStatus.SUCCESS)
                .featureStatus(FeatureStatus.SUCCESS)
                .build();
    private static final PartnerEntity partnerEntity = new PartnerEntity(
            1L,  // id
            11L, // businessId
            true, // enabled,
            FeatureType.DROPSHIP, // featureType,
            FeatureStatus.SUCCESS, // featureStatus,
            true, // cpaPartnerInterface,
            true, // pushStocks
            DebugStatus.SUCCESS,
            true //stocksByPartnerInterface
    );

    @Test
    void testMapToEntity() {
        ReflectionAssert.assertReflectionEquals(partnerEntity, PartnerEntityMapper.map(partner));
    }

    @Test
    void testMapToPojo() {
        ReflectionAssert.assertReflectionEquals(partner, PartnerEntityMapper.map(partnerEntity));
    }
}
