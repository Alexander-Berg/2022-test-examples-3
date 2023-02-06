package ru.yandex.market.olap2.load;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;
import ru.yandex.market.olap2.ytreflect.YtTestTable;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class YtLoaderITest {

    @Autowired
    private YtLoader ytLoader;

    @Test
    public void mustLoad() throws IOException {
        LoadTask task = new TestLoadTask("loadtstsevid1", YtTestTable.TBL);
        ytLoader.download(task);
        assertTrue(new File(task.getFile()).length() > 0);
    }
}
