package ru.yandex.direct.api.v5.entity.adgroups.converter;

import com.yandex.direct.api.v5.general.StatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.guessExternalStatus;

@RunWith(Parameterized.class)
public class GetResponseConverterGuessExternalStatusPositiveTest {

    @Parameterized.Parameter
    public StatusModerate statusModerate;

    @Parameterized.Parameter(1)
    public StatusPostModerate statusPostModerate;

    @Parameterized.Parameter(2)
    public StatusEnum expectedStatus;

    @Parameterized.Parameters(name = "statusModerate {0} & statusPostModerate {1} => status {2}")
    public static Object[][] getParameters() {
        return new Object[][]{
                // rejected
                {StatusModerate.NO, StatusPostModerate.NEW, StatusEnum.REJECTED},
                {StatusModerate.NO, StatusPostModerate.NO, StatusEnum.REJECTED},
                {StatusModerate.NO, StatusPostModerate.READY, StatusEnum.REJECTED},
                {StatusModerate.NO, StatusPostModerate.REJECTED, StatusEnum.REJECTED},
                {StatusModerate.NO, StatusPostModerate.SENT, StatusEnum.REJECTED},
                {StatusModerate.NO, StatusPostModerate.YES, StatusEnum.REJECTED},
                // draft
                {StatusModerate.NEW, StatusPostModerate.NEW, StatusEnum.DRAFT},
                {StatusModerate.NEW, StatusPostModerate.NO, StatusEnum.DRAFT},
                {StatusModerate.NEW, StatusPostModerate.READY, StatusEnum.DRAFT},
                {StatusModerate.NEW, StatusPostModerate.REJECTED, StatusEnum.DRAFT},
                {StatusModerate.NEW, StatusPostModerate.SENT, StatusEnum.DRAFT},
                {StatusModerate.NEW, StatusPostModerate.YES, StatusEnum.DRAFT},
                // moderation
                {StatusModerate.SENT, StatusPostModerate.NEW, StatusEnum.MODERATION},
                {StatusModerate.SENT, StatusPostModerate.NO, StatusEnum.MODERATION},
                {StatusModerate.SENT, StatusPostModerate.READY, StatusEnum.MODERATION},
                {StatusModerate.SENT, StatusPostModerate.SENT, StatusEnum.MODERATION},
                {StatusModerate.SENT, StatusPostModerate.REJECTED, StatusEnum.MODERATION},
                {StatusModerate.SENDING, StatusPostModerate.NEW, StatusEnum.MODERATION},
                {StatusModerate.SENDING, StatusPostModerate.NO, StatusEnum.MODERATION},
                {StatusModerate.SENDING, StatusPostModerate.READY, StatusEnum.MODERATION},
                {StatusModerate.SENDING, StatusPostModerate.SENT, StatusEnum.MODERATION},
                {StatusModerate.SENDING, StatusPostModerate.REJECTED, StatusEnum.MODERATION},
                {StatusModerate.READY, StatusPostModerate.NEW, StatusEnum.MODERATION},
                {StatusModerate.READY, StatusPostModerate.NO, StatusEnum.MODERATION},
                {StatusModerate.READY, StatusPostModerate.READY, StatusEnum.MODERATION},
                {StatusModerate.READY, StatusPostModerate.SENT, StatusEnum.MODERATION},
                {StatusModerate.READY, StatusPostModerate.REJECTED, StatusEnum.MODERATION},
                // preaccepted
                {StatusModerate.SENT, StatusPostModerate.YES, StatusEnum.PREACCEPTED},
                {StatusModerate.SENDING, StatusPostModerate.YES, StatusEnum.PREACCEPTED},
                {StatusModerate.READY, StatusPostModerate.YES, StatusEnum.PREACCEPTED},
                // accepted
                {StatusModerate.YES, StatusPostModerate.YES, StatusEnum.ACCEPTED},
        };
    }

    @Test
    public void test() {
        assertThat(guessExternalStatus(statusModerate, statusPostModerate)).isEqualTo(expectedStatus);
    }
}
