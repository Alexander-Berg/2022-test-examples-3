package ru.yandex.mail.promocode.mocks;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import com.opentable.db.postgres.embedded.FlywayPreparer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;

import ru.yandex.mail.promocode.dao.PromoCodeRepository;

/**
 * @author Sergey Galyamichev
 */
@Profile("test")
@Configuration
@ImportResource("classpath*:spring/webservice-ctx.xml")
public class TestConfiguration {
    @Bean(destroyMethod = "close")
    public EmbeddedPostgres postgres() throws Exception {
        return EmbeddedPostgres.start();
    }

    @Bean
    public PromoCodeRepository sentEmailsDao(EmbeddedPostgres ps) throws Exception {
        FlywayPreparer flywayPreparer = FlywayPreparer.forClasspathLocation("db/migration");
        flywayPreparer.prepare(ps.getPostgresDatabase());
        return new PromoCodeRepository(ps.getPostgresDatabase());
    }
}
