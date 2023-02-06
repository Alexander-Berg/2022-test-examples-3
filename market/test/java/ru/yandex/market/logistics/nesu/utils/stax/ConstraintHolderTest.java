package ru.yandex.market.logistics.nesu.utils.stax;

import java.time.LocalTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.enums.Constraint;
import ru.yandex.market.logistics.nesu.exception.ShopValidationException;
import ru.yandex.market.logistics.nesu.validation.shop.rule.ConstraintHolder;

@DisplayName("Тесты на класс-хранитель настроек валидации")
class ConstraintHolderTest extends AbstractTest {

    @Test
    @DisplayName("Получаем корректное значение целочисленного ограничения из списка")
    void extractIntegerRuleReturnCorrectSettings() {
        ConstraintHolder constraintHolder = ConstraintHolder.of(Map.of(Constraint.MIN, "50"));

        softly.assertThat(constraintHolder.getIntegerConstraint(Constraint.MIN)).isEqualTo(50);
    }

    @Test
    @DisplayName("Получаем корректное значение ограничения по времени из списка")
    void extractLocalTimeRuleReturnCorrectSettings() {
        ConstraintHolder constraintHolder = ConstraintHolder.of(Map.of(Constraint.MAX, "12:00"));

        softly.assertThat(constraintHolder.getLocalTimeConstraint(Constraint.MAX))
            .isEqualTo(LocalTime.of(12, 00));
    }

    @Test
    @DisplayName("Получаем исключение при запросе Integer настройки т.к. настройка не int")
    void extractIntRuleThrowsExceptionIfSettingIsntCorrect() {
        String wrongIntValue = "кто-то накосячил";
        ConstraintHolder constraintHolder = ConstraintHolder.of(Map.of(Constraint.MIN, wrongIntValue));

        softly.assertThatThrownBy(() -> constraintHolder.getIntegerConstraint(Constraint.MIN))
            .isInstanceOf(ShopValidationException.class)
            .hasMessage("Cannot parse constraint value 'кто-то накосячил'");
    }

    @Test
    @DisplayName("Получаем исключение при запросе LocalTime настройки т.к. настройка не время")
    void extractLocalTimeRuleThrowsExceptionIfSettingIsntCorrect() {
        String wrongValue = "кто-то накосячил";
        ConstraintHolder constraintHolder = ConstraintHolder.of(Map.of(Constraint.MAX, wrongValue));

        softly.assertThatThrownBy(() -> constraintHolder.getIntegerConstraint(Constraint.MAX))
            .isInstanceOf(ShopValidationException.class)
            .hasMessage("Cannot parse constraint value 'кто-то накосячил'");
    }

    @Test
    @DisplayName("Получаем исключение если значение настройки пустое")
    void extractRuleWithEmptyConstraintValueThrowsException() {
        String exceptionMessage = "Cannot parse constraint value ''";
        ConstraintHolder constraintHolder = ConstraintHolder.of(
            Map.of(Constraint.MAX, "", Constraint.MIN, "")
        );
        softly.assertThatThrownBy(() ->
            constraintHolder.getIntegerConstraint(
                Constraint.MAX
            )
        )
            .isInstanceOf(ShopValidationException.class)
            .hasMessage(exceptionMessage);

        softly.assertThatThrownBy(() ->
            constraintHolder.getLocalTimeConstraint(
                Constraint.MIN
            )
        )
            .isInstanceOf(ShopValidationException.class)
            .hasMessage(exceptionMessage);
    }

    @Test
    @DisplayName("Получаем null если такой настройки нет")
    void extractRuleThatDoesntExistsReturnNull() {
        ConstraintHolder constraintHolder = ConstraintHolder.of(Map.of());
        softly.assertThat(constraintHolder.getIntegerConstraint(Constraint.MIN)).isNull();
        softly.assertThat(constraintHolder.getIntegerConstraint(Constraint.MAX)).isNull();
    }

}
