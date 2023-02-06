package ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест проверяет возможность конвертации параметров и операторов, используемых в expression-корректировках.
 * <p>
 * Конвертация выполняется в киворды и операторы БК. При добавлении нового, нужно добавить соответствие в конвертер.
 * <p>
 * Возможно, для этого потребуется добавление киворда в proto, т.к. сейчас там перечисленны не все возможные киворды
 * <a href="https://a.yandex-team.ru/arc/trunk/arcadia/adv/direct/proto/expression/keywords.proto">keywords.proto</a>
 * <a href="https://a.yandex-team.ru/arc/trunk/arcadia/adv/direct/proto/expression/operations.proto">operations.proto</a>
 * Модификация этих файлов должна быть согласованна с БК
 */
class ExpressionMultiplierConverterTest {

    @ParameterizedTest
    @EnumSource(BidModifierExpressionParameter.class)
    void keywordMapShouldContainsAllPossibleParameters(BidModifierExpressionParameter parameter) {
        assertThat(BaseExpressionMultiplierHandler.KEYWORD_MAP).containsKey(parameter);
    }

    @ParameterizedTest
    @EnumSource(BidModifierExpressionOperator.class)
    void operatorMapShouldContainsAllPossibleOperators(BidModifierExpressionOperator operator) {
        assertThat(BaseExpressionMultiplierHandler.OPERATION_MAP).containsKey(operator);
    }

}
