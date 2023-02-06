package ru.yandex.market.admin.service.remote;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.service.remote.matchers.UIFfSupplyItemCorrectionMatchers;
import ru.yandex.market.admin.ui.model.supplier.UIBillingCorrectionCommonInfo;
import ru.yandex.market.admin.ui.model.supplier.UIFfSupplyItemCorrection;
import ru.yandex.market.admin.ui.model.supplier.UIRawCorrection;
import ru.yandex.market.admin.ui.model.supplier.UIServiceType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.fulfillment.surplus.SurplusSupplyBillingDao;
import ru.yandex.market.core.fulfillment.correction.SupplyBillingCorrectionService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;

/**
 * Тесты для {@link RemoteFfSupplyItemCorrectionUIService}.
 *
 * @author vbudnev
 */
class RemoteFfSupplyItemCorrectionUIServiceTest extends FunctionalTest {

    @Autowired
    private SurplusSupplyBillingDao surplusSupplyBillingDao;

    @Autowired
    private SupplyBillingCorrectionService supplyBillingCorrectionService;
    @Autowired
    private EnvironmentService environmentService;

    private RemoteFfSupplyItemCorrectionUIService service;

    static Stream<Arguments> csvStructArgs() {
        return Stream.of(
                Arguments.of("Неверно задан supplier_id",
                        "12aa3,500,sku_1,-100",
                        "В строке '1' ожидалось число, For input string: \"12aa3\"."
                ),
                Arguments.of("Неверно задан supply_id",
                        "123,500aa,sku_1,-100",
                        "В строке '1' ожидалось число, For input string: \"500aa\"."
                ),
                Arguments.of("Неверно задана сумма",
                        "123,500,sku_1,100aaa",
                        "В строке '1' ожидалось число, For input string: \"100aaa\"."
                ),
                Arguments.of("Неверное количество полей",
                        "12aa3,111,sku_1",
                        "Неверное число столбцов в строке '1'."
                ),
                Arguments.of("Дублирующиейся идентификаторы для корректировок",
                        "111,10001,some_sku1,3003\n" +
                                "222,10002,some_sku2,3003\n" +
                                "111,10001,some_sku1,4003",
                        "Duplicate record for supplier_id=111 supply_id=10001 shop_sku=some_sku1"
                )
        );
    }

    private static Clock initClock() {
        Instant instant = LocalDateTime.of(2019, 1, 1, 12, 34, 56)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    @BeforeEach
    void beforeEach() {
        service = new RemoteFfSupplyItemCorrectionUIService(
                surplusSupplyBillingDao,
                supplyBillingCorrectionService,
                null,
                environmentService,
                initClock()
        );
    }

    @Test
    @DisplayName("Парсер ui моделей излишков из csv")
    @DbUnitDataSet(
            before = "RemoteFfSupplyItemCorrectionUIServiceTest.surplus.before.csv"
    )
    void test_parseAndValidate() {
        List<UIFfSupplyItemCorrection> corrections = service.parseAndValidate(
                new UIRawCorrection(
                        "774,10001,shop_sku1,3003\n" +
                                "774,10001,shop_sku2,4004\n",
                        "some_comment",
                        UIServiceType.FF_SURPLUS_SUPPLY
                )
        );

        assertThat(corrections,
                contains(
                        allOf(
                                UIFfSupplyItemCorrectionMatchers.hasSupplierId(774L),
                                UIFfSupplyItemCorrectionMatchers.hasSupplyId(10001L),
                                UIFfSupplyItemCorrectionMatchers.hasShopSku("shop_sku1"),
                                UIFfSupplyItemCorrectionMatchers.hasAmount(3003),
                                UIFfSupplyItemCorrectionMatchers.hasComment("some_comment")
                        ),
                        allOf(
                                UIFfSupplyItemCorrectionMatchers.hasSupplierId(774L),
                                UIFfSupplyItemCorrectionMatchers.hasSupplyId(10001L),
                                UIFfSupplyItemCorrectionMatchers.hasShopSku("shop_sku2"),
                                UIFfSupplyItemCorrectionMatchers.hasAmount(4004),
                                UIFfSupplyItemCorrectionMatchers.hasComment("some_comment")
                        )
                )
        );
    }

    @Test
    @DisplayName("Сохранение корректировок для излишков")
    @DbUnitDataSet(
            before = "RemoteFfSupplyItemCorrectionUIServiceTest.surplus.before.csv",
            after = "RemoteFfSupplyItemCorrectionUIServiceTest.surplus.after.csv"
    )
    void test_persist() {
        UIFfSupplyItemCorrection uc1
                = new UIFfSupplyItemCorrection(774L, "shop_sku1", 10001L, "correction_comment", 501);

        UIFfSupplyItemCorrection uc2
                = new UIFfSupplyItemCorrection(774L, "shop_sku2", 10001L, "correction_comment", 502);

        List<UIFfSupplyItemCorrection> corrections = ImmutableList.of(uc1, uc2);

        UIBillingCorrectionCommonInfo info = new UIBillingCorrectionCommonInfo(
                900L,
                "some login",
                UIServiceType.FF_SURPLUS_SUPPLY,
                "ignored comment"
        );

        service.persist(corrections, info);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("csvStructArgs")
    @DisplayName("Первичная проверка структуры в csv")
    void test_parseAndValidate_structureCheck(
            String description,
            String csv,
            String expectedErrMsg
    ) {
        Exception ex = Assertions.assertThrows(
                Exception.class,
                () -> {
                    service.parseAndValidate(
                            new UIRawCorrection(
                                    csv,
                                    "some_comment",
                                    UIServiceType.FF_SURPLUS_SUPPLY
                            )
                    );
                }
        );

        assertThat(ex.getMessage(), is(expectedErrMsg));
    }

    @Test
    @DisplayName("На этапе валидации проверяем наличие прямого начисления")
    void test_parseAndValidate_existingBillingRequired() {

        Exception ex = Assertions.assertThrows(
                Exception.class,
                () -> service.parseAndValidate(
                        new UIRawCorrection(
                                "111,10001,some_sku1,3003",
                                "some_comment",
                                UIServiceType.FF_SURPLUS_SUPPLY
                        )
                )
        );

        assertThat(ex.getMessage(), is("No billed amount for supplier_id=111 supply_id=10001 shop_sku=some_sku1"));
    }
}
