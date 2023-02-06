package ru.yandex.market.mbo.synchronizer.export;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.fail;

/**
 * Created by sergtru on 10.03.2017.
 */
public class JsonValidatorTest {
    private Path tmpDir;

    @Before
    public void setUp() throws Exception {
        tmpDir = Files.createTempDirectory("mbo-test-");
    }

    @After
    public void tearDown() throws Exception {
        MoreFiles.deleteRecursively(tmpDir, RecursiveDeleteOption.ALLOW_INSECURE);
    }

    @Test
    public void testUnicode() throws Exception {
        testFor("{\"key\":\"\\u04A8\"}", true);
        testFor("{\"key\":\"\u04A8\"}", true);
    }

    @Test
    public void testSlash() throws Exception {
        testFor("{\"key\":\"slashed\\\\value\"}", true);
    }

    @Test
    public void testFailOnSlash() throws Exception {
        testFor("{\"key\":\"slashed\\value\"}", false);
    }

    @Test
    public void testFailOnClosedTagAbsent() throws Exception {
        testFor("{\"key\":\"value\"", false);
    }

    @Test
    public void testFailOnTail() throws Exception {
        testFor("{\"key\":\"value\"}tail", false);
    }

    @Test
    public void testFailOnWrongSeparator() throws Exception {
        testFor("{\"key\":\"value\"; \"key2\":\"value2\"}", false);
        testFor("[\"value\"; \"value2\"]", false);
    }

    private void testFor(String json, boolean shouldAccept) throws Exception {
        File testFile = tmpDir.resolve("validator-test.json").toFile();
        try (Writer writer = ExporterUtils.getWriter(testFile)) {
            writer.append(json);
        }
        JsonValidator validator = new JsonValidator();
        boolean validationResult = validator.validate(testFile.getAbsolutePath());
        if (validationResult != shouldAccept) {
            if (shouldAccept) {
                fail("Validator should accept valid json " + json);
            } else {
                fail("Validator should not accept invalid json " + json);
            }
        }
    }
}
