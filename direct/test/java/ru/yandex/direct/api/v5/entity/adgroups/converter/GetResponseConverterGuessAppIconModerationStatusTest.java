package ru.yandex.direct.api.v5.entity.adgroups.converter;

import com.yandex.direct.api.v5.general.StatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.guessAppIconModerationStatus;

@RunWith(Parameterized.class)
public class GetResponseConverterGuessAppIconModerationStatusTest {

    @Parameterized.Parameter
    public StatusIconModerate statusIconModerate;

    @Parameterized.Parameter(1)
    public StatusEnum expectedStatus;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {StatusIconModerate.SENDING, StatusEnum.MODERATION},
                {StatusIconModerate.SENT, StatusEnum.MODERATION},
                {StatusIconModerate.READY, StatusEnum.MODERATION},
                {StatusIconModerate.NO, StatusEnum.REJECTED},
                {StatusIconModerate.YES, StatusEnum.ACCEPTED},
        };
    }

    @Test
    public void test() {
        assertThat(guessAppIconModerationStatus(statusIconModerate)).isEqualTo(expectedStatus);
    }
}
