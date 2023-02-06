package ru.yandex.market.partner.mvc.validation;

import java.util.function.Consumer;

import org.junit.Test;
import org.springframework.validation.Errors;

import ru.yandex.market.api.cpa.tax.dto.ShopVatForm;
import ru.yandex.market.core.tax.model.TaxSystem;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.core.tax.model.VatSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit тесты для {@link ShopVatFormValidator}.
 *
 * @author avetokhin 23/05/17.
 */
public class ShopVatFormValidatorTest {

    private final ShopVatFormValidator validator = new ShopVatFormValidator();

    /**
     * Проверяем валидные кейзы для различных комбинаций входных параметров. Валидатор при этом ничего не пишет в
     * объект errors.
     */
    @Test
    public void testValid() {
        final Errors errors = mockErrors();

        // NO_VAT доступен для всех СНО.
        validator.validate(vat(TaxSystem.OSN, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.USN, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.USN_MINUS_COST, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.ENVD, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.ESHN, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.PSN, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT), errors);

        // Остальные ставки доступны только для ОСН
        validator.validate(vat(TaxSystem.OSN, VatRate.VAT_0, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.OSN, VatRate.VAT_10, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.OSN, VatRate.VAT_10_110, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.OSN, VatRate.VAT_18, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.OSN, VatRate.VAT_18_118, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.OSN, VatRate.VAT_20, VatSource.WEB, VatRate.NO_VAT), errors);
        validator.validate(vat(TaxSystem.OSN, VatRate.VAT_20_120, VatSource.WEB, VatRate.NO_VAT), errors);

        assertThat(errors.hasErrors(), equalTo(false));
    }

    /**
     * Проверяем невалидные кейзы для различных комбинаций входных параметров. Валидатор при этом пишет в объект errors.
     */
    @Test
    public void testInvalid() {
        // Все поля должны быть заполнены.
        testEmptyError((errors -> validator.validate(vat(null, null, null, null), errors)));
        testEmptyError((errors -> validator.validate(vat(TaxSystem.OSN, null, null, null), errors)));
        testEmptyError((errors -> validator.validate(vat(TaxSystem.OSN, VatRate.NO_VAT, null, null), errors)));
        testEmptyError((errors -> validator.validate(vat(TaxSystem.OSN, VatRate.NO_VAT, VatSource.WEB, null), errors)));

        // Для не ОСН недоступны ставки кроме NO_VAT.
        testError((errors -> validator.validate(vat(TaxSystem.USN, VatRate.VAT_0, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.USN, VatRate.VAT_10, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.USN, VatRate.VAT_10_110, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.USN, VatRate.VAT_18, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.USN, VatRate.VAT_18_118, VatSource.WEB, VatRate.NO_VAT), errors)));

        testError((errors -> validator.validate(vat(TaxSystem.USN_MINUS_COST, VatRate.VAT_0, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.USN_MINUS_COST, VatRate.VAT_10, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.USN_MINUS_COST, VatRate.VAT_10_110, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.USN_MINUS_COST, VatRate.VAT_18, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.USN_MINUS_COST, VatRate.VAT_18_118, VatSource.WEB, VatRate.NO_VAT), errors)));

        testError((errors -> validator.validate(vat(TaxSystem.ENVD, VatRate.VAT_0, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.ENVD, VatRate.VAT_10, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.ENVD, VatRate.VAT_10_110, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.ENVD, VatRate.VAT_18, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.ENVD, VatRate.VAT_18_118, VatSource.WEB, VatRate.NO_VAT), errors)));

        testError((errors -> validator.validate(vat(TaxSystem.ESHN, VatRate.VAT_0, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.ESHN, VatRate.VAT_10, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.ESHN, VatRate.VAT_10_110, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.ESHN, VatRate.VAT_18, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.ESHN, VatRate.VAT_18_118, VatSource.WEB, VatRate.NO_VAT), errors)));

        testError((errors -> validator.validate(vat(TaxSystem.PSN, VatRate.VAT_0, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.PSN, VatRate.VAT_10, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.PSN, VatRate.VAT_10_110, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.PSN, VatRate.VAT_18, VatSource.WEB, VatRate.NO_VAT), errors)));
        testError((errors -> validator.validate(vat(TaxSystem.PSN, VatRate.VAT_18_118, VatSource.WEB, VatRate.NO_VAT), errors)));
    }


    private ShopVatForm vat(final TaxSystem taxSystem, final VatRate vatRate, final VatSource vatSource,
                            final VatRate deliveryVatRate) {
        return new ShopVatForm(null, taxSystem, vatRate, vatSource, deliveryVatRate);
    }

    private Errors mockErrors() {
        return mock(Errors.class);
    }

    private void testEmptyError(Consumer<Errors> function) {
        final Errors errors = mockErrors();
        function.accept(errors);
        verify(errors, atLeastOnce()).rejectValue(anyString(), anyString(), any(), anyString());
    }

    private void testError(Consumer<Errors> function) {
        final Errors errors = mockErrors();
        function.accept(errors);
        verify(errors, atLeastOnce()).reject(anyString(), anyString());
    }

}
