package ru.yandex.market.core.supplier.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.delivery.PartnerAndWarehouseInfoResult;
import ru.yandex.market.core.delivery.model.DeliveryWarehouseDTO;
import ru.yandex.market.core.supplier.service.PartnerFulfillmentLinkService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.campaign.model.CampaignType.SUPPLIER;

/**
 * Тесты для {@link PartnerFulfillmentLinkService}
 *
 * @author avetokhin 13.08.18.
 */
@DbUnitDataSet(before = "PartnerFulfillmentLinkServiceTest.common.before.csv")
class PartnerFulfillmentLinkServiceTest extends FunctionalTest {

    private static final int FF_SERVICE_ID = 145;
    private static final long FBS_SERVICE_ID = 100;
    private static final int PARTNER_1 = 1;
    private static final int PARTNER_2 = 2;
    private static final int PARTNER_3 = 3;
    private static final int PARTNER_4 = 4;

    @Autowired
    private PartnerFulfillmentLinkService partnerFulfillmentLinkService;
    @Autowired
    private LMSClient lmsClient;

    @Test
    @DbUnitDataSet(
            before = "PartnerFulfillmentLinkServiceTest.before.csv",
            after = "PartnerFulfillmentLinkServiceTest.afterAddLinks.csv")
    void testAddLinks() {
        partnerFulfillmentLinkService.link(PARTNER_1, FF_SERVICE_ID, 100500);
        partnerFulfillmentLinkService.link(PARTNER_2, FF_SERVICE_ID, 100500);
        partnerFulfillmentLinkService.link(PARTNER_3, FBS_SERVICE_ID, 100500);
        partnerFulfillmentLinkService.link(PARTNER_4, FF_SERVICE_ID, 100500);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerFulfillmentLinkServiceTest.afterAddLinks.csv",
            after = "PartnerFulfillmentLinkServiceTest.before.csv")
    void testRemoveLinks() {
        when(lmsClient.getPartner(anyLong())).thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(FBS_SERVICE_ID)
                .stockSyncEnabled(true)
                .build()));

        partnerFulfillmentLinkService.unlink(PARTNER_2, FF_SERVICE_ID, 100500);
        partnerFulfillmentLinkService.unlink(PARTNER_3, FBS_SERVICE_ID, 100500);
        partnerFulfillmentLinkService.unlink(PARTNER_4, FF_SERVICE_ID, 100500);

        verify(lmsClient).getPartner(FBS_SERVICE_ID);
        verify(lmsClient).updatePartnerSettings(FBS_SERVICE_ID, PartnerSettingDto.newBuilder()
                .stockSyncEnabled(false)
                .stockSyncSwitchReason(StockSyncSwitchReason.BUSINESS_WAREHOUSE_DISABLED)
                .korobyteSyncEnabled(false)
                .build());
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerFulfillmentLinkServiceTest.beforePartnerInfo.csv")
    void shouldNotCreateDtoIfLinkWasNotFoundByPartnerIdAndServiceId() {
        ArrayList<DeliveryWarehouseDTO> deliveryWarehouseDTOs = new ArrayList<>();
        deliveryWarehouseDTOs.add(new DeliveryWarehouseDTO(10001, 20001));
        deliveryWarehouseDTOs.add(new DeliveryWarehouseDTO(10002, 20002));

        List<PartnerAndWarehouseInfoResult> result =
                partnerFulfillmentLinkService.getPartnerInformation(deliveryWarehouseDTOs);

        assertTrue(result.isEmpty());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerFulfillmentLinkServiceTest.beforePartnerInfo.csv")
    void shouldNotCreateDtoIfPartnerTypeIsNotConnectedWithRbg() {
        ArrayList<DeliveryWarehouseDTO> deliveryWarehouseDTOs = new ArrayList<>();
        deliveryWarehouseDTOs.add(new DeliveryWarehouseDTO(146, 5));

        List<PartnerAndWarehouseInfoResult> result =
                partnerFulfillmentLinkService.getPartnerInformation(deliveryWarehouseDTOs);

        assertTrue(result.isEmpty());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerFulfillmentLinkServiceTest.beforePartnerInfo.csv")
    void shouldChooseFeedIdIfPartnerTypeIsSupplierAndPartnerFeedIdIfPartnerTypeIsShop() {
        ArrayList<DeliveryWarehouseDTO> deliveryWarehouseDTOs = new ArrayList<>();
        deliveryWarehouseDTOs.add(new DeliveryWarehouseDTO(101, 6));
        deliveryWarehouseDTOs.add(new DeliveryWarehouseDTO(102, 7));
        deliveryWarehouseDTOs.add(new DeliveryWarehouseDTO(102, 8));
        deliveryWarehouseDTOs.add(new DeliveryWarehouseDTO(102, 9));

        List<PartnerAndWarehouseInfoResult> result =
                partnerFulfillmentLinkService.getPartnerInformation(deliveryWarehouseDTOs);

        assertThat(result).hasSize(2);

        checkResult(result.get(0), 101, 6, Collections.singletonList(103L), SUPPLIER);

        checkResult(result.get(1), 102, 7, Collections.singletonList(104L), SUPPLIER);
    }

    private void checkResult(PartnerAndWarehouseInfoResult resultDTO,
                             long warehouseId,
                             long supplierId,
                             List<Long> feedList,
                             CampaignType campaignType) {
        assertEquals(campaignType, resultDTO.getType());
        assertEquals(warehouseId, resultDTO.getWarehouseId());
        assertEquals(supplierId, resultDTO.getSupplierId());

        ReflectionAssert.assertReflectionEquals(feedList, resultDTO.getFeedIdList(),
                ReflectionComparatorMode.LENIENT_ORDER);
    }
}
