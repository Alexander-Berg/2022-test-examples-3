package ru.yandex.market.core.delivery;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.experiment.StocksByPiExperiment;
import ru.yandex.market.core.delivery.model.LogisticPartnerSettings;
import ru.yandex.market.core.supplier.model.PartnerFulfillmentLink;
import ru.yandex.market.core.util.LogisticPartnerServiceUtil;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseFilter;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.type.ExtendedShipmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.entity.type.PartnerStatus.ACTIVE;
import static ru.yandex.market.logistics.management.entity.type.PartnerStatus.FROZEN;
import static ru.yandex.market.logistics.management.entity.type.PartnerStatus.INACTIVE;
import static ru.yandex.market.logistics.management.entity.type.PartnerStatus.TESTING;

@DbUnitDataSet(before = "LogisticPartnerServiceTest.before.csv")
class LogisticPartnerServiceTest extends FunctionalTest {

    @Autowired
    private LogisticPartnerService tested;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private NesuClient nesuClient;
    @Autowired
    private StocksByPiExperiment ff4shopsStockFlagExperiment;

    @BeforeEach
    void init() {
        ff4shopsStockFlagExperiment.reset();
    }

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(lmsClient, nesuClient);
    }

    @Test
    void testShouldNotChangeFrozenLmsStatus() {
        when(lmsClient.getPartner(eq(1L))).thenReturn(
                LogisticPartnerServiceUtil.getMockedLmsResponse(false, FROZEN));
        ArgumentCaptor<PartnerSettingDto> settingCaptor = ArgumentCaptor.forClass(PartnerSettingDto.class);
        LogisticPartnerSettings settings = LogisticPartnerSettings.builder()
                .stockSyncEnabled(true)
                .korobyteSyncEnabled(true)
                .areStocksFromFeed(true)
                .autoSwitchStockSyncEnabled(true)
                .partnerStatus(PartnerStatus.ACTIVE)
                .build();
        tested.changeLogisticPartnerSettings(settings, 1L);
        verify(lmsClient).getPartner(1L);
        verify(lmsClient).updatePartnerSettings(same(12L), settingCaptor.capture());
        verify(nesuClient).setStockSyncStrategy(12L, 1L, true);
        verify(lmsClient, never()).changePartnerStatus(anyLong(), any(PartnerStatus.class));
    }

    @Test
    void testChangeLogisticPartnerSettings_allFieldsUpdated() {
        when(lmsClient.getPartner(eq(1L))).thenReturn(
                LogisticPartnerServiceUtil.getMockedLmsResponse(false, INACTIVE));
        ArgumentCaptor<PartnerSettingDto> settingCaptor = ArgumentCaptor.forClass(PartnerSettingDto.class);

        LogisticPartnerSettings settings = LogisticPartnerSettings.builder()
                .stockSyncEnabled(true)
                .korobyteSyncEnabled(true)
                .areStocksFromFeed(true)
                .autoSwitchStockSyncEnabled(true)
                .partnerStatus(PartnerStatus.ACTIVE)
                .build();

        tested.changeLogisticPartnerSettings(settings, 1L);
        verify(lmsClient).getPartner(1L);
        verify(lmsClient).changePartnerStatus(12L, PartnerStatus.ACTIVE);
        verify(nesuClient).setStockSyncStrategy(12L, 1L, true);
        verify(lmsClient).updatePartnerSettings(same(12L), settingCaptor.capture());

        PartnerSettingDto settingValue = settingCaptor.getValue();
        assertNotNull(settingValue);
        assertTrue(settingValue.getAutoSwitchStockSyncEnabled());
        assertTrue(settingValue.getKorobyteSyncEnabled());
        assertTrue(settingValue.getStockSyncEnabled());
        assertEquals("TRACK", settingValue.getTrackingType());
        assertEquals(StockSyncSwitchReason.NEW, settingValue.getStockSyncSwitchReason());
        assertEquals(Integer.valueOf(1), settingValue.getLocationId());
    }

    @Test
    void testChangeLogisticPartnerSettings_onlyKorobyteSyncUpdated() {
        when(lmsClient.getPartner(eq(1L))).thenReturn(
                LogisticPartnerServiceUtil.getMockedLmsResponse(true, TESTING));
        ArgumentCaptor<PartnerSettingDto> settingCaptor = ArgumentCaptor.forClass(PartnerSettingDto.class);

        LogisticPartnerSettings settings = LogisticPartnerSettings.builder()
                .korobyteSyncEnabled(false)
                .build();

        tested.changeLogisticPartnerSettings(settings, 1L);
        verify(lmsClient).getPartner(1L);
        verify(lmsClient, never()).changePartnerStatus(anyLong(), any(PartnerStatus.class));
        verify(lmsClient).updatePartnerSettings(same(12L), settingCaptor.capture());

        PartnerSettingDto settingValue = settingCaptor.getValue();
        assertNotNull(settingValue);
        assertTrue(settingValue.getAutoSwitchStockSyncEnabled());
        assertFalse(settingValue.getKorobyteSyncEnabled());
        assertTrue(settingValue.getStockSyncEnabled());
        assertEquals("TRACK", settingValue.getTrackingType());
        assertEquals(StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL, settingValue.getStockSyncSwitchReason());
        assertEquals(Integer.valueOf(1), settingValue.getLocationId());
    }

    @Test
    void testChangeLogisticPartnerSettings_onlyPartnerStatusUpdated() {
        when(lmsClient.getPartner(eq(1L))).thenReturn(
                LogisticPartnerServiceUtil.getMockedLmsResponse(false, INACTIVE));

        LogisticPartnerSettings settings = LogisticPartnerSettings.builder()
                .partnerStatus(PartnerStatus.ACTIVE)
                .build();

        tested.changeLogisticPartnerSettings(settings, 1L);
        verify(lmsClient).getPartner(1L);
        verify(lmsClient).changePartnerStatus(12L, PartnerStatus.ACTIVE);
        verify(nesuClient, never()).setStockSyncStrategy(eq(12L), eq(1L), anyBoolean());
        verify(lmsClient, never()).updatePartnerSettings(eq(12L), any(PartnerSettingDto.class));
    }

    @Test
    void testChangeLogisticPartnerSettings_onlyStockSyncStrategyUpdated() {
        when(lmsClient.getPartner(eq(1L))).thenReturn(
                LogisticPartnerServiceUtil.getMockedLmsResponse(false, ACTIVE));

        LogisticPartnerSettings settings = LogisticPartnerSettings.builder()
                .areStocksFromFeed(true)
                .build();

        tested.changeLogisticPartnerSettings(settings, 1L);
        verify(lmsClient).getPartner(1L);
        verify(nesuClient).setStockSyncStrategy(12L, 1L, true);
    }

    @Test
    @DbUnitDataSet(before = "testChangeLogisticPartnerSettings_onlySockSyncStrategyUpdated_ff4shops_exp.before.csv")
    void testChangeLogisticPartnerSettings_onlySockSyncStrategyUpdated_ff4shops_exp() {
        when(lmsClient.getPartner(eq(1L))).thenReturn(
                LogisticPartnerServiceUtil.getMockedLmsResponse(false, ACTIVE));

        LogisticPartnerSettings settings = LogisticPartnerSettings.builder()
                .areStocksFromFeed(true)
                .build();

        tested.changeLogisticPartnerSettings(settings, 1L);
        verify(lmsClient).getPartner(1L);
    }

    @Test
    void testChangeLogisticPartnerSettings_noLogisticPartnerFound() {
        when(lmsClient.getPartner(eq(1L))).thenReturn(Optional.empty());

        LogisticPartnerSettings settings = LogisticPartnerSettings.builder()
                .areStocksFromFeed(true)
                .build();

        tested.changeLogisticPartnerSettings(settings, 1L);
        verify(lmsClient).getPartner(1L);
    }

    @Test
    void testChangeLogisticPartnerSettings_forShop() {
        LogisticPartnerSettings settings = LogisticPartnerSettings.builder()
                .areStocksFromFeed(true)
                .build();

        tested.changeLogisticPartnerSettings(settings, 2L);
    }

    @Test
    void hasActivePartnerRelationFalse() {
        // given
        when(lmsClient.getBusinessWarehouses(any(), any(), any()))
                .thenReturn(new PageResult<BusinessWarehouseResponse>().setData(List.of()));

        // when
        var result = tested.hasActivePartnerRelation(List.of(new PartnerFulfillmentLink(3L, 13L, null)));

        // then
        assertFalse(result);
        var filterCaptor = ArgumentCaptor.forClass(BusinessWarehouseFilter.class);
        verify(lmsClient).getBusinessWarehouses(filterCaptor.capture(), any(), any());
        var filter = filterCaptor.getValue();
        assertEquals(13L, Iterables.getLast(filter.getIds()));
    }

    @Test
    void hasActivePartnerRelationTrue() {
        // given
        var whatever = BusinessWarehouseResponse.newBuilder()
                .partnerStatus(PartnerStatus.ACTIVE)
                .shipmentType(ExtendedShipmentType.WITHDRAW)
                .build();
        when(lmsClient.getBusinessWarehouses(any(), any(), any()))
                .thenReturn(new PageResult<BusinessWarehouseResponse>().setData(List.of(whatever)));

        // when
        var result = tested.hasActivePartnerRelation(List.of(new PartnerFulfillmentLink(3L, 13L, null)));

        // then
        assertTrue(result);
        var filterCaptor = ArgumentCaptor.forClass(BusinessWarehouseFilter.class);
        verify(lmsClient).getBusinessWarehouses(filterCaptor.capture(), any(), any());
        var filter = filterCaptor.getValue();
        assertEquals(13L, Iterables.getLast(filter.getIds()));
    }

    @Test
    void testRegisterNewLogisticPartner_Dsbs() {
        var registerShopCaptor = ArgumentCaptor.forClass(RegisterShopDto.class);

        tested.registerNewLogisticPartner(3L, "test", null, null);
        verify(nesuClient, times(1)).registerShop(registerShopCaptor.capture());

        RegisterShopDto registerShopDto = registerShopCaptor.getValue();
        assertEquals(3L, registerShopDto.getId());
        assertEquals(10L, registerShopDto.getBusinessId());
        assertEquals("test", registerShopDto.getName());
        assertEquals(ShopRole.DROPSHIP_BY_SELLER, registerShopDto.getRole());
        assertNull(registerShopDto.getTaxSystem());
        assertNull(registerShopDto.getSiteUrl());
        assertNull(registerShopDto.getCreatePartnerOnRegistration());
        assertNull(registerShopDto.getWarehouseContact());

        // marketId and balance-related identifiers should be null
        assertNull(registerShopDto.getMarketId());
        assertNull(registerShopDto.getBalanceClientId());
        assertNull(registerShopDto.getBalanceContractId());
        assertNull(registerShopDto.getBalancePersonId());
    }

    @Test
    void testRegisterNewLogisticPartner_Fbs() {
        var registerShopCaptor = ArgumentCaptor.forClass(RegisterShopDto.class);

        tested.registerNewLogisticPartner(12L, "dropship", null, true);
        verify(nesuClient, times(1)).registerShop(registerShopCaptor.capture());

        RegisterShopDto registerShopDto = registerShopCaptor.getValue();
        assertEquals(12L, registerShopDto.getId());
        assertEquals(10L, registerShopDto.getBusinessId());
        assertEquals("dropship", registerShopDto.getName());
        assertEquals(ShopRole.DROPSHIP, registerShopDto.getRole());
        assertNull(registerShopDto.getTaxSystem());
        assertNull(registerShopDto.getSiteUrl());
        assertTrue(registerShopDto.getCreatePartnerOnRegistration());
        assertNull(registerShopDto.getWarehouseContact());

        // marketId and balance-related identifiers should be null
        assertNull(registerShopDto.getMarketId());
        assertNull(registerShopDto.getBalanceClientId());
        assertNull(registerShopDto.getBalanceContractId());
        assertNull(registerShopDto.getBalancePersonId());
    }

    @Test
    void testConfirmNewLogisticPartner_Dsbs() {
        var configureShopCaptor = ArgumentCaptor.forClass(ConfigureShopDto.class);

        tested.confirmNewLogisticPartner(3L, 3L);
        verify(nesuClient, times(1)).configureShop(eq(3L), configureShopCaptor.capture());

        ConfigureShopDto configureShopDto = configureShopCaptor.getValue();
        assertEquals(configureShopDto.getMarketId(), 3L);
        assertEquals(configureShopDto.getBalanceClientId(), 777L);
        assertEquals(configureShopDto.getBalanceContractId(), 500001L);
        assertEquals(configureShopDto.getBalancePersonId(), 13L);
    }
}
