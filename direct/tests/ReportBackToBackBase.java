package ru.yandex.autotests.direct.tests;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.web.util.rules.ExceptionWrapperRule;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Collectors.toList;
import static ru.yandex.autotests.direct.cmd.data.Logins.SUPER;
import static ru.yandex.autotests.direct.steps.XlsTestProperties.getXlsTestProperties;
import static ru.yandex.autotests.direct.steps.matchers.TableDifferMatcher.equalToTable;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

public abstract class ReportBackToBackBase {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @ClassRule
    public static final SemaphoreRule SEMAPHORE_RULE = getSemaphoreRule();
    @Rule
    public ExceptionWrapperRule exceptionWrapperRule = new ExceptionWrapperRule();

    protected List<List<String>> beta1Result;
    protected List<List<String>> beta2Result;
    private Function<File, List<List<String>>> fileParser = provideFileParser();

    protected abstract File getReport(DirectCmdSteps steps);

    protected abstract Function<File, List<List<String>>> provideFileParser();

    @Before
    public void setUp() {
        final User account = User.get(SUPER);
        List<List<List<String>>> results = of(
                getXlsTestProperties().getStage1(), getXlsTestProperties().getStage2()
        ).parallelStream()
                .map(stage -> {
                    DirectCmdSteps steps = new DirectCmdSteps(getPropertiesWithCmdStage(stage));
                    steps.authSteps().authenticate(account);
                    File reportFile = getReport(steps);
                    if (getXlsTestProperties().isDebug() && getXlsTestProperties().isLocalRun()) {
                        writeDebugInfo(stage, reportFile);
                    }
                    return fileParser.apply(reportFile);
                })
                .collect(toList());
        beta1Result = results.get(0);
        beta2Result = results.get(1);
    }

    public void test() {
        assertThat("результат соответствует ожидаемому", beta2Result, equalToTable(beta1Result));
    }

    private void writeDebugInfo(String stage, File reportFile) {
        String outputFilePath = Paths.get(getXlsTestProperties().getOutputDir(),
                System.currentTimeMillis() + "." + FilenameUtils.getExtension(reportFile.getAbsolutePath()))
                .toString();
        log.debug("Отчет с беты {}: {} ", stage, outputFilePath);
        try {
            Files.copy(reportFile.toPath(), new FileOutputStream(outputFilePath));
        } catch (IOException e) {
            log.error("Ошибка при записи в файл: ", e);
        }
    }

    private DirectTestRunProperties getPropertiesWithCmdStage(String stage) {
        DirectTestRunProperties properties = DirectTestRunProperties.newInstance();
        properties.setDirectCmdStage(stage);
        return properties;
    }

    private static SemaphoreRule getSemaphoreRule() {
        Integer permits = DirectTestRunProperties.getInstance().getDirectSemaphorePermits();
        String key = getXlsTestProperties().getBeta1() + "-" + permits;
        return new SemaphoreRule(key, permits);
    }
}