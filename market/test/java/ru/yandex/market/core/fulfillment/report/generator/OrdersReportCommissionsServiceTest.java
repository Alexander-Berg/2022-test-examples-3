package ru.yandex.market.core.fulfillment.report.generator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.billing.cpa_auction.CpaAuctionBillingDao;
import ru.yandex.market.core.billing.dao.DbInstallmentBilledAmountDao;
import ru.yandex.market.core.billing.dao.OrdersBillingDao;
import ru.yandex.market.core.billing.fulfillment.returns_orders.report.StorageReturnsOrdersReportDao;
import ru.yandex.market.core.billing.fulfillment.surplus.SurplusSupplyBillingDao;
import ru.yandex.market.core.fulfillment.billing.storage.dao.StorageBillingBilledAmountDao;
import ru.yandex.market.core.fulfillment.report.OrderReportDao;
import ru.yandex.market.core.offer.mapping.MarketCategoryInfo;
import ru.yandex.market.core.offer.mapping.MarketSkuInfo;
import ru.yandex.market.core.offer.mapping.MarketSkuMappingInfo;
import ru.yandex.market.core.offer.mapping.MboMappingService;
import ru.yandex.market.core.offer.mapping.ShopOffer;
import ru.yandex.market.core.order.model.MbiOrder;
import ru.yandex.market.core.order.model.MbiOrderBuilder;
import ru.yandex.market.core.order.model.MbiOrderItem;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.sorting.model.report.SortingBillingReportDao;

import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "OrdersReportCommissionsServiceTest.before.csv")
class OrdersReportCommissionsServiceTest extends FunctionalTest {
    @Autowired
    private OrdersBillingDao ordersBillingDao;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private OrdersReportCommissionsService commissionsService;

    @BeforeEach
    void setUp() {
        StorageReturnsOrdersReportDao storageReportDao = new StorageReturnsOrdersReportDao(namedParameterJdbcTemplate);
        SortingBillingReportDao sortingReportDao = new SortingBillingReportDao(namedParameterJdbcTemplate);
        SurplusSupplyBillingDao surplusDao = new SurplusSupplyBillingDao(namedParameterJdbcTemplate);
        StorageBillingBilledAmountDao storageBilledDao = new StorageBillingBilledAmountDao(namedParameterJdbcTemplate);
        OrderReportDao orderReportDao = new OrderReportDao(namedParameterJdbcTemplate.getJdbcTemplate());
        CpaAuctionBillingDao cpaAuctionBillingDao = new CpaAuctionBillingDao(namedParameterJdbcTemplate);
        DbInstallmentBilledAmountDao dbInstallmentBilledAmountDao = new DbInstallmentBilledAmountDao(namedParameterJdbcTemplate);

        MboMappingService mboMappingService = mock(MboMappingService.class);
        MarketSkuMappingInfo mapping =
                MarketSkuMappingInfo.of(
                        ShopOffer.builder()
                                .setSupplierId(778855L)
                                .setShopSku("shop_sku_1")
                                .build(),
                        MarketSkuInfo.of(123, "msku_123", MarketCategoryInfo.of(1, "cat"), null)
                );
        when(mboMappingService.createActiveMarketSkuMappingStream(anyLong()))
                .thenReturn(Stream.of(mapping));

        commissionsService =
                new OrdersReportCommissionsService(
                        ordersBillingDao,
                        namedParameterJdbcTemplate,
                        storageReportDao,
                        sortingReportDao,
                        surplusDao,
                        cpaAuctionBillingDao,
                        storageBilledDao,
                        orderReportDao,
                        mboMappingService,
                        dbInstallmentBilledAmountDao);
    }

    @Test
    void checkDropshipCommissions() {
        MbiOrder order =
                new MbiOrderBuilder()
                        .setId(27911936L)
                        .setCreationDate(Date.from(Instant.parse("2020-10-26T00:00:00Z")))
                        .setTrantime(Date.from(Instant.parse("2020-10-26T00:00:00Z")))
                        .setStatus(MbiOrderStatus.DELIVERED)
                        .setItems(Set.of(build(55285902L, "shop_sku_1")))
                        .build();

        CommissionPrepData data =
                CommissionPrepData.builder()
                        .setPartnerId(778855L)
                        .setDropship(true)
                        .setOrders(Set.of(order))
                        .setDateFrom(Instant.parse("2020-10-20T10:15:30.00Z").atZone(systemDefault()).toLocalDate())
                        .setDateTo(Instant.parse("2020-11-10T10:15:30.00Z").atZone(systemDefault()).toLocalDate())
                        .build();

        assertThat(commissionsService.getFee(data).get(27911936L)).isEqualByComparingTo("75.00");
        assertThat(commissionsService.getDeliveryCommission(data).get(27911936L)).isEqualByComparingTo("15.00");
        assertThat(commissionsService.getDeliveryToCustomerReturn(data).get(27911936L)).isEqualByComparingTo("5.00");
        assertThat(commissionsService.getReturnStorage(data).get(27911936L)).isEqualByComparingTo("150.00");
        assertThat(commissionsService.getAgencyCommission(data).get(27911936L)).isEqualByComparingTo("14.00");
        assertThat(commissionsService.getSortingCommission(data).get(27911936L)).isEqualByComparingTo("320.00");
        assertThat(commissionsService.getLoyaltyParticipationCommission(data).get(27911936L)).isEqualByComparingTo(
                "15.00");

        assertThat(commissionsService.getFfProcessing(data)).doesNotContainKey(27911936L);
        assertThat(commissionsService.getFfStorageBilling(data)).doesNotContainKey(27911936L);
        assertThat(commissionsService.getFfSurplusSupply(data)).doesNotContainKey(27911936L);
        assertThat(commissionsService.getWithdrawCommissions(data)).doesNotContainKey(27911936L);
    }

    @Test
    void checkFulfillmentCommissions() {
        MbiOrder order =
                new MbiOrderBuilder()
                        .setId(26453202L)
                        .setCreationDate(Date.from(Instant.parse("2020-10-26T00:00:00Z")))
                        .setTrantime(Date.from(Instant.parse("2020-10-26T00:00:00Z")))
                        .setStatus(MbiOrderStatus.DELIVERED)
                        .setItems(Set.of(build(59025528L, "shop_sku_1")))
                        .build();

        CommissionPrepData data =
                CommissionPrepData.builder()
                        .setPartnerId(558877L)
                        .setDropship(false)
                        .setOrders(Set.of(order))
                        .setDateFrom(Instant.parse("2020-10-20T10:15:30.00Z").atZone(systemDefault()).toLocalDate())
                        .setDateTo(Instant.parse("2020-11-10T10:15:30.00Z").atZone(systemDefault()).toLocalDate())
                        .build();

        assertThat(commissionsService.getFee(data).get(26453202L)).isEqualByComparingTo("75.00");
        assertThat(commissionsService.getFfProcessing(data).get(26453202L)).isEqualByComparingTo("80.00");
        assertThat(commissionsService.getDeliveryCommission(data).get(26453202L)).isEqualByComparingTo("15.00");
        assertThat(commissionsService.getDeliveryToCustomerReturn(data).get(26453202L)).isEqualByComparingTo("5.00");
        assertThat(commissionsService.getReturnStorage(data).get(26453202L)).isEqualByComparingTo("150.00");
        assertThat(commissionsService.getAgencyCommission(data).get(26453202L)).isEqualByComparingTo("14.00");
        assertThat(commissionsService.getFfStorageBilling(data).get(26453202L)).isEqualByComparingTo("5.00");
        assertThat(commissionsService.getFfSurplusSupply(data).get(26453202L)).isEqualByComparingTo("20.00");
        assertThat(commissionsService.getLoyaltyParticipationCommission(data).get(26453202L)).isEqualByComparingTo("15.00");
        assertThat(commissionsService.getWithdrawCommissions(data).get(26453202L)).isEqualByComparingTo("11.67");

        assertThat(commissionsService.getSortingCommission(data)).doesNotContainKey(26453202L);
    }

    @Test
    void feeCommissions() {
        MbiOrder order =
                new MbiOrderBuilder()
                        .setId(57049854L)
                        .setCreationDate(Date.from(Instant.parse("2021-08-02T00:00:00Z")))
                        .setTrantime(Date.from(Instant.parse("2021-08-02T00:00:00Z")))
                        .setStatus(MbiOrderStatus.DELIVERED)
                        .setItems(
                                Set.of(build(106605276L, "6658"),
                                        build(106605277L, "2909"),
                                        build(106605278L, "7838"),
                                        build(106605279L, "5149")))
                        .build();

        CommissionPrepData data =
                CommissionPrepData.builder()
                        .setPartnerId(1099865L)
                        .setOrders(Set.of(order))
                        .setDateFrom(Instant.parse("2021-08-01T10:15:30.00Z").atZone(systemDefault()).toLocalDate())
                        .setDateTo(Instant.parse("2021-08-03T10:15:30.00Z").atZone(systemDefault()).toLocalDate())
                        .build();

        Map<Long, BigDecimal> fee = commissionsService.getFee(data);
        assertThat(commissionsService.getFee(data).get(57049854L)).isEqualByComparingTo("1950.00");
    }

    private MbiOrderItem build(long orderId, String shopSku) {
        return MbiOrderItem.builder().setId(orderId).setShopSku(shopSku).setCount(1).build();
    }
}
