package ru.yandex.direct.env;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.rules.TemporaryFolder;

import static ru.yandex.direct.env.EnvironmentTypeReader.ENV_NAME;
import static ru.yandex.direct.env.EnvironmentTypeReader.PROPERTY_NAME;

public abstract class EnvironmentTypeReaderBaseTest {

    @Rule
    public ProvideSystemProperty propRule;

    @Rule
    public EnvironmentVariables envRule = new EnvironmentVariables();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected File tmpFile, tmpFile2;
    protected String fileCont, addFileCont;
    protected EnvironmentType expectedResult;

    public EnvironmentTypeReaderBaseTest(String prop, String env, String fileCont, String addFileCont,
                                         EnvironmentType expectedResult) throws IOException {
        propRule = new ProvideSystemProperty(PROPERTY_NAME, prop);
        envRule.set(ENV_NAME, env);
        this.fileCont = fileCont;
        this.addFileCont = addFileCont;
        this.expectedResult = expectedResult;
    }

    @Before
    public void before() throws IOException {
        tmpFile = createSourceFile(fileCont);
        tmpFile2 = createSourceFile(addFileCont);
    }

    private File createSourceFile(String data) throws IOException {
        var file = folder.newFile();
        if (data != null) {
            FileUtils.writeStringToFile(file, data, StandardCharsets.UTF_8);
        } else {
            file.delete();
        }
        return file;
    }
}
