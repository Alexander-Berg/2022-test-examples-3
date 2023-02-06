package ru.yandex.market.ff.grid.validation.rule.custom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.grid.validation.rule.concrete.ValidationRuleTest;

/**
 * Unit тесты для {@link VatRateValidationRule}.
 *
 * @author avetokhin 18/12/17.
 */
public class VatRateValidationRuleTest extends ValidationRuleTest {
    private static VatRateValidationRule createRule(boolean isMandatory) {
        return new VatRateValidationRule(isMandatory, "", "");
    }

    @Override
    protected Stream<Arguments> params() {
        final Collection<Arguments> parameters = new ArrayList<>();

        // Все значения перечисления VatRate и их коды.
        for (VatRate vatRate : VatRate.values()) {
            parameters.add(Arguments.of(createCell(vatRate.name()), true, createRule(true)));
            parameters.add(Arguments.of(createCell(vatRate.name()), true, createRule(false)));
            parameters.add(Arguments.of(createCell(vatRate.getId().toString()), true, createRule(true)));
            parameters.add(Arguments.of(createCell(vatRate.getId().toString()), true, createRule(false)));
        }

        parameters.add(Arguments.of(createCell(null), true, createRule(false)));
        parameters.add(Arguments.of(createCell(""), true, createRule(false)));
        parameters.add(Arguments.of(createCell(""), false, createRule(true)));
        parameters.add(Arguments.of(createCell(null), false, createRule(true)));
        parameters.add(Arguments.of(createCell("-1"), false, createRule(true)));
        parameters.add(Arguments.of(createCell("0"), false, createRule(true)));
        parameters.add(Arguments.of(createCell("20"), false, createRule(true)));
        parameters.add(Arguments.of(createCell("abr"), false, createRule(true)));
        parameters.add(Arguments.of(createCell("-1"), false, createRule(false)));
        parameters.add(Arguments.of(createCell("0"), false, createRule(false)));
        parameters.add(Arguments.of(createCell("20"), false, createRule(false)));
        parameters.add(Arguments.of(createCell("abr"), false, createRule(false)));

        return parameters.stream();
    }
}
