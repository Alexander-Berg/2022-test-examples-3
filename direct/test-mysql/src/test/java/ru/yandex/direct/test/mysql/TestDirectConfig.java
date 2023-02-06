package ru.yandex.direct.test.mysql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

public class TestDirectConfig {
    @Test
    public void testDirectConfig() throws IOException {
        TestMysqlConfig config = TestMysqlConfig.directConfig();
        if (TestDirectConfig.class.getResource(config.getDockerImageFilename()) == null) {
            Assert.fail(config.getDockerImageFilename() + " not found.");
        }

        try (
                InputStream stream = TestDirectConfig.class.getResourceAsStream(config.getDockerImageFilename());
                InputStreamReader reader = new InputStreamReader(stream);
                BufferedReader bufferedReader = new BufferedReader(reader)
        ) {
            String imageTag = bufferedReader.readLine();
            if (bufferedReader.readLine() != null) {
                Assert.fail("Should not contain more than one line");
            }
            Assert.assertNotNull("Image tag not present", imageTag);
            Assert.assertFalse("Should not contain dirty images", imageTag.contains("dirty"));

        }
    }
}
