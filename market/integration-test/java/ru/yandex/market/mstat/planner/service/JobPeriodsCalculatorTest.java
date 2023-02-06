package ru.yandex.market.mstat.planner.service;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.model.JobPeriod;
import ru.yandex.market.mstat.planner.model.ProjectWithRequest;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.task.cron.JobPeriodsCalculator;
import ru.yandex.market.mstat.planner.util.LoggingJdbcTemplate;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mstat.planner.util.RestUtil.dateToLocalDate;

public class JobPeriodsCalculatorTest extends AbstractDbIntegrationTest {

    @Autowired
    private JobPeriodsCalculator jobPeriodsCalculator;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ProjectWithRequestService projectWithRequestService;

    @Test
    public void testFillJobPeriodsTable() {

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        LocalDate planStart = today.minusDays(2);
        LocalDate planEnd = today.plusDays(4);

        long contourId2 = data.createTestContour(data.groupId, "Новый контур");
        Map<String, BigDecimal> contours1 = ImmutableMap.<String, BigDecimal>builder()
                .put(String.valueOf(contourId2), BigDecimal.valueOf(0.4))
                .put(String.valueOf(data.contourId), BigDecimal.valueOf(0.6))
                .build();
        Long projectId1 = data.createProject(contours1, null);
        Request r1 = data.createPlanTemplate(1, data.departmentId, data.login, "6d",
                java.sql.Date.valueOf(planStart), java.sql.Date.valueOf(planEnd), null, projectId1);
        Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        ProjectWithRequest pwd = projectWithRequestService.createRelocation(
                ImmutableMap.<String, BigDecimal>builder().put(String.valueOf(data.contourId), BigDecimal.ONE).build(),
                null, data.login, data.departmentId, AuthInfoService.PLANNER, null);

        LocalDate filledPeriodEnd = today.plusDays(20);
        LocalDate filledPeriodStart = today.minusDays(5);
        jobPeriodsCalculator.fillJobPeriods(filledPeriodStart, filledPeriodEnd);
        Set<JobPeriod> periods = getCurrentJobPeriods();

        assertEquals(3, periods.size());
        for (JobPeriod p : periods) {
            if (p.getReportRequestType().equals("project")) {
                assertEquals(planEnd, p.getPeriodEnd());
                assertEquals(planStart, p.getPeriodStart());
            } else {
                assertEquals(planEnd.plusDays(1), p.getPeriodStart());
                assertEquals(filledPeriodEnd, p.getPeriodEnd());
            }
        }

        int increasePlanDays = 2;
        LocalDate newPlanEnd = planEnd.plusDays(increasePlanDays);
        plan1.setDate_end(java.sql.Date.valueOf(newPlanEnd));
        requestService.updateRequest(plan1.getRequest_id(), plan1, AuthInfoService.PLANNER);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        jobPeriodsCalculator.updateJobPeriods(dateToLocalDate(plan1.getDate_start()), dateToLocalDate(plan1.getDate_end()));

        Set<JobPeriod> periods2 = getCurrentJobPeriods();
        assertEquals(3, periods2.size());
        for (JobPeriod p : periods2) {
            if (p.getReportRequestType().equals("project")) {
                assertEquals(newPlanEnd, p.getPeriodEnd());
                assertEquals(planStart, p.getPeriodStart());
            } else {
                assertEquals(newPlanEnd.plusDays(1), p.getPeriodStart());
                assertEquals(filledPeriodEnd, p.getPeriodEnd());
            }
        }

        plan1 = requestService.getRequestById(plan1.getRequest_id());
        double newJobLoad = 0.5;
        plan1.setJob_load(BigDecimal.valueOf(newJobLoad));
        requestService.updateRequest(plan1.getRequest_id(), plan1, AuthInfoService.PLANNER);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);
        jobPeriodsCalculator.updateJobPeriods(dateToLocalDate(plan1.getDate_start()), dateToLocalDate(plan1.getDate_end()));

        Set<JobPeriod> periods3 = getCurrentJobPeriods();
        assertEquals(4, periods3.size());
        for (JobPeriod p : periods3) {
            if (p.getReportRequestType().equals("project")) {
                if (p.getJobLoad().equals(BigDecimal.valueOf(newJobLoad).setScale(4, RoundingMode.HALF_UP))) {
                    assertEquals(newPlanEnd, p.getPeriodEnd());
                    assertEquals(today, p.getPeriodStart());
                } else {
                    assertEquals(newPlanEnd, p.getPeriodEnd());
                    assertEquals(planStart, p.getPeriodStart());
                }
            } else {
                assertEquals(newPlanEnd.plusDays(1), p.getPeriodStart());
                assertEquals(filledPeriodEnd, p.getPeriodEnd());
                assertEquals(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP), p.getJobLoad());
            }
        }

    }


    private Set<JobPeriod> getCurrentJobPeriods() {
        Set<JobPeriod> periods = new HashSet<>();
        jdbcTemplate.query("select *\n" +
                        "from job_periods",
                rs -> {
                    periods.add(JobPeriod.from(rs));
                });
        return periods;
    }

}
