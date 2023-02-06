package ru.yandex.market.mstat.planner.task.notification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.mstat.planner.model.Department;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

public class FillInPlan3NotifyJobTest {
    @Test
    public void departmentStatsWithAllFilled() {
        Map<Department, Integer> headcount = ImmutableMap.of(
                new Department(1L, 1L, "", "department-2", false), 10,
                new Department(2L, 1L, "", "department-1", false), 20
        );

        List<FillInPlan3NotifyJob.Statistics> statistics = FillInPlan3NotifyJob.departmentStats(headcount,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(statistics, hasSize(3));

        assertNotNull(statistics.get(0));
        assertThat(statistics.get(0).getName(), is("department-1"));
        assertThat(statistics.get(0).getStat(), is("Все сотрудники подтверждены!"));

        assertNotNull(statistics.get(1));
        assertThat(statistics.get(1).getName(), is("department-2"));
        assertThat(statistics.get(1).getStat(), is("Все сотрудники подтверждены!"));
    }

    @Test
    public void departmentStatsWithSomeNonFilled() {
        Department department1 = new Department(1L, 1L, "", "department-1", false);
        Department department2 = new Department(2L, 1L, "", "department-2", false);
        Department department3 = new Department(3L, 1L, "", "department-3", false);
        department1.getChildren().add(department3);

        Map<Department, Integer> headcount = ImmutableMap.of(
                department2, 10,
                department1, 20
        );
        Map<Long, Long> nonFilledEmps = ImmutableMap.of(1L, 15L);

        List<FillInPlan3NotifyJob.Statistics> statistics = FillInPlan3NotifyJob.departmentStats(headcount,
                nonFilledEmps, Collections.emptyMap(), Collections.emptyMap());
        assertThat(statistics, hasSize(3));

        assertNotNull(statistics.get(0));
        assertThat(statistics.get(0).getName(), is("department-1"));
        assertThat(statistics.get(0).getStat(), is("15 из 20 (75 %)"));

        assertNotNull(statistics.get(1));
        assertThat(statistics.get(1).getName(), is("department-2"));
        assertThat(statistics.get(1).getStat(), is("Все сотрудники подтверждены!"));
    }

    @Test
    public void departmentStatsSkipCategoryMgmnt() {
        Map<Department, Integer> headcount = ImmutableMap.of(
                new Department(1L, 1L, "", "department-2", false), 10,
                new Department(84233L, 1L, "", "department-SERVICE_CATEGORY_MANAGEMENT_GROUP", false), 20
        );
        Map<Long, Long> nonFilledEmps = ImmutableMap.of(84233L, 15L);

        List<FillInPlan3NotifyJob.Statistics> statistics = FillInPlan3NotifyJob.departmentStats(headcount,
                nonFilledEmps, Collections.emptyMap(), Collections.emptyMap());
        assertThat(statistics, hasSize(2));

        assertNotNull(statistics.get(0));
        assertThat(statistics.get(0).getName(), is("department-2"));
        assertThat(statistics.get(0).getStat(), is("Все сотрудники подтверждены!"));
        assertThat(statistics.get(0).getW(), is("- (-)"));
        assertThat(statistics.get(0).getB(), is("- (-)"));
        assertThat(statistics.get(0).getO(), is("- (-)"));
    }

    @Test
    public void departmentStatsWithCube() {
        Map<Department, Integer> headcount = ImmutableMap.of(
                new Department(1L, 1L, "", "department-1", false), 10,
                new Department(2L, 1L, "", "department-2", false), 3
        );
        Map<Long, Long> nonFilledEmps = ImmutableMap.of(84233L, 15L);
        Map<Long, Map<String, BigDecimal>> cubeCur = ImmutableMap.of(1L,
                ImmutableMap.of("W", BigDecimal.valueOf(1), "B", BigDecimal.valueOf(0), "O",
                        BigDecimal.valueOf(5)),
                2L,
                ImmutableMap.of("W", BigDecimal.valueOf(2), "B", BigDecimal.valueOf(3), "O",
                        BigDecimal.valueOf(7)));

        Map<Long, Map<String, BigDecimal>> cubePre = ImmutableMap.of(1L,
                ImmutableMap.of("W", BigDecimal.valueOf(2), "B", BigDecimal.valueOf(1), "O",
                        BigDecimal.valueOf(3)));

        List<FillInPlan3NotifyJob.Statistics> statistics = FillInPlan3NotifyJob.departmentStats(headcount,
                nonFilledEmps, cubeCur, cubePre);
        assertThat(statistics, hasSize(3));

        assertNotNull(statistics.get(0));
        assertThat(statistics.get(0).getName(), is("department-1"));
        assertThat(statistics.get(0).getStat(), is("Все сотрудники подтверждены!"));
        assertThat(statistics.get(0).getW(), is("1.0 (2.0)"));
        assertThat(statistics.get(0).getB(), is("- (1.0)"));
        assertThat(statistics.get(0).getO(), is("5.0 (3.0)"));

        assertThat(statistics.get(1).getName(), is("department-2"));
        assertThat(statistics.get(1).getStat(), is("Все сотрудники подтверждены!"));
        assertThat(statistics.get(1).getW(), is("2.0 (-)"));
        assertThat(statistics.get(1).getB(), is("3.0 (-)"));
        assertThat(statistics.get(1).getO(), is("7.0 (-)"));

        assertThat(statistics.get(2).getName(), is("Итого"));
        assertThat(statistics.get(2).getStat(), is(""));
        assertThat(statistics.get(2).getW(), is("3.0 (2.0)"));
        assertThat(statistics.get(2).getB(), is("3.0 (1.0)"));
        assertThat(statistics.get(2).getO(), is("12.0 (3.0)"));
    }
}
