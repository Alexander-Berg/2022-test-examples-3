package ru.yandex.market.billing.imports.supply;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.PartnerDao;
import ru.yandex.market.yql_test.annotation.YqlTest;

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
                        "Require table does not exits: " +
                                "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/2018-07-01"
                ),
                Arguments.of(
                        "Нет таблицы с товарами fulfillment_request_item за 2018-07-01",
                        LocalDate.of(2018, 7, 1),
                        Arrays.asList(
                                "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/2018-07-01",
                                "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/2018-06-31",
                                "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/2018-07-02"
                        ),
                        "Require table does not exits: " +
                                "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/2018-07-01"
                )
        );
    }

    private void initCypressAnswer(List<String> existingTables) {
        doAnswer(answer -> {
            final YPath tablePath = answer.getArgument(0);
            return existingTables.contains(tablePath.toString());
        }).when(cypress).exists(any(YPath.class));
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
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/latest"
            },
            csv = "DailyImportSupplyServiceTest.test_batchInsertSupplies.yt.csv",
            yqlMock = "DailyImportSupplyServiceTest.test_batchInsertSupplies.yt.mock"
    )
    @DbUnitDataSet(
            after = "DailyImportSupplyServiceTest.test_batchInsertSupplies.after.csv"
    )
    void test_batchInsertSupplies() {
        dailyImportSupplyService.fetchSuppliesFromYt();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/latest",
                    "//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/latest",
                    "//home/market/production/mstat/dictionaries/mbo/mboc_offers/latest"
            },
            csv = "DailyImportSupplyServiceTest.test_batchInsertSupplyItems.yt.csv",
            yqlMock = "DailyImportSupplyServiceTest.test_batchInsertSupplyItems.yt.mock"
    )
    @DbUnitDataSet(
            after = "DailyImportSupplyServiceTest.test_batchInsertSupplyItems.after.csv"
    )
    void test_batchInsertSupplyItems() {
        Set<Long> suppliersFilterSet = Set.of(123L, 456L, 777L, 888L);
        dailyImportSupplyService.fetchSupplyItemsFromYt(suppliersFilterSet);
    }

    @Test
    @DbUnitDataSet(
            before = "DailyImportSupplyServiceTest.test_saveLastImportDate.before.csv",
            after = "DailyImportSupplyServiceTest.test_saveLastImportDate.after.csv"
    )
    void test_saveLastImportDate() {
        DailyImportSupplyService service = new DailyImportSupplyService(
                Mockito.mock(FulfillmentSupplyYtDao.class),
                Mockito.mock(FulfillmentSupplyDbDao.class),
                Mockito.mock(TransactionTemplate.class),
                Mockito.mock(PartnerDao.class),
                environmentService
        );

        service.importSupplies(LocalDate.of(2020, 5, 24));
    }
}
