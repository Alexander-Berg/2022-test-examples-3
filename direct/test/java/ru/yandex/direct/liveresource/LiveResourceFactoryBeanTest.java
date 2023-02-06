package ru.yandex.direct.liveresource;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.config.EssentialConfiguration;
import ru.yandex.direct.liveresource.provider.LiveResourceFactoryBean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = EssentialConfiguration.class)
public class LiveResourceFactoryBeanTest {

    private static final String CLASSPATH_RESOURCE_PATH = "/ru/yandex/direct/liveresource/test";
    private static final String CLASSPATH_CONTENT = "test content";
    private static final String MEMORY_CONTENT = "memory content";

    private static final String FILE_CONTENT = "file content";

    @Autowired
    public LiveResourceFactoryBean liveResourceFactoryBean;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File file;

    @Before
    public void setUp() throws Exception {
        file = folder.newFile();
        FileUtils.writeStringToFile(file, FILE_CONTENT, StandardCharsets.UTF_8);
    }

    @Test
    public void get_ClasspathResource() {
        assertThat(liveResourceFactoryBean.get("classpath://" + CLASSPATH_RESOURCE_PATH).getContent(),
                startsWith(CLASSPATH_CONTENT));
    }

    @Test
    public void get_FileResource() {
        assertThat(liveResourceFactoryBean.get("file://" + file.getAbsolutePath()).getContent(),
                startsWith(FILE_CONTENT));
    }

    @Test
    public void get_MemoryResource() {
        assertThat(liveResourceFactoryBean.get("memory://" + MEMORY_CONTENT).getContent(), equalTo(MEMORY_CONTENT));
    }

    @Test(expected = LiveResourceReadException.class)
    public void get_IncorrectClasspath() {
        liveResourceFactoryBean.get("classpath://" + CLASSPATH_RESOURCE_PATH + ".incorrect").getContent();
    }

    @Test(expected = LiveResourceReadException.class)
    public void get_IncorrectFile() {
        liveResourceFactoryBean.get("file://" + file.getAbsolutePath() + ".incorrect").getContent();
    }

    @Test
    public void get_FileFromToUri() {
        assertThat(liveResourceFactoryBean.get(file.toURI().toString()).getContent(),
                startsWith(FILE_CONTENT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void get_IncorrectProtocol() {
        liveResourceFactoryBean.get("air://" + file.getAbsolutePath());
    }

    @Test
    public void get_UserHomeFile() {
        String filePath = ".direct-tokens/filename";
        LiveResource liveResource = liveResourceFactoryBean.get("file://~/" + filePath);
        assertThat(liveResource.getLocation(), equalTo(System.getProperty("user.home") + File.separator + filePath));
    }
}

