package ru.yandex.market.mbi.tariffs.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.Constants;
import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.Utils;
import ru.yandex.market.mbi.tariffs.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.model.DistributionJsonSchema;
import ru.yandex.market.mbi.tariffs.model.DraftDTO;
import ru.yandex.market.mbi.tariffs.model.FulfillmentTariffsJsonSchema;
import ru.yandex.market.mbi.tariffs.model.ModelType;
import ru.yandex.market.mbi.tariffs.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.model.SupplierCategoryFeeTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.service.check.EntityCheckException;
import ru.yandex.market.mbi.tariffs.service.check.EntityChecker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для {@link ru.yandex.market.mbi.tariffs.service.check.EntityChecker}
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "entity_checker/partners.before.csv")
class EntityCheckerTest extends FunctionalTest {
    private static final LocalDate DATE_2021_01_01 = LocalDate.of(2021, 1, 1);
    private static final LocalDate DATE_2021_02_01 = LocalDate.of(2021, 2, 1);
    private static final LocalDate DATE_2020_11_01 = LocalDate.of(2020, 11, 1);
    private static final LocalDate DATE_2020_12_01 = LocalDate.of(2020, 12, 1);

    @Autowired
    private EntityChecker entityChecker;

    @Test
    public void checkSuccess() {
        assertDoesNotThrow(() -> entityChecker.checkDraft(validDraft()));
    }

    @DbUnitDataSet(
            before = "entity_checker/check.failed.before.csv"
    )
    @ParameterizedTest(name = "[{index}]. {displayName}")
    @MethodSource("checkFailedTestData")
    @DisplayName("Тесты на различные падения при проверках")
    void checkFailed(
            DraftDTO draft,
            List<String> expectedExceptions
    ) {
        EntityCheckException exception = assertThrows(
                EntityCheckException.class,
                () -> entityChecker.checkDraft(draft)
        );

        assertThat(
                exception.getErrors(),
                containsInAnyOrder(expectedExceptions.toArray(String[]::new))
        );
    }

    private static Stream<Arguments> checkFailedTestData() {
        return Stream.of(
                //на даты
                Arguments.of(
                        validDraft().dateTo(DATE_2021_01_01.minusMonths(1)),
                        List.of("dateTo must be after then dateFrom")
                ),
                //на мету
                Arguments.of(
                        validDraft().meta(List.of()),
                        List.of("Meta can't be empty")
                ),
                Arguments.of(
                        validDraft().meta(List.of(new SupplierCategoryFeeTariffJsonSchema()))
                                .serviceType(ServiceTypeEnum.FEE),
                        List.of("The field 'amount' must be not null")
                ),
                Arguments.of(
                        validDraft().meta(List.of(new SupplierCategoryFeeTariffJsonSchema()
                                .amount(new BigDecimal("-123")))).serviceType(ServiceTypeEnum.FEE),
                        List.of("The field 'amount' must be not negative")
                ),
                //на партнеров
                Arguments.of(
                        validDraft().partner(Constants.Partners.INVALID_PARTNER),
                        List.of("Business partner with id [100] doesn't exist")
                ),
                Arguments.of(
                        validDraft().dateFrom(DATE_2020_11_01).dateTo(DATE_2020_12_01),
                        List.of("Current entity has intersections with tariffs [1]")
                ),
                Arguments.of(
                        validDraft().dateFrom(DATE_2020_11_01).dateTo(DATE_2020_12_01).partner(null),
                        List.of("Current entity has intersections with tariffs [3]")
                ),
                // тикет в st
                Arguments.of(
                        validDraft().approvalTicketId(null),
                        List.of("ApprovalTicket must be not empty")
                ),
                Arguments.of(
                        validDraft().approvalTicketId(""),
                        List.of("ApprovalTicket must be not empty")
                ),
                Arguments.of(
                        validDraft().approvalTicketId("ST_TICKET_DOESNT_EXISTS"),
                        List.of("ApprovalTicket is not exists")
                ),
                //все сразу проверки
                Arguments.of(
                        validDraft()
                                .dateTo(DATE_2021_01_01.minusMonths(1))
                                .meta(List.of())
                                .partner(Constants.Partners.INVALID_PARTNER)
                                .approvalTicketId(null)
                        ,
                        List.of(
                                "dateTo must be after then dateFrom",
                                "Meta can't be empty",
                                "Business partner with id [100] doesn't exist",
                                "ApprovalTicket must be not empty"
                        )
                )
        );
    }

    @Test
    @DisplayName("Даты окончания (невключительно) не пересекается с датой начала другого тарифа")
    @DbUnitDataSet(before = "entity_checker/testWithNoIntersectionOnClosedDates.before.csv")
    void testWithNoIntersectionOnClosedDates() {
        DraftDTO draft = new DraftDTO();
        draft.setId(1L);
        draft.setTariffId(10L);
        draft.setServiceType(ServiceTypeEnum.DISTRIBUTION);
        draft.setDateFrom(DATE_2020_12_01);
        draft.setDateTo(DATE_2021_01_01);
        draft.setUpdatedTime(OffsetDateTime.now());
        draft.setModelType(ModelType.FULFILLMENT_BY_SELLER);
        draft.setUpdatedBy("testLogin");
        draft.setMeta(Utils.convert(
                List.of(new DistributionJsonSchema()
                        .categoryId(198119L)
                        .tariffName("CEHAC")
                        .partnerSegmentTariffKey("closer-others")
                        .amount(new BigDecimal("0.18"))
                        .type(CommonJsonSchema.TypeEnum.RELATIVE)
                        .currency("RUB")
                        .billingUnit(BillingUnitEnum.ORDER)
                )
        ));
        draft.setApprovalTicketId("MBI-53560");
        assertDoesNotThrow(() -> entityChecker.checkDraft(draft));
    }

    @Test
    @DisplayName("Тарифы различия по modelType")
    @DbUnitDataSet(before = "entity_checker/testWithNoIntersectionOnDiffModelType.before.csv")
    void testWithNoIntersectionOnDiffOrderType() {
        DraftDTO draft = new DraftDTO();
        draft.setId(1L);
        draft.setServiceType(ServiceTypeEnum.FF_PROCESSING);
        draft.setDateFrom(DATE_2021_01_01);
        draft.setDateTo(DATE_2021_02_01);
        draft.setApprovalTicketId("MBI-54497");
        draft.setModelType(ModelType.DELIVERY_BY_SELLER);
        draft.setMeta(List.of(
                new FulfillmentTariffsJsonSchema()
                        .ordinal(1)
                        .priceTo(125000)
                        .weightTo(15000)
                        .dimensionsTo(150)
                        .minValue(null)
                        .billingUnit(BillingUnitEnum.ITEM)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .amount(new BigDecimal(3000))
                        .currency("RUB")
        ));
        assertDoesNotThrow(() -> entityChecker.checkDraft(draft));
    }

    private static DraftDTO validDraft() {
        DraftDTO draft = new DraftDTO();
        draft.setId(1L);
        draft.setTariffId(10L);
        draft.setServiceType(ServiceTypeEnum.DISTRIBUTION);
        draft.setDateFrom(DATE_2021_01_01);
        draft.setDateTo(DATE_2021_01_01.plusMonths(1));
        draft.setPartner(Constants.Partners.VALID_PARTNER_SHOP);
        draft.setUpdatedTime(OffsetDateTime.now());
        draft.setUpdatedBy("testLogin");
        draft.setModelType(ModelType.FULFILLMENT_BY_SELLER);
        draft.setMeta(Utils.convert(
                List.of(new DistributionJsonSchema()
                        .categoryId(198119L)
                        .tariffName("CEHAC")
                        .amount(new BigDecimal("0.018"))
                        .type(CommonJsonSchema.TypeEnum.RELATIVE)
                        .currency("RUB")
                        .billingUnit(BillingUnitEnum.ORDER)
                )
        ));
        draft.setApprovalTicketId("MBI-53560");
        return draft;
    }

    @Test
    @DisplayName("Тесты на успешную проверку всех тарифов в базе")
    @DbUnitDataSet(before = "entity_checker/check.tariffs.success.csv")
    void testTariffsSuccess() {
        assertDoesNotThrow(() -> entityChecker.checkTariffs());
    }

    @Test
    @DisplayName("Тесты на не успешную проверку всех тарифов в базе")
    @DbUnitDataSet(before = "entity_checker/check.tariffs.failed.csv")
    void testTariffsFailed() {
        EntityCheckException entityCheckException = assertThrows(
                EntityCheckException.class,
                () -> entityChecker.checkTariffs()
        );
        assertThat(entityCheckException.getErrors(), Matchers.contains(
                List.of(
                        "Following tariffs pairs have intersections: (1, 2), (3, 4)",
                        "Following tariffs can't be created : [(7: The field 'type' must be not null)]",
                        "Following tariffs have empty model type: [8]"
                ).toArray(String[]::new)
        ));

    }

    @Test
    @DisplayName("MARKETBILLING-512")
    @DbUnitDataSet(before = "entity_checker/checkNoIntersectionWithClosedTariff.before.csv")
    void checkNoIntersectionWithClosedTariff() {
        DraftDTO draft = new DraftDTO();
        draft.setId(1L);
        draft.setServiceType(ServiceTypeEnum.FF_PROCESSING);
        draft.setDateFrom(DATE_2021_01_01);
        draft.setDateTo(DATE_2021_01_01.plusMonths(1));
        draft.setUpdatedTime(OffsetDateTime.now());
        draft.setUpdatedBy("testLogin");
        draft.setModelType(ModelType.FULFILLMENT_BY_YANDEX);
        draft.setMeta(Utils.convert(
                List.of(new FulfillmentTariffsJsonSchema()
                        .ordinal(1)
                        .billingUnit(BillingUnitEnum.ITEM)
                        .amount(BigDecimal.ONE)
                        .currency("RUB")
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                )
        ));
        draft.setApprovalTicketId("MARKETBILLING-512");
        assertDoesNotThrow(() -> entityChecker.checkDraft(draft));
    }
}
