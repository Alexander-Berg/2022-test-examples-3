package ru.yandex.market.ff.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.util.CalendaringServiceUtils;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FailedFreezeStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SimpleStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.AvailableStockResponse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.StockFreezingResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class
ShadowWithdrawRequestValidationServiceTest extends IntegrationTest {

    private static final String MARKET_NAME_1 = "market_name1";
    private static final String MARKET_NAME_2 = "market_name2";
    private static final String MARKET_NAME_3 = "market_name3";
    private static final String SHOP_SKU_1 = "SHOPSKU1";
    private static final String SHOP_SKU_2 = "SHOPSKU2";
    private static final String SHOP_SKU_3 = "SHOPSKU3";

    @Autowired
    private RequestValidationService requestValidationService;

    @Autowired
    private LmsClientCachingService lmsClientCachingService;

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @BeforeEach
    void setupCorrectExternalServices() {
        StockFreezingResponse enoughToFreeze = StockFreezingResponse.success(1L);
        when(stockStorageOutboundClient.freezeStocks(any(), any())).thenReturn(enoughToFreeze);
        mockLmsClient(145);
    }

    @AfterEach
    void invalidateCache() {
        lmsClientCachingService.invalidateCache();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/1/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void unsuccessfulShadowWithdrawValidationBecauseNoItemsInPreviousSupplies() {
        validate();
        verify(stockStorageOutboundClient, never()).freezeStocks(any(), any());
        verify(stockStorageOutboundClient, never()).getAvailable(anyLong(), anyInt(), any(), anyList());
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/2/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void unsuccessfulShadowWithdrawValidationBecauseNotEnoughItemsForFreeze() {
        StockFreezingResponse notEnoughFreezeResponse = StockFreezingResponse.notEnough(1L,
                Collections.singletonList(FailedFreezeStock.of(SHOP_SKU_2, 1L, 1, 3, 0)));
        when(stockStorageOutboundClient.freezeStocks(any(), any())).thenReturn(notEnoughFreezeResponse);
        validate();
        verify(stockStorageOutboundClient, never()).getAvailable(anyLong(), anyInt(), any(), anyList());
        verify(stockStorageOutboundClient).freezeStocks(any(), any());
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/3/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/3/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void unsuccessfulShadowWithdrawValidationBecauseNoTimeSlotsForService() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(0, csClient);
        validate();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/4/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/4/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void unsuccessfulShadowWithdrawValidationBecauseNotEnoughPalletsLimitForAtLeastOneItem() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(4, csClient);
        validate();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/5/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/5/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void unsuccessfulShadowWithdrawValidationBecauseNotEnoughItemsLimitForAtLeastOneItem() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(4, csClient);
        validate();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/6/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/6/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void unsuccessfulShadowWithdrawValidationBecauseNotEnoughPalletsLimitForOneItem() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(4, csClient);
        validate();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/7/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/7/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void unsuccessfulShadowWithdrawValidationBecauseNotEnoughItemsLimitForOneItem() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(4, csClient);
        validate();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/8/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/8/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulShadowWithdrawValidationWithChangeItemsCountWhileFreeze() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(4, csClient);
        when(stockStorageOutboundClient.getAvailable(anyLong(), anyInt(), any(), anyList()))
                .thenReturn(getAvailableStockResponse(30, 5, 0));
        validate();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/9/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/9/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulShadowWithdrawValidationWithoutChangeItemsCountWhileFreeze() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(4, csClient);
        when(stockStorageOutboundClient.getAvailable(anyLong(), anyInt(), any(), anyList()))
                .thenReturn(getAvailableStockResponse(300, 500, 20));
        validate();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/10/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/10/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulShadowWithdrawValidationWhenThereIsBookingsWithoutRequestId() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(4, csClient);
        when(stockStorageOutboundClient.getAvailable(anyLong(), anyInt(), any(), anyList()))
                .thenReturn(getAvailableStockResponse(300, 500, 20));
        validate();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/11/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/11/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulShadowWithdrawValidationWhenThereIsBookingsWithoutRequests() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(4, csClient);
        when(stockStorageOutboundClient.getAvailable(anyLong(), anyInt(), any(), anyList()))
                .thenReturn(getAvailableStockResponse(300, 500, 20));
        validate();
    }

    @Test
    @DatabaseSetup("classpath:service/shadow-withdraw-validation/12/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/shadow-withdraw-validation/12/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void successfulAuctionShadowWithdrawValidation() {
        CalendaringServiceUtils.mockGetSlotsWithoutQuotaCheck(4, csClient);
        validate();
        Mockito.verify(stockStorageOutboundClient, never()).freezeStocks(any(), anyList());
    }

    private void validate() {
        ShopRequest request = shopRequestFetchingService.getRequestOrThrow(1);
        requestValidationService.validateAndPrepare(request);
    }

    private AvailableStockResponse getAvailableStockResponse(int firstQuantity,
                                                             int secondQuantity,
                                                             int thirdQuantity) {
        return AvailableStockResponse.success(StockType.FIT,
                List.of(
                        new SimpleStock(MARKET_NAME_1, 1L, SHOP_SKU_1, firstQuantity, 145, false),
                        new SimpleStock(MARKET_NAME_2, 1L, SHOP_SKU_2, secondQuantity, 145, false),
                        new SimpleStock(MARKET_NAME_3, 1L, SHOP_SKU_3, thirdQuantity, 145, false)
                )
        );
    }

    private void mockLmsClient(long serviceId) {
        List<PartnerExternalParamGroup> returnedValue = List.of(new PartnerExternalParamGroup(
                serviceId,
                List.of(new PartnerExternalParam(PartnerExternalParamType.IS_CALENDARING_ENABLED.name(), "", "true")))
        );
        when(lmsClient.getPartnerExternalParams(Set.of(PartnerExternalParamType.IS_CALENDARING_ENABLED)))
                .thenReturn(returnedValue);
    }
}
