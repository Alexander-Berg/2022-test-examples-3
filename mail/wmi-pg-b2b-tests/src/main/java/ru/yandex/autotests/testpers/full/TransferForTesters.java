package ru.yandex.autotests.testpers.full;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.google.common.net.HttpHeaders;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.qatools.properties.DefaultValue;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Required;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static com.github.rholder.retry.StopStrategies.stopAfterAttempt;
import static com.github.rholder.retry.WaitStrategies.fixedWait;
import static com.google.common.base.Predicates.isNull;
import static com.jayway.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.testpers.manual.LaunchMigration.*;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Aqua.Test
@Title("Тестирование миграции сравнением")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransferForTesters {
    
    private static final Logger LOG = LogManager.getLogger(TransferForTesters.class);

    public static final String ORA2PG_PACKAGE_TRANSFER = "ora2pg-package-transfer";

    public static final Retryer<String> RETRYER = RetryerBuilder.<String>newBuilder()
            .retryIfResult(isNull())
            .withStopStrategy(stopAfterAttempt(5))
            .withWaitStrategy(fixedWait(5, SECONDS)).build();

    public static final URI TRANSFER = UriBuilder.fromPath("/job/{job}/buildWithParameters")
            .scheme("https").host(JENKINS).port(443)
            .build(ORA2PG_PACKAGE_TRANSFER);

    @Parameter("Логин")
    @Property("transfer.login")
    @Required
    public String login;

    @Parameter("В какую сторону")
    @Property("transfer.to.db")
    @DefaultValue("postgre:1")
    public String toDb;
    
    @Parameter("Перенос настроек")
    @Property("transfer.settings")
    @DefaultValue("true")
    public boolean transferSettings;

    @Before
    public void setUp() throws Exception {
        PropertyLoader.newInstance().populate(this);
    }

    @Test
    @Title("[Трансфер] Джобой " + ORA2PG_PACKAGE_TRANSFER)
    public void cTransferORAtoPG() throws Exception {
        String location = given().auth().preemptive().basic(LOGIN, TOKEN)
                .filter(log())
                .formParam("LOGIN", login)
                .formParam("TRANSFER_SETTINGS", transferSettings)
                .formParam("TO_DB", toDb)
                .expect()
                .post(TRANSFER).header(HttpHeaders.LOCATION);

        shouldWaitForJobSuccessStatus(location);
    }

    private void shouldWaitForJobSuccessStatus(String location) throws Exception {
        String path = RETRYER.call(() -> given().filter(log()).port(443).get(location + "api/json").path("executable?.url"));
        String result = RETRYER.call(() -> given().filter(log()).port(443).get(path + "api/json").path("result"));
        assertThat(result, equalTo("SUCCESS"));
    }
}
