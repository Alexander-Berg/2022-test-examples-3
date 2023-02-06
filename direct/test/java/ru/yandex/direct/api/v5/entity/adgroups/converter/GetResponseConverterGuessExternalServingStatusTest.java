package ru.yandex.direct.api.v5.entity.adgroups.converter;

import com.yandex.direct.api.v5.general.ServingStatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.guessExternalServingStatus;

@RunWith(Parameterized.class)
public class GetResponseConverterGuessExternalServingStatusTest {

    @Parameterized.Parameter
    public Boolean bsRarelyLoaded;

    @Parameterized.Parameter(1)
    public ServingStatusEnum expectedStatus;

    @Parameterized.Parameters(name = "bsRarelyLoaded {0} => serving status {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {Boolean.FALSE, ServingStatusEnum.ELIGIBLE},
                {Boolean.TRUE, ServingStatusEnum.RARELY_SERVED},
        };
    }

    @Test
    public void test() {
        assertThat(guessExternalServingStatus(bsRarelyLoaded)).isEqualTo(expectedStatus);
    }
}
