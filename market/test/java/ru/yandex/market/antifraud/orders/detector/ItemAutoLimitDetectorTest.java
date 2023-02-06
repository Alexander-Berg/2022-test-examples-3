package ru.yandex.market.antifraud.orders.detector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.UserMarkers;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.service.CategoriesService;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.storage.dao.StatDao;
import ru.yandex.market.antifraud.orders.storage.entity.antifraud.AccountState;
import ru.yandex.market.antifraud.orders.storage.entity.antifraud.PassportFeatures;
import ru.yandex.market.antifraud.orders.storage.entity.rules.ItemAutoLimitDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.stat.ItemPeriodicCountStat;
import ru.yandex.market.antifraud.orders.storage.entity.stat.PeriodStatValues;
import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemAutoLimitDetectorTest {

    @Mock
    private StatDao statDao;

    private CategoriesService categoriesService;
    private ConfigurationService configurationService;

    @Before
    public void initMock() {
        categoriesService = mock(CategoriesService.class);
        configurationService = mock(ConfigurationService.class);
        when(categoriesService.isFMCG(anyInt())).thenReturn(false);
        when(configurationService.getAutoLimitMultiplier()).thenReturn(4);
    }

    @Test
    public void shouldNotChangeCountNotEnoughStats() {
        var detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer();
        when(statDao.getStatsForItems(anyCollection()))
                .thenReturn(List.of(
                        ItemPeriodicCountStat.builder()
                                .msku(11L)
                                .modelId(21L)
                                .categoryId(41)
                                .periodStat_1d(PeriodStatValues.builder()
                                        .countAvgGlue((BigDecimal.valueOf(6)))
                                        .countAvgUser(BigDecimal.valueOf(3))
                                        .countSigmaGlue(BigDecimal.valueOf(4))
                                        .build())
                                .build()
                ));
        OrderDetectorResult result = detector.detectFraud(odc);
        assertThat(result.isFraud()).isFalse();
    }

    @Test
    public void shouldChangeCountYoungAcc() {
        var detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer();
        when(statDao.getStatsForItems(anyCollection()))
                .thenReturn(List.of(
                        ItemPeriodicCountStat.builder()
                                .msku(11L)
                                .modelId(21L)
                                .categoryId(41)
                                .periodStat_1d(PeriodStatValues.builder()
                                        .countAvgGlue((BigDecimal.valueOf(4)))
                                        .countAvgUser(BigDecimal.valueOf(2))
                                        .countSigmaGlue(BigDecimal.valueOf(3))
                                        .countSigmaUser(BigDecimal.valueOf(1))
                                        .build())
                                .build()
                ));
        OrderDetectorResult result = detector.detectFraud(odc);
        assertThat(result.isFraud()).isTrue();
        assertThat(result.getActions()).containsExactly(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(result.getFixedOrder().getItems())
            .containsExactly(OrderItemResponseDto.builder()
                .id(12L)
                .count(3)
                .changes(Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.COUNT))
                .build());
    }

    @Test
    public void shouldSkipItemExcludedMsku() {
        var detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer();
        when(statDao.getStatsForItems(anyCollection()))
                .thenReturn(List.of(
                        ItemPeriodicCountStat.builder()
                            .msku(11L)
                            .modelId(21L)
                            .categoryId(41)
                            .periodStat_1d(PeriodStatValues.builder()
                                .countAvgGlue((BigDecimal.valueOf(4)))
                                .countAvgUser(BigDecimal.valueOf(3))
                                .countSigmaGlue(BigDecimal.valueOf(3))
                                .build())
                            .build()
                ));
        ItemAutoLimitDetectorConfiguration conf = ItemAutoLimitDetectorConfiguration.builder()
            .enabled(true)
            .defaultCategoryMultiplier(BigDecimal.ONE)
            .fmcgMultiplier(BigDecimal.ONE)
            .excludedMsku(Set.of(11L))
            .restrictionType(ItemAutoLimitDetectorConfiguration.RestrictionType.PREPAY)
            .build();
        OrderDetectorResult result = detector.detectFraud(odc, conf);
        assertThat(result.isFraud()).isFalse();
        assertThat(result.getActions()).isEmpty();
        assertThat(result.getFixedOrder()).isNull();
    }

    @Test
    public void shouldChangeCountFmcgYoungAcc() {
        var detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer();
        when(categoriesService.isFMCG(anyInt())).thenReturn(true);
        when(statDao.getStatsForItems(anyCollection()))
                .thenReturn(List.of(
                        ItemPeriodicCountStat.builder()
                                .msku(11L)
                                .modelId(21L)
                                .categoryId(41)
                                .periodStat_1d(PeriodStatValues.builder()
                                        .countAvgGlue((BigDecimal.valueOf(2)))
                                        .countAvgUser(BigDecimal.valueOf(1))
                                        .countSigmaGlue(BigDecimal.valueOf(1))
                                        .countSigmaUser(BigDecimal.valueOf(1))
                                        .build())
                                .build()
                ));
        OrderDetectorResult result = detector.detectFraud(odc);
        assertThat(result.isFraud()).isTrue();
        assertThat(result.getActions()).containsExactly(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(result.getFixedOrder().getItems())
            .containsExactly(OrderItemResponseDto.builder()
                .id(12L)
                .count(6)
                .changes(Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.COUNT))
                .build());
    }

    @Test
    public void shouldRemoveItemFromOrderYoungAcc() {
        var detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer();
        when(statDao.getStatsForItems(anyCollection()))
                .thenReturn(List.of(
                        ItemPeriodicCountStat.builder()
                                .msku(11L)
                                .modelId(21L)
                                .categoryId(41)
                                .periodStat_1d(PeriodStatValues.builder()
                                        .countAvgGlue(BigDecimal.valueOf(1))
                                        .countAvgUser(BigDecimal.valueOf(1))
                                        .countSigmaGlue(BigDecimal.valueOf(1))
                                        .countSigmaUser(BigDecimal.valueOf(1))
                                        .build())
                                .periodStat_7d(PeriodStatValues.builder()
                                        .countAvgGlue(BigDecimal.valueOf(1))
                                        .countAvgUser(BigDecimal.valueOf(1))
                                        .countSigmaGlue(BigDecimal.valueOf(1))
                                        .countSigmaUser(BigDecimal.valueOf(1))
                                        .build())
                                .build()
                ));
        OrderDetectorResult result = detector.detectFraud(odc);
        assertThat(result.isFraud()).isTrue();
        assertThat(result.getActions()).containsExactly(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(result.getFixedOrder().getItems())
            .containsExactlyInAnyOrder(OrderItemResponseDto.builder()
                    .id(11L)
                    .count(0)
                    .changes(Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.MISSING))
                    .build(),
                OrderItemResponseDto.builder()
                    .id(12L)
                    .count(0)
                    .changes(Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.MISSING))
                    .build());
    }

    @Test
    public void shouldCorrectToUserLimitYoungAcc() {
        var detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer(123L, 124L);
        when(statDao.getStatsForItems(anyCollection()))
                .thenReturn(List.of(
                        ItemPeriodicCountStat.builder()
                                .msku(11L)
                                .modelId(21L)
                                .categoryId(41)
                                .periodStat_1d(PeriodStatValues.builder()
                                        .countAvgGlue(BigDecimal.valueOf(4))
                                        .countAvgUser(BigDecimal.valueOf(2))
                                        .countSigmaGlue(BigDecimal.valueOf(3))
                                        .countSigmaUser(BigDecimal.valueOf(1))
                                        .build())
                                .build()
                ));
        OrderDetectorResult result = detector.detectFraud(odc);
        assertThat(result.isFraud()).isTrue();
        assertThat(result.getActions()).containsExactly(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(result.getFixedOrder().getItems())
            .containsExactly(OrderItemResponseDto.builder()
                .id(12L)
                .count(3)
                .changes(Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.COUNT))
                .build());
    }

    @Test
    public void shouldCorrectToWeekLimitYoungAcc() {
        var detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer();
        when(statDao.getStatsForItems(anyCollection()))
                .thenReturn(List.of(
                        ItemPeriodicCountStat.builder()
                                .msku(11L)
                                .modelId(21L)
                                .categoryId(41)
                                .periodStat_1d(PeriodStatValues.builder()
                                        .countAvgGlue(BigDecimal.valueOf(3))
                                        .countAvgUser(BigDecimal.valueOf(1))
                                        .countSigmaGlue(BigDecimal.valueOf(3))
                                        .countSigmaUser(BigDecimal.valueOf(2))
                                        .build())
                                .periodStat_7d(PeriodStatValues.builder()
                                        .countAvgGlue(BigDecimal.valueOf(2))
                                        .countAvgUser(BigDecimal.valueOf(2))
                                        .countSigmaGlue(BigDecimal.valueOf(3))
                                        .countSigmaUser(BigDecimal.valueOf(2))
                                        .build())
                                .build()
                ));
        OrderDetectorResult result = detector.detectFraud(odc);
        assertThat(result.isFraud()).isTrue();
        assertThat(result.getActions()).containsExactly(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(result.getFixedOrder().getItems())
            .containsExactly(OrderItemResponseDto.builder()
                .id(12L)
                .count(1)
                .changes(Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.COUNT))
                .build());
    }

    @Test
    public void shouldForcePrepayGoodAcc() {
        var detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer().toBuilder()
                .passportFeaturesFuture(
                        new FutureValueHolder<>(Optional.of(PassportFeatures.builder().accountCreated(false).build())))
                .accountStateFuture(new FutureValueHolder<>(Optional.of(AccountState.builder().build())))
                .build();
        when(statDao.getStatsForItems(anyCollection()))
                .thenReturn(List.of(
                        ItemPeriodicCountStat.builder()
                                .msku(11L)
                                .modelId(21L)
                                .categoryId(41)
                                .periodStat_1d(PeriodStatValues.builder()
                                        .countAvgGlue(BigDecimal.valueOf(3))
                                        .countAvgUser(BigDecimal.valueOf(1))
                                        .countSigmaGlue(BigDecimal.valueOf(3))
                                        .countSigmaUser(BigDecimal.valueOf(3))
                                    .build())
                            .periodStat_7d(PeriodStatValues.builder()
                                .countAvgGlue(BigDecimal.valueOf(2))
                                .countAvgUser(BigDecimal.valueOf(2))
                                .countSigmaGlue(BigDecimal.valueOf(3))
                                .countSigmaUser(BigDecimal.valueOf(3))
                                .build())
                            .build()
                ));
        ItemAutoLimitDetectorConfiguration defaultConf = detector.defaultConfiguration();
        ItemAutoLimitDetectorConfiguration conf = defaultConf.toBuilder()
            .restrictionType(ItemAutoLimitDetectorConfiguration.RestrictionType.PREPAY)
            .build();
        OrderDetectorResult result = detector.detectFraud(odc, conf);
        assertThat(result.isFraud()).isTrue();
        assertThat(result.getActions()).containsExactly(AntifraudAction.PREPAID_ONLY);
        assertThat(result.getFixedOrder().getItems())
            .isEmpty();
    }

    @Test
    public void shouldForcePrepayGoodAccAboveLimit() {
        var detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer().toBuilder()
                .passportFeaturesFuture(
                        new FutureValueHolder<>(Optional.of(PassportFeatures.builder().accountCreated(false).build())))
                .accountStateFuture(new FutureValueHolder<>(Optional.of(AccountState.builder().build())))
                .build();
        when(statDao.getStatsForItems(anyCollection()))
                .thenReturn(List.of(
                        ItemPeriodicCountStat.builder()
                                .msku(11L)
                                .modelId(21L)
                                .categoryId(41)
                                .periodStat_1d(PeriodStatValues.builder()
                                        .countAvgGlue(BigDecimal.valueOf(2))
                                        .countAvgUser(BigDecimal.valueOf(1))
                                        .countSigmaGlue(BigDecimal.valueOf(1))
                                        .countSigmaUser(BigDecimal.valueOf(0))
                                        .build())
                                .periodStat_7d(PeriodStatValues.builder()
                                        .countAvgGlue(BigDecimal.valueOf(2))
                                        .countAvgUser(BigDecimal.valueOf(2))
                                        .countSigmaGlue(BigDecimal.valueOf(3))
                                        .countSigmaUser(BigDecimal.valueOf(3))
                                        .build())
                                .build()
                ));
        ItemAutoLimitDetectorConfiguration defaultConf = detector.defaultConfiguration();
        ItemAutoLimitDetectorConfiguration conf = defaultConf.toBuilder()
                .restrictionType(ItemAutoLimitDetectorConfiguration.RestrictionType.PREPAY)
                .build();
        OrderDetectorResult result = detector.detectFraud(odc, conf);
        assertThat(result.isFraud()).isTrue();
        assertThat(result.getActions()).containsExactly(AntifraudAction.PREPAID_ONLY, AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(result.getFixedOrder().getItems())
            .containsExactly(OrderItemResponseDto.builder()
                .id(12L)
                .count(5)
                .changes(Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.COUNT))
                .build());
    }

    @Test
    public void shouldCorrectToUserLimitForReseller() {
        ItemAutoLimitDetector detector = new ItemAutoLimitDetector(statDao, categoriesService, configurationService);
        OrderDataContainer odc = getDataContainer().toBuilder()
            .userMarkers(getMarkersF())
            .build();
        when(statDao.getStatsForItems(anyCollection()))
            .thenReturn(List.of(
                ItemPeriodicCountStat.builder()
                    .msku(11L)
                    .modelId(21L)
                    .categoryId(41)
                    .periodStat_1d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(4))
                        .countAvgUser(BigDecimal.valueOf(2))
                        .countSigmaGlue(BigDecimal.valueOf(3))
                        .countSigmaUser(BigDecimal.valueOf(1))
                        .build())
                    .build()
            ));
        ItemAutoLimitDetectorConfiguration defaultConf = detector.defaultConfiguration();
        ItemAutoLimitDetectorConfiguration conf = defaultConf.toBuilder()
            .restrictionType(ItemAutoLimitDetectorConfiguration.RestrictionType.PREPAY)
            .excludedMarkers(Set.of("reseller"))
            .build();
        OrderDetectorResult result = detector.detectFraud(odc, conf);
        assertThat(result.isFraud()).isTrue();
        assertThat(result.getActions()).containsExactly(AntifraudAction.ORDER_ITEM_CHANGE);
        assertThat(result.getFixedOrder().getItems())
            .containsExactly(OrderItemResponseDto.builder()
                .id(12L)
                .count(3)
                .changes(Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.COUNT))
                .build());
    }

    private OrderDataContainer getDataContainer() {
        return getDataContainer(123L, 123L);
    }

    private OrderDataContainer getDataContainer(Long requestUid, Long orderUid) {
        return OrderDataContainer.builder()
            .lastOrdersFuture(new FutureValueHolder<>(
                List.of(Order.newBuilder()
                        .setCreationDate(Instant.now().minusSeconds(86400).toEpochMilli())
                                .setStatus("DELIVERED")
                                .setRgb(RGBType.BLUE)
                                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setStringValue(String.valueOf(orderUid)).build())
                                .addItems(OrderItem.newBuilder()
                                        .setCount(5)
                                        .setPrice(129900)
                                        .setSku("11")
                                        .setModelId(21)
                                        .setSupplierId(31)
                                        .setHid(41)
                                        .build())
                                .build()
                        )
                ))
                .orderRequest(MultiCartRequestDto.builder()
                    .buyer(OrderBuyerRequestDto.builder().uid(requestUid).build())
                    .carts(List.of(
                        CartRequestDto.builder()
                            .items(List.of(OrderItemRequestDto.builder()
                                .id(11L)
                                .msku(11L)
                                .modelId(21L)
                                .supplierId(31L)
                                .categoryId(41)
                                .count(3)
                                .price(BigDecimal.valueOf(2499))
                                .build())
                            )
                            .build(),
                        CartRequestDto.builder()
                            .items(List.of(OrderItemRequestDto.builder()
                                .id(12L)
                                .msku(11L)
                                .modelId(21L)
                                .supplierId(31L)
                                .categoryId(41)
                                .count(7)
                                .price(BigDecimal.valueOf(2499))
                                .build())
                            )
                            .build()
                    ))
                    .build())
                .build();
    }

    private Future<Optional<UserMarkers>> getMarkersF() {
        return new FutureValueHolder<>(Optional.of(UserMarkers.builder().markers(Set.of("reseller")).build()));
    }
}
