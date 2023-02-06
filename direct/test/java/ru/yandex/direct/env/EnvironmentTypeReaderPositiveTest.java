package ru.yandex.direct.env;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.env.EnvironmentType.DEV7;
import static ru.yandex.direct.env.EnvironmentType.DEVELOPMENT;
import static ru.yandex.direct.env.EnvironmentType.PRODUCTION;
import static ru.yandex.direct.env.EnvironmentType.TESTING;
import static ru.yandex.direct.env.EnvironmentTypeReader.ENV_NAME;
import static ru.yandex.direct.env.EnvironmentTypeReader.PROPERTY_NAME;

@NotThreadSafe
@RunWith(Parameterized.class)
public class EnvironmentTypeReaderPositiveTest extends EnvironmentTypeReaderBaseTest {

    public EnvironmentTypeReaderPositiveTest(String prop, String env, String fileCont, String addFileCont,
                                             EnvironmentType result) throws IOException {
        super(prop, env, fileCont, addFileCont, result);
    }

    @Parameterized.Parameters(name = "prop: {0} env: {1} file: {2}")
    public static Iterable<Object[]> envsData() {
        // prop, env, file, file2 -- result
        return Arrays.asList(new Object[][]{
                // can read from each source
                {TESTING.toString(), null, null, null, TESTING},
                {null, TESTING.toString(), null, null, TESTING},
                {null, null, TESTING.toString(), null, TESTING},
                {null, null, DEVELOPMENT.toString(), DEV7.toString(), DEV7},
                // sources priority
                {PRODUCTION.toString(), TESTING.toString(), DEVELOPMENT.toString(), DEV7.toString(), PRODUCTION},
                {null, TESTING.toString(), DEVELOPMENT.toString(), DEV7.toString(), TESTING},
                {null, null, PRODUCTION.toString(), DEV7.toString(), PRODUCTION},
                {null, null, TESTING.toString(), "invalid", TESTING},
                {PRODUCTION.toString(), "invalid", "invalid", "invalid", PRODUCTION},
                // default value
                {null, null, null, null, EnvironmentType.DEFAULT},
                {null, null, null, DEV7.toString(), EnvironmentType.DEFAULT},
                {"", "", "", "", EnvironmentType.DEFAULT}
        });
    }

    @Test
    public void read_withValidParameters() {
        var env = EnvironmentTypeReader.read(PROPERTY_NAME, ENV_NAME, tmpFile.getAbsolutePath(),
                tmpFile2.getAbsolutePath());
        assertThat(env, Matchers.is(expectedResult));
    }
}
