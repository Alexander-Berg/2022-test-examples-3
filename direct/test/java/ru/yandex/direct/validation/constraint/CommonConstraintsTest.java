package ru.yandex.direct.validation.constraint;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static ru.yandex.direct.validation.constraint.CommonConstraints.isEqual;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;

public class CommonConstraintsTest {

    private static final Defect<Void> SOME_DEFECT = invalidValue();

    @Test
    public void isEqual_success() {
        SoftAssertions.assertSoftly(soft -> {
            //Равно
            //toLowerCase - чтобы сравнивать не константы
            soft.assertThat(isEqual("XYZ".toLowerCase(), SOME_DEFECT).apply("XYZ".toLowerCase()))
                    .as(("\"xyz\" isEqual \"xyz\" -> null")).isEqualTo(null);
            soft.assertThat(isEqual(789, SOME_DEFECT).apply(789))
                    .as(("789 isEqual 789 -> null")).isEqualTo(null);

            //Не равно
            soft.assertThat(isEqual("ABC", SOME_DEFECT).apply("DEF"))
                    .as(("\"ABC\" isEqual \"DEF\" -> Defect")).isEqualTo(SOME_DEFECT);
            soft.assertThat(isEqual(123, SOME_DEFECT).apply(456))
                    .as(("123 isEqual 456 -> Defect")).isEqualTo(SOME_DEFECT);
            soft.assertThat(isEqual(new Object(), SOME_DEFECT).apply(new Object()))
                    .as(("the first object isEqual the second object -> Defect")).isEqualTo(SOME_DEFECT);

            //Игнорируем null в проверяемом значении, на него отдельный констреинт
            soft.assertThat(isEqual(0, SOME_DEFECT).apply(null))
                    .as(("0 isEqual null -> null")).isEqualTo(null);

            //НЕ игнорируем null в значении для сравнения
            soft.assertThat(isEqual(null, SOME_DEFECT).apply(0))
                    .as(("null isEqual 0 -> Defect")).isEqualTo(SOME_DEFECT);
        });
    }
}
