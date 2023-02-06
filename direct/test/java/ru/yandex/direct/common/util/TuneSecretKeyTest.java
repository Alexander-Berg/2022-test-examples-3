package ru.yandex.direct.common.util;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class TuneSecretKeyTest {

    /**
     * SECRET_KEY_FOR_UID и SECRET_KEY_FOR_YANDEX_UID_COOKIE посчитаны для этого кол-во дней
     */
    private static final long DAYS = 17574;
    private static final long UID = 465398871;
    private static final String SECRET_KEY_FOR_UID = "u70c11ae5ad83e06b44a71608326d18e2";
    private static final String YANDEX_UID_COOKIE = "736683661509881516";
    private static final String SECRET_KEY_FOR_YANDEX_UID_COOKIE = "ye1c9a95834df4ab5b86a352635c589f1";

    @Parameterized.Parameter
    public Long uid;

    @Parameterized.Parameter(value = 1)
    public String yandexUidCookie;

    @Parameterized.Parameter(value = 2)
    public String expectedSecretKey;

    @Parameterized.Parameters()
    public static Collection<Object[]> getParameters() {
        return asList(
                new Object[]{null, null, null},
                new Object[]{UID, null, SECRET_KEY_FOR_UID},
                new Object[]{UID, YANDEX_UID_COOKIE, SECRET_KEY_FOR_UID},
                new Object[]{null, YANDEX_UID_COOKIE, SECRET_KEY_FOR_YANDEX_UID_COOKIE}
        );
    }


    @Test
    public void checkTuneSecretKey() {
        String tuneSecretKey = TuneSecretKey.generateSecretKey(DAYS, uid, yandexUidCookie);
        assertThat(tuneSecretKey).isEqualTo(expectedSecretKey);
    }
}
