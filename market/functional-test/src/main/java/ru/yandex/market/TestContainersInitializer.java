package ru.yandex.market;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.OracleContainer;

import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

public class TestContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    static Logger log = LoggerFactory.getLogger(TestContainersInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        OracleContainer oracleContainer = StaticOracleContainer.getInstance();
        oracleContainer.followOutput(s -> log.trace(s::getUtf8String));

        final String jdbcUrl = String.format(
                "jdbc:oracle:thin:@//%s:%s/XE",
                oracleContainer.getContainerIpAddress(),
                oracleContainer.getOraclePort()
        );

        addInlinedPropertiesToEnvironment(
                applicationContext,
                "liquibase.cs_billing.cs_billing.jdbc.url=" + jdbcUrl,
                "cs_billing.cs_billing.jdbc.url=" + jdbcUrl,
                "mb_stat_report.billing.jdbc.url=" + jdbcUrl,
                "java_sec.billing.jdbc.url=" + jdbcUrl,
                "vendors.billing.jdbc.url=" + jdbcUrl,
                "cs_billing_tms.cs_billing.jdbc.url=" + jdbcUrl
        );
    }
}
