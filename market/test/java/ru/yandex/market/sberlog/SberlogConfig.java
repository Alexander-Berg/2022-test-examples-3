package ru.yandex.market.sberlog;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sberlog.cache.LocalCacheService;
import ru.yandex.market.sberlog.cipher.CipherService;
import ru.yandex.market.sberlog.config.CipherConfig;
import ru.yandex.market.sberlog.dao.ManipulateSessionDao;

import java.io.IOException;
import java.util.Random;

import javax.sql.DataSource;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 18.02.19
 */
@Configuration
public class SberlogConfig {

    @Bean
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder().start();
    }


    @Bean
    public JdbcTemplate jdbcTemplate(EmbeddedPostgres embeddedPostgres) {
        return new JdbcTemplate(embeddedPostgres.getPostgresDatabase());
    }


    @Bean
    public SpringLiquibase springLiquibase(EmbeddedPostgres embeddedPostgres) {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setDataSource(embeddedPostgres.getPostgresDatabase());
        springLiquibase.setDefaultSchema("public");
        springLiquibase.setChangeLog("db/changelog/db.test.change-master.xml");
        return springLiquibase;
    }

    @Bean
    public CipherService cipherService() {
        Random r = new Random();
        String secretDb = String.valueOf(r.nextLong());
        String secretCookie = String.valueOf(r.nextLong());
        String salt = String.valueOf(r.nextLong());

        CipherConfig cipherConfig = new CipherConfig();
        cipherConfig.setDbKeyVersion("1");
        cipherConfig.setSecretCookieVersion("1");
        cipherConfig.setSecretDb(secretDb);
        cipherConfig.setSalt(salt);
        cipherConfig.setSecretCookie(secretCookie);

        return cipherConfig.cipherService();
    }

    @Bean
    public ManipulateSessionDao manipulateSessionDao(JdbcTemplate jdbcTemplate, CipherService cipherService) {
        return new ManipulateSessionDao(jdbcTemplate, cipherService);
    }

}
