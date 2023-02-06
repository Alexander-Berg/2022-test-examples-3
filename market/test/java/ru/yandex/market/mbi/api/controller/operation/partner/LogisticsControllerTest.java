package ru.yandex.market.mbi.api.controller.operation.partner;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.mockito.ArgumentMatchers.eq;

/**
 * Функциональные тест для {@link LogisticsController}.
 */
public class LogisticsControllerTest extends FunctionalTest {

    @Autowired
    NesuClient nesuClient;

    @Test
    @DbUnitDataSet(before = "LogisticsControllerTest.registerNesuDBS.before.csv")
    void registerNesuDBS() {
        RegisterShopDto registerShopDto = RegisterShopDto.builder()
                .id(1L)
                .externalId(null)
                .businessId(2L)
                .regionId(213)
                .name("torba-tut.ru DBS")
                .role(ShopRole.DROPSHIP_BY_SELLER)
                .build();
        getMbiOpenApiClient().registerPartnerNesu(1L, 1L, null, null);
        Mockito.verify(nesuClient).registerShop(eq(registerShopDto));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsControllerTest.registerNesuFBS.before.csv")
    void registerNesuFBS() {
        RegisterShopDto registerShopDto = RegisterShopDto.builder()
                .id(1L)
                .externalId(null)
                .businessId(2L)
                .regionId(213)
                .name("dropship")
                .role(ShopRole.DROPSHIP)
                .build();
        getMbiOpenApiClient().registerPartnerNesu(1L, 1L, null, null);
        Mockito.verify(nesuClient).registerShop(eq(registerShopDto));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsControllerTest.registerNesuFBS.before.csv")
    void registerOrCreateNesuFBSFlag() {
        RegisterShopDto registerShopDto = RegisterShopDto.builder()
                .id(1L)
                .externalId(null)
                .businessId(2L)
                .regionId(213)
                .name("dropship")
                .role(ShopRole.DROPSHIP)
                .createPartnerOnRegistration(true)
                .build();
        getMbiOpenApiClient().registerOrCreatePartnerNesu(1L, 1L, null, null, true);
        Mockito.verify(nesuClient).registerShop(eq(registerShopDto));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsControllerTest.setCreatePartner.before.csv")
    void registerOrCreateNesuNotExistent() {
        Assertions.assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().registerOrCreatePartnerNesu(1L, 2L,
                        null, null, true),
                "Partner with id 2 was not found"
        );
    }
}
