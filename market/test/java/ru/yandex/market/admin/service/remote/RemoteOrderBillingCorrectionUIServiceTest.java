package ru.yandex.market.admin.service.remote;

import java.util.Collections;
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
import ru.yandex.market.admin.service.remote.matchers.UiOrderItemBillingCorrectionMatchers;
import ru.yandex.market.admin.ui.model.supplier.UIBillingCorrectionCommonInfo;
import ru.yandex.market.admin.ui.model.supplier.UIRawCorrection;
import ru.yandex.market.admin.ui.model.supplier.UIServiceType;
import ru.yandex.market.admin.ui.model.supplier.UiOrderItemBillingCorrection;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.OrderBilledAmountsDao;
import ru.yandex.market.core.billing.OrderBillingCorrectionService;
import ru.yandex.market.core.order.DbOrderService;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;

/**
 * Тесты для {@link RemoteOrderBillingCorrectionUIService}
 *
 * @author vbudnev
 */
class RemoteOrderBillingCorrectionUIServiceTest extends FunctionalTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderBillingCorrectionService orderBillingCorrectionService;

    @Autowired
    private OrderBilledAmountsDao orderBilledAmountsDao;

    @Autowired
    private DbOrderService dbOrderService;

    @Autowired
    private EnvironmentService environmentService;

    private RemoteOrderBillingCorrectionUIService service;

    static Stream<Arguments> csvStructArgs() {
        return Stream.of(
                Arguments.of("Неверно задан supplier_id",
                        "774aa,10001,shop_sku1,3003",
                        "В строке '1' ожидалось число, For input string: \"774aa\"."
                ),
                Arguments.of("Неверно задан order_id",
                        "774,10001aaa,shop_sku1,3003",
                        "В строке '1' ожидалось число, For input string: \"10001aaa\"."
                ),
                Arguments.of("Неверно задана сумма",
                        "774,10001,shop_sku1,3003aaa",
                        "В строке '1' ожидалось число, For input string: \"3003aaa\"."
                ),
                Arguments.of("Неверное количество полей",
                        "774,10001,shop_sku1",
                        "Неверное число столбцов в строке '1'."
                ),
                Arguments.of("Дублирующиеся идентификаторы для корректировок",
                        "" +
                                "774,10001,shop_sku1,3003\n" +
                                "111,10001,some_sku1,4003\n" +
                                "774,10001,shop_sku1,3003",
                        "Duplicate record for BilledOrderItemKey{supplierId=774, orderId=10001, shopSku=shop_sku1}"
                )
        );
    }

    @BeforeEach
    void beforeEach() {
        service = new RemoteOrderBillingCorrectionUIService(
                orderService,
                orderBillingCorrectionService,
                null,
                orderBilledAmountsDao,
                dbOrderService,
                environmentService

        );
    }

    @Test
    @DisplayName("Парсер ui моделей корректировок из csv")
    @DbUnitDataSet(
            before = "RemoteOrderBillingCorrectionUIServiceTest.persist.before.csv"
    )
    void test_parseAndValidate() {
        List<UiOrderItemBillingCorrection> corrections = service.parseAndValidate(
                new UIRawCorrection(
                        "" +
                                "774,10001,shop_sku1,3003\n" +
                                "774,10001,shop_sku2,-4004",
                        "some_comment",
                        UIServiceType.FF_PROCESSING_CORRECTION
                )
        );

        assertThat(corrections,
                contains(
                        allOf(
                                UiOrderItemBillingCorrectionMatchers.hasSupplierId(774L),
                                UiOrderItemBillingCorrectionMatchers.hasOrderId(10001L),
                                UiOrderItemBillingCorrectionMatchers.hasShopSku("shop_sku1"),
                                UiOrderItemBillingCorrectionMatchers.hasAmount(3003),
                                UiOrderItemBillingCorrectionMatchers.hasComment("some_comment")
                        ),
                        allOf(
                                UiOrderItemBillingCorrectionMatchers.hasSupplierId(774L),
                                UiOrderItemBillingCorrectionMatchers.hasOrderId(10001L),
                                UiOrderItemBillingCorrectionMatchers.hasShopSku("shop_sku2"),
                                UiOrderItemBillingCorrectionMatchers.hasAmount(-4004),
                                UiOrderItemBillingCorrectionMatchers.hasComment("some_comment")
                        )
                )
        );
    }

    @Test
    @DisplayName("Сохранение корректировок")
    @DbUnitDataSet(
            before = "RemoteOrderBillingCorrectionUIServiceTest.persist.before.csv",
            after = "RemoteOrderBillingCorrectionUIServiceTest.persist.after.csv"
    )
    void test_persist() {
        UiOrderItemBillingCorrection uc1
                = new UiOrderItemBillingCorrection(774L, 10001L, "shop_sku1", "correction_comment", 501);

        UiOrderItemBillingCorrection uc2
                = new UiOrderItemBillingCorrection(774L, 10001L, "shop_sku2", "correction_comment", 502);

        List<UiOrderItemBillingCorrection> corrections = ImmutableList.of(uc1, uc2);

        UIBillingCorrectionCommonInfo infoFfProcessing = new UIBillingCorrectionCommonInfo(
                900L,
                "some login",
                UIServiceType.FF_PROCESSING_CORRECTION,
                "ignored comment"
        );

        UIBillingCorrectionCommonInfo infoFee = new UIBillingCorrectionCommonInfo(
                900L,
                "some login",
                UIServiceType.FEE_CORRECTION,
                "ignored comment"
        );

        service.persist(corrections, infoFfProcessing);
        service.persist(corrections, infoFee);
    }

    @Test
    @DisplayName("Сохранение DSBS корректировок")
    @DbUnitDataSet(
            before = "RemoteOrderBillingCorrectionUIServiceTest.persist.before.csv",
            after = "RemoteOrderBillingCorrectionUIServiceTest.persistDsbs.after.csv"
    )
    void test_persistDsbs() {
        UiOrderItemBillingCorrection uc1
                = new UiOrderItemBillingCorrection(10774L, 10003L, "shop_sku5", "correction_comment", 503);

        UiOrderItemBillingCorrection uc2
                = new UiOrderItemBillingCorrection(10774L, 10004L, "shop_sku6", "correction_comment", 504);

        UiOrderItemBillingCorrection uc3
                = new UiOrderItemBillingCorrection(10774L, 10004L, "shop_sku7", "correction_comment", 505);

        List<UiOrderItemBillingCorrection> corrections = ImmutableList.of(uc1, uc2, uc3);

        UIBillingCorrectionCommonInfo infoCancelOrderFee = new UIBillingCorrectionCommonInfo(
                900L,
                "some login",
                UIServiceType.CANCELLED_ORDER_FEE_CORRECTION,
                "ignored comment"
        );

        service.persist(corrections, infoCancelOrderFee);
    }

    @Test
    @DisplayName("На этапе создания проверяем наличие только одной позиции в заказе с заданным идентификатором")
    @DbUnitDataSet(before = "RemoteOrderBillingCorrectionUIServiceTest.persist.before.csv")
    void test_persist_checksUniqueMapping() {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> service.persist(
                        ImmutableList.of(
                                new UiOrderItemBillingCorrection(774L, 10002L, "shop_sku3", "correction_comment", 501)
                        ),
                        new UIBillingCorrectionCommonInfo(
                                900L,
                                "some login",
                                UIServiceType.FEE_CORRECTION,
                                "ignored comment"
                        )
                )
        );

        assertThat(ex.getMessage(),
                is("Found billed ambiguity for key=BilledOrderItemKey{supplierId=774, orderId=10002, shopSku=shop_sku3} service_type=FEE")
        );
    }

    @Test
    @DisplayName("На этапе создания проверяем тип услуги")
    void test_persist_checkServiceType() {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () ->
                        service.persist(
                                Collections.emptyList(),
                                new UIBillingCorrectionCommonInfo(
                                        900L,
                                        "some login",
                                        UIServiceType.FF_SURPLUS_SUPPLY,
                                        "ignored comment"
                                )
                        )
        );

        assertThat(ex.getMessage(), is("Explicitly not supported correction type: ff_surplus_supply"));
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
                                    UIServiceType.FF_PROCESSING_CORRECTION
                            )
                    );
                }
        );

        assertThat(ex.getMessage(), is(expectedErrMsg));
    }

    @Test
    @DisplayName("На этапе валидации проверяем наличие только одной позиции в заказе с заданным идентификатором")
    @DbUnitDataSet(before = "RemoteOrderBillingCorrectionUIServiceTest.persist.before.csv")
    void test_parseAndValidate_existingUniqueItemRequired() {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> service.parseAndValidate(
                        new UIRawCorrection(
                                "774,10002,shop_sku3,3003",
                                "some_comment",
                                UIServiceType.FF_PROCESSING_CORRECTION
                        )
                )
        );

        assertThat(ex.getMessage(),
                is("Found billed ambiguity for key=BilledOrderItemKey{supplierId=774, orderId=10002, shopSku=shop_sku3} service_type=FF_PROCESSING")
        );
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
                                UIServiceType.FF_PROCESSING_CORRECTION
                        )
                )
        );

        assertThat(
                ex.getMessage(),
                is("" +
                        "No billed amount for " +
                        "billing_type=FF_PROCESSING " +
                        "correctionType=FF_PROCESSING_CORRECTION " +
                        "key=BilledOrderItemKey{supplierId=111, orderId=10001, shopSku=some_sku1}")
        );
    }
}
