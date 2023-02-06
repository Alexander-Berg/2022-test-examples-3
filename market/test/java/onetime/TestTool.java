package onetime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.mbo.export.MboParameters;

import javax.annotation.Resource;
import java.util.List;

/**
 * Should be used for various data queries, required to investigate issue.
 * So not commit changes to this file. If you need reusable tool - create separate tool test class.
 *
 * How to use the tool in IDE on local machine:
 *
 * 1) Write a one-time query you need in the test
 *
 * 2) Select platform test runner instead of Gradle
 * (For IntelliJ IDEA: Preferences -> Build, Execution, Deployment -> Build Tools ->
 * Gradle -> Runner -> Run tests using: Platform Test Runner)
 *
 * 3) Add the following line to /private/etc/hosts:
 * 127.0.0.1           pgaas.mail.yandex.net
 *
 * 4) Configure ssh tunnel to pgaas:
 * ssh -f -N -L 12000:pgaas.mail.yandex.net:12000 public01h.market.yandex.net
 *
 * 5) Run the test in IDE
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:tool-development.xml"})
public class TestTool extends ToolBase {

    private static final Logger log = LogManager.getLogger();

    @Resource
    ParamUtils paramUtils;

    @Test
    @Ignore("Don't need to run test tool with unit tests")
    public void test() {
        List<MboParameters.Parameter> params = paramUtils.getParams(4547637);
        log.info("Got {} params", params.size());
    }
}
