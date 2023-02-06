package ru.yandex.market.billing.monitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.monitor.model.MonitorJobsGroups;
import ru.yandex.market.tms.quartz2.model.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JugglerAggregateTest extends FunctionalTest {


    @Autowired
    List<Executor> executors;

    /**
     * Проверяет наличие у каждой джобы тэга критичности  crit-0, crit-1, crit-2.
     * Эти тэги обязательны для корректной работы мониторингов в juggler
     */
    @Test
    public void checkCriticalTags() {
        List<MonitorJobsGroups> tags = List.of(MonitorJobsGroups.JOBS_PRIORITY_0,
                MonitorJobsGroups.JOBS_PRIORITY_1,
                MonitorJobsGroups.JOBS_PRIORITY_2);
        for (Object executor : executors) {
            JugglerAggregate agg = executor.getClass().getAnnotation(JugglerAggregate.class);
            Set<MonitorJobsGroups> groups = new HashSet<>(Arrays.asList(agg.tags()));
            assertThat("Executor doesn't have crit tags: " + executor,
                    CollectionUtils.intersection(groups, tags), hasSize(1));
        }
    }

    /**
     * Проверяет наличие аннотации JugglerAggregate у каждого класса джобы
     */
    @Test
    public void checkJobsAnnotations() {
        for (Object executor : executors) {
            assertNotNull(executor.getClass().getAnnotation(JugglerAggregate.class),
                    executor + " doesn't have JugglerAggregate annotation");
        }
    }

    /**
     * Проверяет совпадение количества ссылок и их названий в аннотациях классов джоб
     */
    @Test
    public void checkJobsUrls() {
        for (Object executor : executors) {
            JugglerAggregate aggregate = executor.getClass().getAnnotation(JugglerAggregate.class);
            Assertions.assertEquals(aggregate.urls().length, aggregate.urlTitles().length,
                    "Urls and titles sizes in annotation for " + executor + " are not equal");
        }
    }

}
