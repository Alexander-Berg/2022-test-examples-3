package ru.yandex.market.core.credit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static ru.yandex.market.core.credit.CreditTemplateValidator.MAX_TERM_MONTHS;
import static ru.yandex.market.core.credit.CreditTemplateValidator.RATE_POW;

/**
 * Тесты на валидацию {@link CreditTemplate}.
 *
 * @author serenitas
 */
class CreditTemplateTest {

    private static CreditTemplateValidator CREDIT_TEMPLATE_VALIDATOR;

    @BeforeAll
    static void setUp() {
        EnvironmentService environmentService = Mockito.mock(EnvironmentService.class);
        Mockito.when(environmentService.getIntValue(Mockito.eq(CreditTemplateValidator.CREDITS_MIN_PRICE_LIMIT), Mockito.anyInt()))
                .thenReturn(5000);
        CREDIT_TEMPLATE_VALIDATOR = new CreditTemplateValidator(environmentService);
    }

    private static CreditTemplate.Builder createNewTemplateBuilder() {
        return new CreditTemplate.Builder()
                .setPartnerId(3)
                .setMaxTermMonths(6)
                .setMinRateScaled((int) (21.8 * RATE_POW))
                .setConditionsUrl("http://conditions.url/new_template")
                .setType(CreditTemplateType.FEED);
    }

    @Test
    @DisplayName("Минимальная цена больше максимальной")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testMinPriceGreaterThanMax() {
        long minPrice = 1000000L;
        long maxPrice = 10000L;
        CreditTemplate template = createNewTemplateBuilder()
                .setType(CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE)
                .setMinPrice(minPrice)
                .setMaxPrice(maxPrice)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    CREDIT_TEMPLATE_VALIDATOR.validateTemplate(template);
                });
        Assertions.assertEquals(String.format(
                "Max price must be greater or equal to min price, got min price %d, max price %d",
                minPrice, maxPrice),
                exception.getMessage());
    }

    @Test
    @DisplayName("Цена больше MAX_PRICE")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testPriceGreaterThanLimit() {
        long price = CreditTemplateValidator.MAX_PRICE + 1;
        CreditTemplate template = createNewTemplateBuilder()
                .setType(CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE)
                .setMinPrice(price)
                .setMaxPrice(price)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    CREDIT_TEMPLATE_VALIDATOR.validateTemplate(template);
                });
        Assertions.assertEquals(String.format(
                "Invalid max price %s - must be less or equal than %s", price, CreditTemplateValidator.MAX_PRICE),
                exception.getMessage());
    }

    @Test
    @DisplayName("Максимальный срок кредита больше 50 лет")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testIllegalMaxTerm() {
        int maxTermMonths = 1200;
        CreditTemplate template = createNewTemplateBuilder()
                .setMaxTermMonths(maxTermMonths)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    CREDIT_TEMPLATE_VALIDATOR.validateTemplate(template);
                });
        Assertions.assertEquals(String.format(
                "Invalid max credit term %d - cannot be more than %d months", maxTermMonths, MAX_TERM_MONTHS),
                exception.getMessage());
    }

    @Test
    @DisplayName("Минимальная процентная ставка больше 100 процентов")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testBigMinRate() {
        int minRateScaled = 500 * RATE_POW;
        CreditTemplate template = createNewTemplateBuilder()
                .setMinRateScaled(minRateScaled)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    CREDIT_TEMPLATE_VALIDATOR.validateTemplate(template);
                });
        Assertions.assertEquals(String.format(
                "Invalid min rate %.2f - must be between 0 and 100 percents",
                (double) minRateScaled / RATE_POW),
                exception.getMessage());
    }

    @Test
    @DisplayName("Минимальная процентная ставка отрицательная")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testNegativeMinRate() {
        int minRateScaled = -5 * RATE_POW;
        CreditTemplate template = createNewTemplateBuilder()
                .setMinRateScaled(minRateScaled)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    CREDIT_TEMPLATE_VALIDATOR.validateTemplate(template);
                });
        Assertions.assertEquals(String.format(
                "Invalid min rate %.2f - must be between 0 and 100 percents",
                (double) minRateScaled / RATE_POW),
                exception.getMessage());
    }

    @Test
    @DisplayName("Минимальная процентная ставка с тремя знаками после запятой")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testIllegalMinRate() {
        int minRateScaled = (int) (12.765 * RATE_POW);
        CreditTemplate template = createNewTemplateBuilder()
                .setMinRateScaled(minRateScaled)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    CREDIT_TEMPLATE_VALIDATOR.validateTemplate(template);
                });
        Assertions.assertEquals(String.format(
                "Invalid min rate %.5f - number of decimal places must not be greater than 2",
                (double) minRateScaled / RATE_POW),
                exception.getMessage());
    }

    @Test
    @DisplayName("Ограничения для типа FEED")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testTypeFeedWithLimits() {
        long minPrice = 10000L;
        long maxPrice = 100000L;
        CreditTemplate template = createNewTemplateBuilder()
                .setType(CreditTemplateType.FEED)
                .setMinPrice(minPrice)
                .setMaxPrice(maxPrice)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    CREDIT_TEMPLATE_VALIDATOR.validateTemplate(template);
                });
        Assertions.assertEquals(String.format(
                "Only credit template with %s type can have minPrice and maxPrice, got %s type",
                CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE, CreditTemplateType.FEED),
                exception.getMessage());
    }

    @Test
    @DisplayName("Ограничения для типа DEFAULT_FOR_ALL")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testTypeDefaultWithLimits() {
        long minPrice = 10000L;
        CreditTemplate template = createNewTemplateBuilder()
                .setType(CreditTemplateType.DEFAULT_FOR_ALL)
                .setMinPrice(minPrice)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    CREDIT_TEMPLATE_VALIDATOR.validateTemplate(template);
                });
        Assertions.assertEquals(String.format(
                "Only credit template with %s type can have minPrice and maxPrice, got %s type",
                CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE, CreditTemplateType.DEFAULT_FOR_ALL),
                exception.getMessage());
    }

    @Test
    @DisplayName("Нет ограничений по цене для типа DEFAULT_FOR_ALL_IN_RANGE")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testTypeDefaultForAllInRangeWithoutLimits() {
        CreditTemplate template = createNewTemplateBuilder()
                .setType(CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    CREDIT_TEMPLATE_VALIDATOR.validateTemplate(template);
                });
        Assertions.assertEquals(String.format(
                "Credit template with %s type must have minPrice and/or maxPrice",
                CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE),
                exception.getMessage());
    }

    @Test
    @DisplayName("Проверка шаблона без bank_id")
    void testBankIdWithNullValue() {
        Assertions.assertDoesNotThrow(() ->
                CREDIT_TEMPLATE_VALIDATOR.validateTemplate(createNewTemplateBuilder().setBankId(null).build())
        );
    }

    @Test
    @DisplayName("Проверка шаблона с bank_id")
    void testBankIdWithNotNullValue() {
        Assertions.assertDoesNotThrow(() ->
                CREDIT_TEMPLATE_VALIDATOR.validateTemplate(createNewTemplateBuilder().setBankId(12L).build())
        );
    }

}
