package ru.yandex.direct.liveresource;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class LiveResourceFactoryTest {
    private static final String CLASSPATH_RESOURCE_PATH = "/ru/yandex/direct/liveresource/test";
    private static final String CLASSPATH_CONTENT = "test content";
    private static final String MEMORY_CONTENT = "memory content";

    private static final String FILE_CONTENT = "file content";
    private File file;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        file = folder.newFile();
        FileUtils.writeStringToFile(file, FILE_CONTENT, StandardCharsets.UTF_8);
    }

    @Test
    public void get_ClasspathResource() {
        assertThat(LiveResourceFactory.get("classpath://" + CLASSPATH_RESOURCE_PATH).getContent(),
                startsWith(CLASSPATH_CONTENT));
    }

    @Test
    public void get_FileResource() {
        assertThat(LiveResourceFactory.get("file://" + file.getAbsolutePath()).getContent(),
                startsWith(FILE_CONTENT));
    }

    @Test
    public void get_MemoryResource() {
        assertThat(LiveResourceFactory.get("memory://" + MEMORY_CONTENT).getContent(), equalTo(MEMORY_CONTENT));
    }

    @Test(expected = LiveResourceReadException.class)
    public void get_IncorrectClasspath() {
        LiveResource liveResource = LiveResourceFactory.get("classpath://" + CLASSPATH_RESOURCE_PATH + ".incorrect");
        liveResource.getContent();
    }

    @Test(expected = LiveResourceReadException.class)
    public void get_IncorrectFile() {
        LiveResource liveResource = LiveResourceFactory.get("file://" + file.getAbsolutePath() + ".incorrect");
        liveResource.getContent();
    }

    @Test
    public void get_FileFromToUri() {
        assertThat(LiveResourceFactory.get(file.toURI().toString()).getContent(),
                startsWith(FILE_CONTENT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void get_IncorrectProtocol() {
        LiveResourceFactory.get("air://" + file.getAbsolutePath());
    }
}
