package ru.yandex.market.pers.test.common;

import java.io.IOException;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 05.03.2021
 */
@SpringBootTest(classes = {
    CoreMockConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WebAppConfiguration
public class AbstractPersWebTest {
    @Autowired
    protected MockMvc mockMvc;

    protected void applySqlScript(DataSource dataSource, String sql) {
        ResourceDatabasePopulator scriptLauncher = new ResourceDatabasePopulator();
        scriptLauncher.addScript(new ClassPathResource(sql));
        scriptLauncher.execute(dataSource);
    }

    protected static String fileToString(String bodyFileName) throws IOException {
        return IOUtils.toString(AbstractPersWebTest.class.getResourceAsStream(bodyFileName), "UTF-8");
    }
}
