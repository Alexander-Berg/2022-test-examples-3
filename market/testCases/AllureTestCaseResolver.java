package ru.yandex.market.tsum.clients.aqua.testcases;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import ru.yandex.market.tsum.clients.aqua.behaviors.AllureTestCaseReport;
import ru.yandex.market.tsum.clients.aqua.startek.TestCaseForStartrek;


public class AllureTestCaseResolver {

    private static final Map<String, BiFunction<String, String, TestCaseReport>> TEST_CASE_RESOLVERS =
        Map.of("checkTMSJobs",
            (name, message) -> CheckTmsJobsTestCaseReport.newBuilder()
                .withName(name)
                .withMessage(message)
                .withJobState(CheckTmsJobMessageParser.parseJobState(message))
                .build()
        );

    private AllureTestCaseResolver() {
    }

    public static TestCaseReport resolve(AllureTestCaseReport report) {
        return TEST_CASE_RESOLVERS.getOrDefault(report.getName(),
            (name, message) -> BasicTestCaseReport.newBuilder()
                .withName(name)
                .withMessage(message)
                .build()).apply(report.getName(), report.getMessage());
    }

    public static Collection<TestCaseForStartrek> resolve(Collection<AllureTestCaseReport> reports) {
        return Optional.ofNullable(reports).orElse(List.of()).stream()
            .map(AllureTestCaseResolver::resolve)
            .map(TestCaseReport::toTestCaseForStartrek)
            .collect(Collectors.toList());
    }
}
