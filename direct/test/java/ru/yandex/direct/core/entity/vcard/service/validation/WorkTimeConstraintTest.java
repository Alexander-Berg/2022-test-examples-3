package ru.yandex.direct.core.entity.vcard.service.validation;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.vcard.service.validation.WorkTimeConstraint.DefectDefinitions.dayScheduleDuplicated;
import static ru.yandex.direct.core.entity.vcard.service.validation.WorkTimeConstraint.DefectDefinitions.emptyWorktime;
import static ru.yandex.direct.core.entity.vcard.service.validation.WorkTimeConstraint.DefectDefinitions.invalidFormat;
import static ru.yandex.direct.core.entity.vcard.service.validation.WorkTimeConstraint.DefectDefinitions.tooLongWorktime;
import static ru.yandex.direct.core.entity.vcard.service.validation.WorkTimeConstraint.workTimeIsValid;

@RunWith(Parameterized.class)
public class WorkTimeConstraintTest {

    @Parameterized.Parameter(0)
    public String workTime;

    @Parameterized.Parameter(1)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        String tooLong = StringUtils.repeat('1', WorkTimeConstraint.WORKTIME_MAX_LENGTH + 1);

        return asList(new Object[][]{

                /*
                    позитивные кейсы
                 */

                {null, null},
                {"0#0#0#0#0#0", null},
                {"0#0#00#00#00#00", null},

                // диапазон дней
                {"0#0#0#0#0#0", null},
                {"0#1#0#0#0#0", null},
                {"2#3#0#0#0#0", null},
                {"0#6#0#0#0#0", null},
                {"6#6#0#0#0#0", null},

                // диапазон минут
                {"1#1#0#0#0#15", null},
                {"1#1#0#0#0#30", null},
                {"1#1#0#0#0#45", null},
                {"1#1#0#15#0#30", null},
                {"1#1#0#30#0#45", null},
                {"1#1#0#45#1#00", null},
                {"1#1#0#00#0#00", null},

                // диапазон часов
                {"1#1#0#0#0#0", null},
                {"1#1#0#0#0#15", null},
                {"1#1#1#0#1#15", null},
                {"1#1#1#0#2#0", null},
                {"1#1#0#0#23#0", null},
                {"1#1#22#0#23#0", null},

                // диапазон часов и минут
                {"1#1#0#0#1#15", null},
                {"1#1#0#0#1#45", null},
                {"1#1#1#0#1#15", null},
                {"1#1#1#0#1#45", null},
                {"1#1#1#30#1#45", null},
                {"1#1#22#30#22#45", null},
                {"1#1#22#45#23#0", null},
                {"1#1#23#30#23#45", null},
                {"1#1#0#0#23#0", null},

                // диапазон дней
                {"0#0#7#0#23#0", null},
                {"0#1#7#0#23#0", null},
                {"5#6#7#0#23#0", null},
                {"6#6#7#0#23#0", null},

                // время работы на несколько дней
                // (во втором периоде должно быть проверено, что позиции правильно интерпретируются)
                {"0#2#21#45#22#15;" + "3#5#0#0#23#0", null},                 // 2 периода подряд
                {"0#2#21#45#22#15;" + "4#5#0#0#23#0", null},                 // 2 периода с промежутком
                {"4#5#0#0#23#0;" + "0#3#21#45#22#15", null},                 // 2 периода в обратном порядке
                {"0#0#0#0#0#0;" + "1#3#0#0#23#0;" + "4#6#0#15#22#45", null},      // 3 периода на все дни
                {"0#0#21#45#22#15;" + "2#3#0#0#23#0;" + "4#6#0#15#22#45", null},  // 3 периода с промежутком

                // день начала больше дня конца
                {"1#0#1#15#22#45", null},
                {"6#5#1#15#22#45", null},

                // логически неправильный диапазон часов и минут (perl-ом считаются валидными)
                {"1#2#1#15#1#15", null},
                {"1#2#1#15#1#00", null},
                {"1#2#1#15#0#15", null},
                {"1#2#1#15#0#45", null},
                {"1#2#3#15#2#30", null},
                {"1#1#23#0#23#0", null},


                /*
                    негативные кейсы
                 */

                // пустое и слишком длинное
                {"", emptyWorktime()},
                {tooLong, tooLongWorktime()},

                // полная хрень
                {";", invalidFormat()},
                {"%", invalidFormat()},
                {"0", invalidFormat()},
                {"0#1", invalidFormat()},

                // не хватает одного числа
                {"#1#1#15#2#30", invalidFormat()},
                {"0##1#15#2#30", invalidFormat()},
                {"0#1##15#2#30", invalidFormat()},
                {"0#1#1##2#30", invalidFormat()},
                {"0#1#1#15##30", invalidFormat()},
                {"0#1#1#15#2#", invalidFormat()},

                // две ; вместо одной
                {"0#1#1#15##2;30", invalidFormat()},

                // символ вместо числа
                {"a#1#1#15#2#30", invalidFormat()},
                {"0#a#1#15#2#30", invalidFormat()},
                {"0#1#a#15#2#30", invalidFormat()},
                {"0#1#1#a#2#30", invalidFormat()},
                {"0#1#1#15#a#30", invalidFormat()},
                {"0#1#1#15#2#a", invalidFormat()},

                // выход чисел за границы
                {"-1#1#1#15#2#30", invalidFormat()},
                {"0#7#1#15#2#30", invalidFormat()},
                {"0#7#-1#15#2#30", invalidFormat()},
                {"0#7#1#14#2#30", invalidFormat()},
                {"0#7#1#60#2#30", invalidFormat()},
                {"0#7#1#-15#2#30", invalidFormat()},
                {"0#7#1#-1#2#30", invalidFormat()},
                {"0#7#1#15#-1#30", invalidFormat()},
                {"0#7#1#15#7#30", invalidFormat()},
                {"0#7#1#15#2#29", invalidFormat()},
                {"0#7#1#15#2#60", invalidFormat()},
                {"0#7#1#15#2#-30", invalidFormat()},
                {"0#7#1#15#2#-1", invalidFormat()},

                // время работы на несколько дней
                // полное дублирование периода
                {"0#0#21#45#22#15;" + "0#0#0#0#23#0", dayScheduleDuplicated()}, // 0;0 -> 0;0
                {"3#5#21#45#22#15;" + "3#5#0#0#23#0", dayScheduleDuplicated()}, // 3;5 -> 3;5
                {"3#5#21#45#22#15;" + "1#2#0#0#23#0;" + "3#5#0#0#23#0", dayScheduleDuplicated()}, // 3;5 -> 3;5

                // пересечение граничных дней периодов (присутствует период из одного дня)
                {"0#1#21#45#22#15;" + "1#1#0#0#23#0", dayScheduleDuplicated()}, // 0;1 -> 1;1
                {"0#1#21#45#22#15;" + "0#0#0#0#23#0", dayScheduleDuplicated()}, // 0;1 -> 0;0
                {"0#0#21#45#22#15;" + "0#1#0#0#23#0", dayScheduleDuplicated()}, // 0;0 -> 0;1
                {"1#1#21#45#22#15;" + "0#1#0#0#23#0", dayScheduleDuplicated()}, // 1;1 -> 0;1
                {"1#1#21#45#22#15;" + "0#1#0#0#23#0", dayScheduleDuplicated()}, // 1;1 -> 0;1

                // пересечение граничных дней периодов
                {"1#3#21#45#22#15;" + "3#5#0#0#23#0", dayScheduleDuplicated()}, // 1;3 -> 3;5
                {"3#5#21#45#22#15;" + "1#3#0#0#23#0", dayScheduleDuplicated()}, // 3;5 -> 1;3

                // включение одного периода в другой
                {"2#6#21#45#22#15;" + "3#3#0#0#23#0", dayScheduleDuplicated()}, // 2;6 -> 3;3
                {"2#6#21#45#22#15;" + "3#4#0#0#23#0", dayScheduleDuplicated()}, // 2;6 -> 3;4
                {"3#3#21#45#22#15;" + "2#6#0#0#23#0", dayScheduleDuplicated()}, // 3;3 -> 2;6
                {"3#4#21#45#22#15;" + "2#6#0#0#23#0", dayScheduleDuplicated()}, // 3;4 -> 2;6
                {"3#4#21#45#22#15;" + "0#0#0#0#23#0;" + "2#6#0#0#23#0", dayScheduleDuplicated()}, // 3;4 -> 2;6
        });
    }

    @Test
    public void testWorkTimeConstraint() {
        Defect defect = workTimeIsValid().apply(workTime);
        if (expectedDefect == null) {
            assertThat("констрейнт не должен выдавать ошибку на время работы = " + workTime,
                    defect, nullValue());
        } else {
            assertThat("ошибка для времени работы = " + workTime + " не соответствует ожидаемой",
                    defect, is(expectedDefect));
        }
    }
}
