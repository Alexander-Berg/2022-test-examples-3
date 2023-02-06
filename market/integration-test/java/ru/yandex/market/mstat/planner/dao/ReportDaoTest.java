package ru.yandex.market.mstat.planner.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.dto.ContourRequest;
import ru.yandex.market.mstat.planner.model.Contour;
import ru.yandex.market.mstat.planner.model.ProjectWithRequest;
import ru.yandex.market.mstat.planner.service.AuthInfoService;
import ru.yandex.market.mstat.planner.service.ContourService;
import ru.yandex.market.mstat.planner.service.ProjectWithRequestService;
import ru.yandex.market.mstat.planner.task.cron.JobPeriodsCalculator;
import ru.yandex.market.mstat.planner.util.report.financialcolor.FinancialReportGenerator;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mstat.planner.util.RestUtil.todayLocalDate;

public class ReportDaoTest extends AbstractDbIntegrationTest {

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private ProjectWithRequestService projectWithRequestService;

    @Autowired
    private JobPeriodsCalculator jobPeriodsCalculator;

    @Autowired
    private ContourDao contourDao;

    @Autowired
    private ContourService contourService;

    @Test
    public void testGetContoursWithTags() {
        List<String> superv = ImmutableList.<String>builder().add(data.login).build();
        Set<Long> businessUnitIds = ImmutableSet.<Long>builder().add(data.buId).build();

        Contour c1 = contourDao.getContour(data.contourId);
        c1.getColors().put(data.colorId, (double) 1);
        c1.setBusinessUnitIds(businessUnitIds);
        ContourRequest cr1 = new ContourRequest();
        cr1.setContour(c1);
        cr1.setSupervisors(superv);
        contourService.updateContour(data.contourId, cr1);

        long color2 = data.createTestColor("AD911" ,"Яндекс.Яндекс");
        long contour2Id = data.createTestContour(data.groupId, "newTestContour");

        Contour c2 = contourDao.getContour(contour2Id);
        c2.getColors().put(color2, (double) 1);
        c2.setBusinessUnitIds(businessUnitIds);
        ContourRequest cr2 = new ContourRequest();
        cr2.setContour(c2);
        cr2.setSupervisors(superv);
        contourService.updateContour(contour2Id, cr2);

        Map<String, BigDecimal> contours1 = new HashMap<>();
        contours1.put(String.valueOf(data.contourId), BigDecimal.valueOf(0.5));
        contours1.put(String.valueOf(contour2Id), BigDecimal.valueOf(0.5));

        ProjectWithRequest relocation1 = projectWithRequestService.createRelocation(contours1, null, data.login, data.departmentId, AuthInfoService.PLANNER, null);
        jobPeriodsCalculator.execute();

        Map<String, FinancialReportGenerator.ColorReport> result =
            reportDao.financialColours(todayLocalDate(), todayLocalDate().plusDays(10), data.parentDepartmentId);

        assertTrue(result.containsKey(data.login));

        Map<Long, Double> resColors = result.get(data.login).getColors();

        assertTrue(resColors.containsKey(data.colorId));
        assertEquals(resColors.get(data.colorId), 0.5, 0.1);

        assertTrue(resColors.containsKey(color2));
        assertEquals(resColors.get(color2), 0.5, 0.1);

    }

}
