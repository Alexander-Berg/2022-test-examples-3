package ru.yandex.market.billing.monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.model.TmsExecutor;
import ru.yandex.market.billing.monitor.model.MonitorJobsGroups;
import ru.yandex.market.tms.quartz2.model.Executor;

import static java.util.Arrays.asList;
import static java.util.Comparator.naturalOrder;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.market.mbi.util.Functional.mapToList;

@Disabled
public class PrintJobInfoTest extends FunctionalTest {

    private static final int NAME_WIDTH = 100;
//    private static final int TAGS_WIDTH = 100;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void printJobTagsTest() {
        Map<String, TmsExecutor> executors = applicationContext.getBeansOfType(TmsExecutor.class);
        System.out.println("Total jobs: " + executors.size());
        executors.forEach((name, executor) -> {
            System.out.println(" " +
                    appendTabs(name, NAME_WIDTH) +
                    "| " +
                    StringUtils.join(extractJobTags(executor)));
        });
    }

    @Test
    public void printTagsToJobsTest() {
        Map<String, TmsExecutor> executors = applicationContext.getBeansOfType(TmsExecutor.class);
        Map<String, List<String>> jobToTags = EntryStream.of(executors)
                .mapValues(this::extractJobTags)
                .toMap();

        Map<String, List<String>> tagToJobs = EntryStream.of(jobToTags)
                .flatMapValues(Collection::stream)
                .invert()
                .grouping();

        List<String> sortedTags = new ArrayList<>(tagToJobs.keySet());
        sortedTags.sort(naturalOrder());

        sortedTags.forEach(tag -> {
            List<String> jobs = tagToJobs.get(tag);
            System.out.println(" Tag: " + tag);
            System.out.println(" Jobs:");
            jobs.forEach(job -> {
                System.out.println(" - " + makeJugglerLink(job));
            });
            System.out.println("\n-------------\n\n");
        });
    }

    @Test
    public void printJobsWithoutTags() {
        Map<String, TmsExecutor> executors = applicationContext.getBeansOfType(TmsExecutor.class);
        List<String> jobsWithAbsentTags = EntryStream.of(executors)
                .mapValues(this::extractJobTags)
                .filterValues(Collection::isEmpty)
                .keys()
                .toList();

        jobsWithAbsentTags.forEach(System.out::println);
    }

    @Test
    public void printJobsWithoutPriorityTags() {
        Map<String, TmsExecutor> executors = applicationContext.getBeansOfType(TmsExecutor.class);
        List<String> jobsWithAbsentPriorityTags = EntryStream.of(executors)
                .mapValues(this::extractJobTags)
                .filterValues(tags -> tags.contains("jobs-priority-1") && !tags.contains("duty-priority-high"))
                .keys()
                .toList();

        jobsWithAbsentPriorityTags.forEach(System.out::println);
    }

    private List<String> extractJobTags(Executor executor) {
        JugglerAggregate jugglerAggregate = findAnnotation(executor.getClass(), JugglerAggregate.class);
        List<MonitorJobsGroups> jobGroups = asList(nvl(jugglerAggregate.tags(), new MonitorJobsGroups[0]));
        return mapToList(jobGroups, MonitorJobsGroups::getTag);
    }

    private String appendTabs(String str, int width) {
        int tabCount = (width - str.length()) / 4;
        return rightPad(str, tabCount, "\t");
    }

    private String makeJugglerLink(String job) {
        return String.format("((" + "https://juggler.yandex-team.ru/project/market-billing/aggregate?" +
                        "service=%s&host=market-billing&project=market-billing&from=1655326800000&to=1656536399999 " +
                        "%s))",
                job, job);
    }
}
