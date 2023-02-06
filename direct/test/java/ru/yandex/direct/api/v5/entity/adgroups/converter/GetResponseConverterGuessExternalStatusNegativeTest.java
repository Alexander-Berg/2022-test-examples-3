package ru.yandex.direct.api.v5.entity.adgroups.converter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.guessExternalStatus;

@RunWith(Parameterized.class)
public class GetResponseConverterGuessExternalStatusNegativeTest {

    @Parameterized.Parameter
    public StatusModerate statusModerate;

    @Parameterized.Parameter(1)
    public StatusPostModerate statusPostModerate;

    @Parameterized.Parameters(name = "statusModerate {0} & statusPostModerate {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {StatusModerate.YES, StatusPostModerate.NEW},
                {StatusModerate.YES, StatusPostModerate.NO},
                {StatusModerate.YES, StatusPostModerate.READY},
                {StatusModerate.YES, StatusPostModerate.SENT},
                {StatusModerate.YES, StatusPostModerate.REJECTED},
        };
    }

    @Test
    public void test() {
        assertThatThrownBy(() -> guessExternalStatus(statusModerate, statusPostModerate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not guess status for adgroup with statusModerate = " + statusModerate
                        + " and statusPostModerate = " + statusPostModerate);
    }
}
