package ru.yandex.market.api.partner.controllers.outlet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.market.api.partner.apisupport.ApiInvalidRequestException;
import ru.yandex.market.api.partner.apisupport.ApiNotFoundException;
import ru.yandex.market.api.partner.apisupport.ErrorRestModelCode;
import ru.yandex.market.api.partner.auth.AuthPrincipal;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletAddressDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletDeliveryRuleDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletScheduleItemDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletTypeDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletWorkingScheduleDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.converter.OutletInfoToOutletDTOConverter;
import ru.yandex.market.api.partner.controllers.region.model.Region;
import ru.yandex.market.api.partner.controllers.util.OutletHelper;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.RegionType;
import ru.yandex.market.core.outlet.DuplicateShopOutletIdException;
import ru.yandex.market.core.outlet.OutletConstraintsValidator;
import ru.yandex.market.core.outlet.OutletDeliveryRuleValidator;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.OutletVisibility;
import ru.yandex.market.core.outlet.moderation.ManageOutletInfoService;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.protocol.ProtocolService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutletControllerTest {
    @InjectMocks
    private OutletController controller;
    @Mock
    private CampaignService campaignService;
    @Mock
    private ManageOutletInfoService outletInfoService;
    @Mock
    private OutletHelper outletHelper;
    @Mock
    private OutletInfoToOutletDTOConverter outletInfoToOutletDTOConverter;
    @Mock
    private RegionService regionService;
    @Mock
    private ProtocolService protocolService;
    @Mock
    private OutletDeliveryRuleValidator outletDeliveryRuleValidator;
    @Mock
    private PartnerService partnerService;
    @Mock
    private OutletConstraintsValidator outletConstraintsValidator;


    @BeforeEach
    void before() {
        when(campaignService.getMarketCampaign(11774L)).thenReturn(
                mockCampaignInfo(11774L, 1774L, CampaignType.SHOP));
        when(campaignService.getMarketCampaign(10774L)).thenReturn(
                mockCampaignInfo(10774L, 774L, CampaignType.SHOP));
        when(campaignService.getMarketCampaign(10L)).thenReturn(
                mockCampaignInfo(10L, 774L, CampaignType.SHOP));
        when(outletInfoService.getOutletInfo(anyLong())).thenReturn(getOutletInfo());
        when(campaignService.getMarketCampaign(10666L)).thenReturn(
                mockCampaignInfo(10666L, 666L, CampaignType.SUPPLIER));
        when(campaignService.getMarketCampaign(10667L)).thenReturn(
                mockCampaignInfo(10667L, 667L, CampaignType.SUPPLIER));
        when(outletInfoService.getOutletInfo(404L)).thenReturn(null);
        when(partnerService.isDropshipAvailable(666L)).thenReturn(true);
        when(partnerService.isDropshipAvailable(667L)).thenReturn(false);
    }

    private CampaignInfo mockCampaignInfo(long campaignId, long datasourceId, CampaignType campaignType) {
        return new CampaignInfo(campaignId, datasourceId, -1L, -1L, campaignType);
    }


    /**
     * Проверяем попытку изменения кампанией аутлета, не принадлежащего текущей кампании
     */
    @Test
    void testTryIllegalChangeOutlet() {
        OutletDTO outletInfo = getExpectedOutletDeliveryRuleRequest();
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };
        ApiNotFoundException e = Assertions.assertThrows(ApiNotFoundException.class, () ->
                controller.putOutlet(11774L, 659515, outletInfo, authPrincipal));
        assertEquals(ErrorRestModelCode.NOT_FOUND, e.getErrors().get(0).getCode());
    }

    @Test
    void testDropshipTryIllegalChangeOutlet() {
        OutletDTO outletInfo = getExpectedOutletDeliveryRuleRequest();
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };
        ApiNotFoundException e = Assertions.assertThrows(ApiNotFoundException.class, () ->
                controller.putOutlet(10666L, 659515, outletInfo, authPrincipal));
        assertEquals(ErrorRestModelCode.NOT_FOUND, e.getErrors().get(0).getCode());
    }

    @Test
    void testSupplierTryIllegalChangeOutlet() {
        OutletDTO outletInfo = getExpectedOutletDeliveryRuleRequest();
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };
        ApiInvalidRequestException e = Assertions.assertThrows(ApiInvalidRequestException.class, () ->
                controller.putOutlet(10667L, 659515, outletInfo, authPrincipal));
        assertEquals(ErrorRestModelCode.BAD_REQUEST, e.getErrors().get(0).getCode());
    }

    /**
     * Проверяем попытку удаления кампанией аутлета, не принадлежащего текущей кампании
     */
    @Test
    void testTryIllegalDeleteOutlet() {
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };
        ApiNotFoundException e = Assertions.assertThrows(ApiNotFoundException.class, () ->
                controller.deleteOutlet(11774L, 659515L, authPrincipal));
        assertEquals(ErrorRestModelCode.NOT_FOUND, e.getErrors().get(0).getCode());
    }


    /**
     * Проверяем случай когда в параметрах запроса передан несуществующий outletId
     */
    @Test
    void testOutletCodeNotFound() {
        OutletDTO outletInfo = getExpectedOutletDeliveryRuleRequest();
        outletInfo.setShopOutletCode("hfh");
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };
        ApiNotFoundException e = Assertions.assertThrows(ApiNotFoundException.class, () ->
                controller.putOutlet(10774L, 404, outletInfo, authPrincipal));
        assertEquals(ErrorRestModelCode.NOT_FOUND, e.getErrors().get(0).getCode());
    }


    /**
     * Проверяем случай когда пытаются создать аутлет с тем же кодом для магазина
     */
    @Test
    void testOutletDuplicate() {
        OutletDTO outletRequestExpected = getExpectedOutletDeliveryRuleRequest();
        outletRequestExpected.setShopOutletCode("hfh");
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };
        when(outletInfoService.getOutletInfo(anyLong(), anyString())).thenReturn(Optional.of(new OutletInfo(11, 774L, OutletType.DEPOT, "name", true, "shopOutletCode")));
        ApiInvalidRequestException e = Assertions.assertThrows(ApiInvalidRequestException.class, () ->
                controller.createOutlet(10, outletRequestExpected, authPrincipal));
        assertEquals(ErrorRestModelCode.DUPLICATE_OUTLET_CODE, e.getErrors().get(0).getCode());
    }

    @Test
    void testDuplicateShopOutletId() {
        OutletDTO outletRequestExpected = getExpectedOutletDeliveryRuleRequest();
        outletRequestExpected.setShopOutletCode("hfh");
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };
        when(outletConstraintsValidator.validate(any())).thenReturn(List.of());
        when(outletInfoService.getOutletInfo(anyLong(), anyString())).thenReturn(Optional.empty());
        when(outletHelper.getRegionById(2L)).thenReturn(new Region(2, "a", RegionType.CITY, null));
        when(outletHelper.convertOutletDTOToInfo(any(), any(), anyLong(), anyLong())).thenReturn(new OutletInfo(11, 774L, OutletType.DEPOT, "name", true, "shopOutletCode"));
        when(protocolService.actionInTransaction(any(), any())).thenThrow(new DuplicateShopOutletIdException("hfh"));

        ApiInvalidRequestException e = Assertions.assertThrows(ApiInvalidRequestException.class, () ->
                controller.createOutlet(10, outletRequestExpected, authPrincipal));
        assertEquals(400, e.getHttpCode());
    }


    /**
     * Проверяет случай когда region id неизвестный (нет в базе в табличке regions)
     */
    @Test
    void testUnknownRegionId() {
        OutletDTO outletRequestExpected = getExpectedOutletDeliveryRuleRequest();
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };

        outletRequestExpected.getOutletAddress().setRegionId(17777777L);
        when(outletInfoService.getOutletInfo(anyLong(), anyString())).thenReturn(Optional.empty());
        ApiInvalidRequestException e = Assertions.assertThrows(ApiInvalidRequestException.class, () ->
                controller.createOutlet(10L, outletRequestExpected, authPrincipal));
        assertEquals(ErrorRestModelCode.UNKNOWN_REGION, e.getErrors().get(0).getCode());

        when(outletInfoService.getOutletInfo(anyLong())).thenReturn(getOutletInfo());
        e = Assertions.assertThrows(ApiInvalidRequestException.class, () ->
                controller.putOutlet(10L, 11L, outletRequestExpected, authPrincipal));
        assertEquals(ErrorRestModelCode.UNKNOWN_REGION, e.getErrors().get(0).getCode());
    }

    /**
     * Проверяет случай когда не передают region id в теле запроса на создание
     */
    @Test
    void testEmptyRegionIdCreate() {
        OutletDTO outletRequestExpected = getExpectedOutletDeliveryRuleRequest();
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };

        //иммитируем, как будто в regionId ничего не передали
        outletRequestExpected.getOutletAddress().setRegionId(null);

        outletRequestExpected.setShopOutletCode(null);
        ApiInvalidRequestException e = Assertions.assertThrows(ApiInvalidRequestException.class, () ->
                controller.createOutlet(10, outletRequestExpected, authPrincipal));
        assertEquals(ErrorRestModelCode.NOT_SPECIFIED, e.getErrors().get(0).getCode());
    }

    /**
     * Проверяет случай когда не передают region id в теле запроса
     */
    @Test
    void testEmptyRegionIdUpdate() {
        OutletDTO outletRequestExpected = getExpectedOutletDeliveryRuleRequest();
        AuthPrincipal authPrincipal = new AuthPrincipal(321L) {
        };

        //иммитируем, как будто в regionId ничего не передали
        outletRequestExpected.getOutletAddress().setRegionId(null);

        when(outletInfoService.getOutletInfo(anyLong())).thenReturn(getOutletInfo());
        ApiInvalidRequestException e = Assertions.assertThrows(ApiInvalidRequestException.class, () ->
                controller.putOutlet(10L, 11, outletRequestExpected, authPrincipal));
        assertEquals(ErrorRestModelCode.NOT_SPECIFIED, e.getErrors().get(0).getCode());
    }

    private OutletInfo getOutletInfo() {
        return new OutletInfo(11, 774L, OutletType.DEPOT, "name", true, "shopOutletCode");
    }

    OutletDTO getExpectedOutletRequestWithoutDelivery() {
        OutletDTO outletRequestExpected = new OutletDTO();
        outletRequestExpected.setType(OutletTypeDTO.DEPOT);
        outletRequestExpected.setName("Место тестировщика");
        outletRequestExpected.setMain(true);
        outletRequestExpected.setShopOutletCode("strOutlet");

        OutletAddressDTO outletAddressDTO = new OutletAddressDTO();
        outletAddressDTO.setStreet("Пискаревский проспект");
        outletAddressDTO.setNumber("2");
        outletAddressDTO.setEstate("3407");
        outletAddressDTO.setBlock("2");
        outletAddressDTO.setRegionId(2L);
        outletRequestExpected.setOutletAddress(outletAddressDTO);
        outletRequestExpected.setVisibility(OutletVisibility.VISIBLE);


        OutletWorkingScheduleDTO outletWorkingScheduleDTO = new OutletWorkingScheduleDTO();
        outletWorkingScheduleDTO.setWorkInHoliday(false);
        List<OutletScheduleItemDTO> outletScheduleItemDTOList = new ArrayList<>();
        OutletScheduleItemDTO outletScheduleItemDTO = new OutletScheduleItemDTO();
        outletScheduleItemDTO.setEndDay(OutletScheduleItemDTO.DayOfWeek.THURSDAY);
        outletScheduleItemDTO.setStartDay(OutletScheduleItemDTO.DayOfWeek.MONDAY);
        outletScheduleItemDTO.setStartTime("09:00");
        outletScheduleItemDTO.setEndTime("20:30");
        outletScheduleItemDTOList.add(outletScheduleItemDTO);
        OutletScheduleItemDTO outletScheduleItemDTO1 = new OutletScheduleItemDTO();
        outletScheduleItemDTO1.setEndDay(OutletScheduleItemDTO.DayOfWeek.FRIDAY);
        outletScheduleItemDTO1.setStartDay(OutletScheduleItemDTO.DayOfWeek.FRIDAY);
        outletScheduleItemDTO1.setStartTime("00:00");
        outletScheduleItemDTO1.setEndTime("23:00");
        outletScheduleItemDTOList.add(outletScheduleItemDTO1);
        outletWorkingScheduleDTO.setScheduleItems(outletScheduleItemDTOList);

        outletRequestExpected.setWorkingSchedule(outletWorkingScheduleDTO);

        outletRequestExpected.setEmails(ru.yandex.common.util.collections.CollectionFactory.list("ofmtest@yandex.ru"));

        outletRequestExpected.setPhones(CollectionFactory.list("+ 7 (345) 919-191991",
                "+ 7 (543) 123-33123111"));
        outletRequestExpected.setCoords("56.156131, 35.802831");

        return outletRequestExpected;

    }

    OutletDTO getExpectedOutletDeliveryRuleRequest() {
        OutletDTO outletInfoExpected = getExpectedOutletRequestWithoutDelivery();
        OutletDeliveryRuleDTO deliveryRule = new OutletDeliveryRuleDTO();
        deliveryRule.setCost(new BigDecimal(100));
        deliveryRule.setPriceFreePickup(new BigDecimal(122L));
        deliveryRule.setMinDeliveryDays(2);
        deliveryRule.setMaxDeliveryDays(2);
        deliveryRule.setOrderBefore(24);
        deliveryRule.setUnspecifiedDeliveryInterval(false);
        deliveryRule.setDeliveryServiceId(113L);

        outletInfoExpected.setDeliveryRules(CollectionFactory.list(deliveryRule));
        return outletInfoExpected;
    }

}
