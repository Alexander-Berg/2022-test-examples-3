package ru.yandex.market.logshatter.parser.front;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

import static ru.yandex.market.logshatter.parser.trace.Environment.DEVELOPMENT;

/**
 * @author Roman Garanko<a href="mailto:mrgrien@yandex-team.ru"></a>
 * @date 22/04/16
 */
public class NodeJsErrorsLogParserTest {
    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new NodeJsErrorsLogParser());
        checker.setOrigin("market-health-dev");
        checker.setParam("logbroker://market-health-dev", "DEVELOPMENT");
        checker.setInstanceId(31337);
    }

    @Test
    public void parseNodeJsError() throws Exception {
        String line = "2016/02/17 07:30:50 +0300\t[worker:id=1,pid=24253]\terror\tAPP\tUSER_REGION_ERROR\t9a70a74f137e908a13e24426076b032f\tUserError: Region initialization failed (requestId: 9a70a74f137e908a13e24426076b032f, step: \"ensureTimezone\")";
        checker.setFile("/var/log/yandex/market-skubi/app.stdout.log");
        checker.check(
            line,
            new Date(1455683450000L), checker.getHost(), "APP", "USER_REGION_ERROR", "", "", -1, "", "error", "9a70a74f137e908a13e24426076b032f", DEVELOPMENT, checker.getInstanceId()
        );

        line = "2016/02/17 12:49:10 +0300\t[worker:id=2,pid=24255]\terror\tRESOURCE\tBAD_PARAM_TO_CALL\treport.getRecommendationsOffers\t-\t-\t1489330900384/36ab5ed17f13aca1b641627082229197/2\tBAD_PARAM_TO_CALL no valid offers to search recommendations for.";
        checker.check(
            line,
            new Date(1455702550000L), checker.getHost(), "RESOURCE", "BAD_PARAM_TO_CALL", "report", "getRecommendationsOffers", -1, "", "error", "1489330900384/36ab5ed17f13aca1b641627082229197/2", DEVELOPMENT, checker.getInstanceId()
        );
    }

    @Test
    public void severityErrorFiltered() throws Exception {
        checker.setFile("/var/log/yandex/market-skubi/app.stdout.log");
        checker.check(
                "2016/02/17 07:30:50 +0300\t[worker:id=1,pid=24253]\terror\tAPP\tUSER_REGION_ERROR\t9a70a74f137e908a13e24426076b032f\tUserError: Region initialization failed (requestId: 9a70a74f137e908a13e24426076b032f, step: \"ensureTimezone\")",
                new Date(1455683450000L), checker.getHost(), "APP", "USER_REGION_ERROR", "", "", -1, "", "error", "9a70a74f137e908a13e24426076b032f", DEVELOPMENT, checker.getInstanceId()
        );
    }

    @Test
    public void severityWarnFiltered() throws Exception {
        checker.setFile("/var/log/yandex/market-skubi/app.stdout.log");
        checker.check(
                "2016/02/17 07:30:50 +0300\t[worker:id=1,pid=24253]\twarn\tAPP\tUSER_REGION_ERROR\t9a70a74f137e908a13e24426076b032f\tUserError: Region initialization failed (requestId: 9a70a74f137e908a13e24426076b032f, step: \"ensureTimezone\")",
                new Date(1455683450000L), checker.getHost(), "APP", "USER_REGION_ERROR", "", "", -1, "", "warn", "9a70a74f137e908a13e24426076b032f", DEVELOPMENT, checker.getInstanceId()
        );
    }

    @Test
    public void severityInfoIgnored() throws Exception {
        checker.checkEmpty(
                "2016/02/17 07:30:50 +0300\t[worker:id=1,pid=24253]\tinfo\tAPP\tUSER_REGION_ERROR\t9a70a74f137e908a13e24426076b032f\tUserError: Region initialization failed (requestId: 9a70a74f137e908a13e24426076b032f, step: \"ensureTimezone\")"
        );
    }

    @Test
    public void parseNodeJsErrorByExperiment() throws Exception {
        String line = "2015/11/10 10:11:28 +0300\t[worker:id=10,pid=340]\terror\tRESOURCE\tVALIDATION_ERROR\thistory.add\t-\t-\t1489330900384/36ab5ed17f13aca1b641627082229197/2\tBAD_PARAM_TO_CALL\tBad param to call resource \"history.add\". Param \"historyItem.name\" can't be blank.";
        checker.setFile("/var/log/yandex/market-skubi-exp/experiment1/app.stdout.log");
        checker.check(
            line,
            new Date(1447139488000L), checker.getHost(), "RESOURCE", "VALIDATION_ERROR", "history", "add", -1, "experiment1", "error", "1489330900384/36ab5ed17f13aca1b641627082229197/2", DEVELOPMENT, checker.getInstanceId()
        );
    }

    @Test
    public void parseNodeJsErrorWithEmptyResource() throws Exception {
        String line = "2017/07/10 08:04:44 +0300\t[worker:id=7,pid=55553]\terror\tRESOURCE\tUNKNOWN_RESOURCE_ERROR\t\t-1\t-\t-\tRESOURCE_ERROR Resource failed. Unknown error <report.getModelsById> ValidationError: Index[1499663084304/012703a51a6f057d2010a9f0efc2b608]: Param \"options.hyperid\" can't be blank . Index[1499663084304/012703a51a6f057d2010a9f0efc2b608]: Param \"options.hyperid\" can't be blank.";
        checker.check(
            line,
            new Date(1499663084000L), checker.getHost(), "RESOURCE", "UNKNOWN_RESOURCE_ERROR", "", "", -1, "", "error", "", DEVELOPMENT, checker.getInstanceId()
        );
    }

}
