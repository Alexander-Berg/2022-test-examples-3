package ru.yandex.market.mbi.api.controller.notification.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.CampaignTypeDTO;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerInfoDTO;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypesRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerUIDsDTO;
import ru.yandex.market.mbi.open.api.client.model.ProvideMessageAccessUIDsForPartnersRequest;
import ru.yandex.market.mbi.open.api.client.model.ProvideMessageAccessUIDsForPartnersResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.CLICK_AND_COLLECT;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.CPC;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.CROSSDOCK;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.DROPSHIP;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.DROPSHIP_BY_SELLER;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.FULFILLMENT;

class NotificationInternalControllerTest extends FunctionalTest {

    @DisplayName("Получить типы программ размещения для списка партнеров.")
    @Test
    @DbUnitDataSet(before = "NotificationInternalControllerTest.providePartnerPlacementProgramTypes.before.csv")
    void providePartnerPlacementProgramTypes() {
        var placementTypes = getMbiOpenApiClient().providePartnerPlacementProgramTypes(
                new PartnerPlacementProgramTypesRequest()
                        .partnerIds(List.of(1L, 2L, 3L, 4L))
        );

        assertThat(placementTypes.getProgramTypes(), containsInAnyOrder(
                allOf(
                        hasProperty("partnerId", Matchers.equalTo(1L)),
                        hasProperty("programTypes",
                                containsInAnyOrder(CROSSDOCK, DROPSHIP, FULFILLMENT))
                ),
                allOf(
                        hasProperty("partnerId", Matchers.equalTo(2L)),
                        hasProperty("programTypes",
                                containsInAnyOrder(CLICK_AND_COLLECT, CPC, DROPSHIP, DROPSHIP_BY_SELLER))
                )
        ));
    }

    @DisplayName("Получить информацию о партнере для списка id.")
    @Test
    @DbUnitDataSet(before = "NotificationInternalControllerTest.providePartnerInfo.before.csv")
    void providePartnerInfo() {
        var partnerInfos = getMbiOpenApiClient().providePartnerInfo(
                new NotificationPartnerInfoRequest().partnerIds(List.of(100L, 200L))
        ).getPartnerInfos();
        assertEquals(partnerInfos.size(), 2);

        Map<Long, NotificationPartnerInfoDTO> result = partnerInfos.stream()
                .collect(Collectors.toMap(NotificationPartnerInfoDTO::getPartnerId, partnerInfo -> partnerInfo));

        var shopPartner = result.get(200L);
        Assertions.assertNotNull(shopPartner);
        assertEquals(200L, shopPartner.getPartnerId());
        assertEquals("Shop Name", shopPartner.getPartnerName());
        assertEquals(20000L, shopPartner.getCampaignId());
        assertEquals(CampaignTypeDTO.SHOP, shopPartner.getCampaignType());

        var supplierPartner = result.get(100L);
        Assertions.assertNotNull(supplierPartner);
        assertEquals(100L, supplierPartner.getPartnerId());
        assertEquals("Supplier Name", supplierPartner.getPartnerName());
        assertEquals(10000L, supplierPartner.getCampaignId());
        assertEquals(CampaignTypeDTO.SUPPLIER, supplierPartner.getCampaignType());
    }

    @DisplayName("Получить список пользователей для получения WebUI сообщение по магазину")
    @Test
    void provideMessageAccessUIDsForPartners() {
        var result = getMbiOpenApiClient().provideMessageAccessUIDsForPartners(
                new ProvideMessageAccessUIDsForPartnersRequest()
                        .partnerIds(List.of(100L, 101L, 102L))
        );

        assertThat(result,
                equalTo(new ProvideMessageAccessUIDsForPartnersResponse()
                        .partnerUIDs(List.of(
                                new PartnerUIDsDTO()
                                        .partnerId(123L)
                                        .userIds(List.of(777L, 778L)),
                                new PartnerUIDsDTO()
                                        .partnerId(555L)
                                        .userIds(List.of(777L, 779L))
                        ))));
    }

}
