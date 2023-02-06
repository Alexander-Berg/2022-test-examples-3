package ru.yandex.market.load.admin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        }
)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "juggler.check.age.max=320",
        "tsum.oauth.token=",
        "yql.secret.token.name=test",
        "yql.jdbc.url=test",
        "yql.jdbc.username=unused",
        "mj.tvm.secret=tvmSecret",
        "load.admin.auth.enabled=false",
        "load.admin.host=load-admin2.vs.market.yandex.net",
        "load.admin.tvm.client.id=123",
        "load.admin.tvm.client.secret=secret",
        "load.admin.blackbox.user.agent=market-load-admin",
        "load.admin.blackbox.url=https://blackbox.yandex-team.ru/blackbox",
        "load.admin.blackbox.tvm.id=2",
        "load.admin.passport.require-https=true",
        "load.admin.passport.login.url=https://passport.yandex-team.ru/passport?mode=auth&retpath=https%3A%2F%2F${load-admin.host:}%2F",
        "promo.clusters=test",
        "promo.table=//promo",
        "yt.token=123",
        "yt.user=test",
        "startrek.api.url=https://st-api.yandex-team.ru",
        "startrek.oauth.token=",
        "solomon.api.url=https://solomon.yandex-team.ru/api",
        "solomon.oauth.token=",
        "checkouter.testing.tvm.id=2010068",
        "checkouter.testing.api.url=https://checkouter.tst.vs.market.yandex.net:39011",
        "load.admin.startrek.production.enabled=false",
        "load.admin.startrek.testing.enabled=true",
        "load.admin.jobs.workflow.interval.ms=500",
        "lom.testing.tvm.id=2011682",
        "lom.production.tvm.id=2011680",
        "lom.testing.api.url=https://logistics-lom.tst.vs.market.yandex.net",
        "lom.production.api.url=https://logistics-lom.vs.market.yandex.net",
        "tracker.testing.tvm.id=2011818",
        "tracker.testing.api.url=http://delivery-tracker-api.tst.vs.market.yandex.net:35700",
        "tracker.production.tvm.id=2011820",
        "tracker.production.api.url=http://delivery-tracker-api.vs.market.yandex.net:35700",
        "load.admin.canceller.testing.enabled=true",
        "load.admin.canceller.production.enabled=true",
        "load.admin.telegram.production.enabled=false",
        "load.admin.telegram.testing.enabled=true",
        "load.admin.access.token=",
        "load.admin.team.abc.service=marketapi",
        "load.admin.team.abc.role=developer",
        "staff.api.url=https://staff-api.yandex-team.ru/"
})
public abstract class AbstractFunctionalTest {
    @Autowired
    private Scheduler scheduler;

    @AfterEach
    public void tearDown() throws SchedulerException {
        scheduler.clear();
    }
}

