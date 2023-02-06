package ru.yandex.direct.env;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static ru.yandex.direct.env.EnvironmentTypeReader.ENV_NAME;
import static ru.yandex.direct.env.EnvironmentTypeReader.PROPERTY_NAME;

@NotThreadSafe
@RunWith(Parameterized.class)
public class EnvironmentTypeReaderNegativeTest extends EnvironmentTypeReaderBaseTest {

    public EnvironmentTypeReaderNegativeTest(String prop, String env, String fileCont, String addFileCont,
                                             EnvironmentType result) throws IOException {
        super(prop, env, fileCont, addFileCont, result);
    }

    @Parameterized.Parameters(name = "prop: {0} env: {1} file: {2}")
    public static Iterable<Object[]> envsData() {
        return Arrays.asList(
                new Object[][]{
                        // prop, env, file, file2 -- result
                        {"invalid", null, null, null, null},
                        {null, "invalid", null, null, null},
                        {null, null, "invalid", null, null},
                }
        );
    }

    @Test(expected = EnvironmentTypeReader.ReadException.class)
    public void read_withInvalidParameters() {
        EnvironmentTypeReader.read(PROPERTY_NAME, ENV_NAME, tmpFile.getAbsolutePath(), tmpFile2.getAbsolutePath());
    }
}
