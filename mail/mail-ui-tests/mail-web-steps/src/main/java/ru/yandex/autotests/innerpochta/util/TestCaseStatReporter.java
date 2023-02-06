package ru.yandex.autotests.innerpochta.util;

import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.qameta.allure.util.ResultsUtils.TMS_LINK_TYPE;
import static java.lang.Math.round;

public class TestCaseStatReporter implements TestLifecycleListener {
    private final ArrayList<String> BANNED_TEST_NAMES = new ArrayList<String>() {{
        add("testCreateNewSimpleFilterForCustomFolder");
        add("testCreateNewSimpleFilterForCustomMark");
        add("shouldCreateExpiredItem");
        add("shouldCreateFutureItem");
        add("shouldCreateTodayItem");
        add("shouldNotSeeSubsPromoMore10Times");
        add("shouldOpenNotFoundPage");
    }};
    private long start;

    private ArrayList<Pattern> PROD_URLS = new ArrayList<Pattern>() {{
        add(Pattern.compile("^(https\\:\\/\\/mail\\.yandex\\.|mail\\.yandex\\.).*"));
        add(Pattern.compile("^(https\\:\\/\\/mail\\.yandex-team\\.|mail\\.yandex-team\\.).*"));
        add(Pattern.compile("^(https\\:\\/\\/calendar\\.yandex-team\\.|calendar\\.yandex-team\\.).*"));
        add(Pattern.compile("^(https\\:\\/\\/calendar\\.yandex\\.|calendar\\.yandex\\.).*"));
        add(Pattern.compile("^(https\\:\\/\\/qa\\.mail\\.yandex\\.|qa\\.mail\\.yandex\\.).*"));
        add(Pattern.compile("^(https\\:\\/\\/qa\\.mail\\.yandex-team\\.|qa\\.mail\\.yandex-team\\.).*"));
    }};

    @Override
    public void beforeTestStart(TestResult result) {
        start = round(System.currentTimeMillis() / 1000.0);
    }

    @Override
    public void afterTestWrite(TestResult result) {
        Pattern pattern = Pattern.compile("([a-zA-Z0-9]*|[a-zA-Z0-9]*\\[[^\\]]*\\])$");
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String date = dateFormat.format(LocalDateTime.now());

        Optional<Link> testpalmLink =
            result.getLinks().stream().filter(link -> link.getType().equals(TMS_LINK_TYPE)).findFirst();
        String status = result.getStatus().value();
        String testName = result.getFullName();
        Boolean isRetry = result.getLabels().stream().anyMatch(label -> label.getName().equals("intermediate"));
        Matcher matcher = pattern.matcher(testName);
        System.out.println(result.getFullName());
        if (matcher.find()) {
            testName = matcher.group(1);
        }
        if (testName.indexOf('[') > -1) {
            if (BANNED_TEST_NAMES.contains(testName.substring(0, testName.indexOf('[')))) {
                testName = testName.substring(0, testName.indexOf('['));
            }
        }
        System.out.println(testName);
        System.out.println(isRetry);
        if (!testpalmLink.isPresent()) {
            System.out.println("No testpalm link");
            return;
        }
        TestResultSaver.getInstance().addResult(makeStatString(
            date,
            testpalmLink.get().getName(),
            testName,
            status.equals(Status.PASSED.value()) ? "1" : "0",
            status.equals(Status.FAILED.value()) || status.equals(Status.BROKEN.value()) ? "1" : "0",
            status.equals(Status.SKIPPED.value()) ? "1" : "0",
            isRetry ? "1" : "0",
            String.valueOf(round(System.currentTimeMillis() / 1000.0) - start),
            isProdUrl() ? "1" : "0"
        ));
    }

    private String makeStatString(String date, String caseId, String testName, String isPassed, String isFailed,
                                  String isSkipped, String isIntermediate, String executionTime, String isProd) {
        return String.format(
            "{\"fielddate\":\"%s\",\"project\":\"%s\",\"id\":%s,\"test_name\":\"%s\",\"is_passed\":%s," +
                "\"is_failed\":%s,\"is_skipped\":%s" +
                ",\"is_intermediate\":%s%s,\"is_prod\":%s}\n",
            date,
            UrlProps.urlProps().getProject(),
            caseId,
            testName,
            isPassed,
            isFailed,
            isSkipped,
            isIntermediate,
            executionTime.equals("0") ? "" : ",\"execution_time\":" + executionTime,
            isProd
        );
    }

    private Boolean isProdUrl() {
        for (Pattern pattern : PROD_URLS) {
            Matcher matcher = pattern.matcher(UrlProps.urlProps().getBaseUri());
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }
}
