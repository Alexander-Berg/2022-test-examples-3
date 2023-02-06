package ru.yandex.market.sberlog_tms;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import liquibase.integration.spring.SpringLiquibase;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.market.sberlog_tms.TestController.TestController;
import ru.yandex.market.sberlog_tms.cipher.CipherService;
import ru.yandex.market.sberlog_tms.config.CipherConfig;
import ru.yandex.market.sberlog_tms.dao.SberlogDbDao;
import ru.yandex.market.sberlog_tms.lock.LockService;
import ru.yandex.market.sberlog_tms.yt.YtUploader;

import java.io.IOException;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 21.10.19
 */
@PropertySource("classpath:local/application-local.properties")
@Configuration
public class SberlogtmsConfig {

    private final String ytProxy = System.getenv("YT_PROXY"); //"localhost:28163"
    private final int httpPort = Integer.valueOf(ytProxy.split(":")[1]) + 1; //httpPort = ytProxy + 1;

    @Value("${sberlogtms.yt.table.path}")
    private String dataPath;
    @Value("${sberlog.secret.salt}")
    private String salt;
    @Value("${sberlogtms.secret.db}")
    private String secretDb;

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
        CipherConfig cipherConfig = new CipherConfig();
        cipherConfig.setSecretDb(secretDb);
        cipherConfig.setSalt(salt);

        return cipherConfig.cipherService();
    }

    @Bean
    public SberlogDbDao sberlogDbDao(JdbcTemplate jdbcTemplate, CipherService cipherService) {
        return new SberlogDbDao(jdbcTemplate, cipherService);
    }

    @Bean
    public YtUploader ytUploader() {
        return new YtUploader(ytTable(), dataPath);
    }

    @Bean
    public LockService lockService() {
        return new LockService(ytCypress(), ytTable(), dataPath);
    }

    private Yt ytTable() {
        YtConfiguration ytConfiguration = YtConfiguration.builder()
                .withApiHost(ytProxy)
                .withToken("")
                .build();
        return Yt.builder(ytConfiguration).http().build();
    }

    private Cypress ytCypress() {
        YtConfiguration ytConfiguration = YtConfiguration.builder()
                .withApiHost(ytProxy)
                .withToken("")
                .build();
        return Yt.builder(ytConfiguration).http().build().cypress();
    }

    @Bean
    public Server startServer() throws Exception {
        Server server = new Server(httpPort);
        server.setStopAtShutdown(true);

        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        servletHandler.addServletWithMapping(TestController.class, "/*");

        server.start();

        return server;
    }
}
