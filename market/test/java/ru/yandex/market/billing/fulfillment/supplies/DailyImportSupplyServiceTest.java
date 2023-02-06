package ru.yandex.market.billing.fulfillment.supplies;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.core.billing.fulfillment.supplies.dao.FulfillmentSupplyDbDao;
import ru.yandex.market.billing.fulfillment.supplies.dao.FulfillmentSupplyYtDao;
import ru.yandex.market.core.billing.fulfillment.supplies.model.FulfillmentSupply;
import ru.yandex.market.core.billing.fulfillment.supplies.model.FulfillmentSupplyItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyImportSupplyServiceTest extends FunctionalTest {
    @Autowired
    private Yt hahnYt;

    @Mock
    private Cypress cypress;

    @Autowired
    private DailyImportSupplyService dailyImportSupplyService;

    @Autowired
    private EnvironmentService environmentService;

    private static Stream<Arguments> getSuccessCheckYtData() {
        return Stream.of(
                Arguments.of(
                        "Таблицы существуют",
                        LocalDate.of(2018, 7, 1),
                        Arrays.asList(
                                "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/2018-07-01",
                                "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/2018-07-01"
                        ),
                        null
                )
        );
    }

    private static Stream<Arguments> getFailCheckYtData() {
        return Stream.of(
                Arguments.of(
                        "Нет таблицы с поставками fulfillment_shop_request за 2018-07-01",
                        LocalDate.of(2018, 7, 1),
                        Arrays.asList(
                                "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/2018-06-31",
                                "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/2018-07-02",
                                "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/2018-07-01"
                        ),
                        "Require table does not exits: //home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/2018-07-01"
                ),
                Arguments.of(
                        "Нет таблицы с товарами fulfillment_request_item за 2018-07-01",
                        LocalDate.of(2018, 7, 1),
                        Arrays.asList(
                                "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/2018-07-01",
                                "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/2018-06-31",
                                "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/2018-07-02"
                        ),
                        "Require table does not exits: //home/market/production/mstat/dictionaries/fulfillment_request_item/1d/2018-07-01"
                )
        );
    }

    @BeforeEach
    void initYt() {
        when(hahnYt.cypress()).thenReturn(cypress);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "getSuccessCheckYtData")
    @DisplayName("Успешные кейсы, когда таблиц поставок и товаров есть в YT")
    void test_verifySuccessYtTablesExist(
            String description,
            LocalDate date,
            List<String> existingTables,
            @Nullable String errorMessage
    ) {
        initCypressAnswer(existingTables);

        dailyImportSupplyService.verifyYtTablesExist(date);
    }

    private void initCypressAnswer(List<String> existingTables) {
        doAnswer(answer -> {
            final YPath tablePath = answer.getArgument(0);
            return existingTables.contains(tablePath.toString());
        }).when(cypress).exists(any(YPath.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "getFailCheckYtData")
    @DisplayName("Не успешные кейсы, когда нет таблиц поставок и товаров есть в YT")
    void test_verifyFailYtTablesExist(
            String description,
            LocalDate date,
            List<String> existingTables,
            @Nullable String errorMessage
    ) {
        initCypressAnswer(existingTables);

        Exception e = Assertions.assertThrows(
                IllegalStateException.class,
                () -> dailyImportSupplyService.verifyYtTablesExist(date)
        );
        assertEquals(e.getMessage(), errorMessage);
    }

    @Test
    void test_batchInsertSupplies() {
        FulfillmentSupplyYtDao ytDaoMock = Mockito.mock(FulfillmentSupplyYtDao.class);
        FulfillmentSupplyDbDao dbDaoMock = Mockito.mock(FulfillmentSupplyDbDao.class);

        Mockito.doAnswer(invocation -> {
            Consumer<FulfillmentSupply> consumer = invocation.getArgument(0);
            IntStream.range(0, 1525)
                    .mapToObj(i -> (FulfillmentSupply) null)
                    .forEach(consumer);

            return null;
        }).when(ytDaoMock).suppliesApplyWithConsumer(Mockito.any());

        DailyImportSupplyService dailyImportSupplyService = new DailyImportSupplyService(
                ytDaoMock,
                dbDaoMock,
                Mockito.mock(TransactionTemplate.class),
                Mockito.mock(SupplierService.class),
                environmentService
        );

        dailyImportSupplyService.fetchSuppliesFromYt();

        Mockito.verify(dbDaoMock, Mockito.times(4))
                .persistFulfillmentSupply(Mockito.anyList());
    }

    @Test
    void test_batchInsertSupplyItems() {
        FulfillmentSupplyYtDao ytDaoMock = Mockito.mock(FulfillmentSupplyYtDao.class);
        FulfillmentSupplyDbDao dbDaoMock = Mockito.mock(FulfillmentSupplyDbDao.class);

        Mockito.doAnswer(invocation -> {
            Consumer<FulfillmentSupplyItem> consumer = invocation.getArgument(0);
            IntStream.range(0, 1525)
                    .mapToObj(i -> FulfillmentSupplyItem.builder()
                            .setSupplyId(1L)
                            .setSupplierId(1L)
                            .setShopSku("sku")
                            .setFactCount(1L)
                            .setCount(1L)
                            .setSurplusCount(0L)
                            .build()
                    ).forEach(consumer);

            return null;
        }).when(ytDaoMock).supplyItemsApplyWithConsumer(Mockito.any());

        DailyImportSupplyService dailyImportSupplyService = new DailyImportSupplyService(
                ytDaoMock,
                dbDaoMock,
                Mockito.mock(TransactionTemplate.class),
                Mockito.mock(SupplierService.class),
                environmentService
        );

        dailyImportSupplyService.fetchSupplyItemsFromYt(Collections.singleton(1L));

        Mockito.verify(dbDaoMock, Mockito.times(4))
                .persistFulfillmentSupplyItem(Mockito.anyList());
    }

    @Test
    @DbUnitDataSet(
            before = "DailyImportSupplyServiceTest.test_saveLastImportDate.before.csv",
            after = "DailyImportSupplyServiceTest.test_saveLastImportDate.after.csv"
    )
    void test_saveLastImportDate() {
        DailyImportSupplyService dailyImportSupplyService = new DailyImportSupplyService(
                Mockito.mock(FulfillmentSupplyYtDao.class),
                Mockito.mock(FulfillmentSupplyDbDao.class),
                Mockito.mock(TransactionTemplate.class),
                Mockito.mock(SupplierService.class),
                environmentService
        );

        dailyImportSupplyService.importSupplies(LocalDate.of(2020, 5, 24));
    }
}
