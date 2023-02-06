package ru.yandex.direct.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DirectConfigFromFileTest {

    @Test
    public void readConfigFromFile_success() throws Exception {
        Config config = DirectConfigFactory
                .readConfigFromFile(new ClassPathResource("sample-config.conf").getFile().getAbsolutePath());
        assertEquals(15, config.getInt("db_shards"));
        assertEquals("direct-dev-letters@yandex-team.ru", config.getString("sendmail.redirect_address"));
    }

    @Test
    public void readConfigFromFileOrNull_success() throws Exception {
        Config config = DirectConfigFactory
                .readConfigFromFileOrNull(new ClassPathResource("sample-config.conf").getFile().getAbsolutePath());
        assertEquals(15, config.getInt("db_shards"));
        assertEquals("direct-dev-letters@yandex-team.ru", config.getString("sendmail.redirect_address"));
    }

    @Test(expected = ConfigException.class)
    public void readConfigFromFile_fail() {
        DirectConfigFactory.readConfigFromFile("/no-such-file.conf");
    }

    @Test
    public void readConfigFromFileOrNull_fail() {
        assertNull(DirectConfigFactory.readConfigFromFileOrNull("/no-such-file.conf"));
    }
}
