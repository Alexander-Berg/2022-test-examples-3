package ru.yandex.market.mstat.planner.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.client.HeadcountClient;
import ru.yandex.market.mstat.planner.model.ProjectWithRequest;
import ru.yandex.market.mstat.planner.service.AuthInfoService;
import ru.yandex.market.mstat.planner.service.EmployeeService;
import ru.yandex.market.mstat.planner.service.ProjectWithRequestService;
import ru.yandex.market.mstat.planner.task.cron.HeadcountsLoader;
import ru.yandex.market.mstat.planner.model.Headcount;
import ru.yandex.market.mstat.planner.service.HeadcountService;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;
import ru.yandex.market.mstat.planner.service.RequestService;
import ru.yandex.market.mstat.planner.model.Request;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.mstat.planner.util.RestUtil.today;

public class HeadcountsLoaderTest extends AbstractDbIntegrationTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private HeadcountsLoader headcountsLoader;

    @Autowired
    private HeadcountService headcountService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ProjectWithRequestService projectWithRequestService;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testHeadcountLoader() {

        headcountTestData("testdata/headcounts.json");
        headcountsLoader.execute();

        Set<Headcount> hcs = headcountService.getHeadcount(99412L);
        assertEquals(1, hcs.size());
        Headcount hc = (Headcount) hcs.toArray()[0];

        assertEquals((Long) 203364L, hc.getDepartmentId());
        assertEquals("pavlovivan", hc.getEmployee());
        assertEquals(1, (int) hc.getHeadcount());
        assertEquals(today(), hc.getStartDate());
        assertEquals(java.sql.Date.valueOf(LocalDate.now().plusYears(1000).minusDays(1).atStartOfDay().toLocalDate()),
                hc.getEndDate());
        assertFalse(hc.getIsHomeworker());
        assertFalse(hc.getIsIntern());
        assertFalse(hc.getIsMaternity());
        assertEquals("Professionals", hc.getCategory());

        List<Headcount> actualTickets = headcountService.getActualJobTicket();
        assertTrue(actualTickets.stream().map(Headcount::getHeadcountId).collect(Collectors.toList()).contains(35820L));
        assertEquals("JOB-62886", actualTickets.stream().filter(a -> a.getHeadcountId()==35820L)
                .map(Headcount::getEmployee).collect(Collectors.toList()).get(0));
        assertTrue(employeeService.employeeIsExists("JOB-62886"));

        //        For testing inherit default contours
        ProjectWithRequest reloc = projectWithRequestService.createRelocation(
                ImmutableMap.<String, BigDecimal>builder().put(String.valueOf(data.contourId), BigDecimal.ONE).build(),
                null, "JOB-85000", data.departmentId, AuthInfoService.PLANNER, null);

        headcountTestData("testdata/headcounts_updated.json");
        headcountsLoader.execute();

        hcs = headcountService.getHeadcount(100850L);
        assertEquals(1, hcs.size());
        hc = (Headcount) hcs.toArray()[0];
        assertEquals(today(), hc.getEndDate());
        assertEquals(today(), hc.getStartDate());

        hcs = headcountService.getHeadcount(35820L);
        assertEquals(2, hcs.size());
        hc = (Headcount) hcs.toArray()[0];
        assertEquals("new_employee", hc.getEmployee());
        assertTrue(employeeService.getEmployee("JOB-62886").getDismissed());

//        Testing inherit default contours 1st case: Vacancy to employee
        Request inheritedRequest = requestService.getActualNotEmptyRelocationByEmployee("mr_inherit");
        assertEquals(inheritedRequest.getProject_id().longValue(), reloc.getProject().getProject_id().longValue());
    }

    @SneakyThrows
    private void headcountTestData(String resourceFile) {
        URL resource = getClass().getClassLoader().getResource(resourceFile);
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }
        File from = new File(resource.toURI());

        JsonNode testData = mapper.readTree(from);

        HeadcountClient mockClient = headcountsLoader.getClient();
        Mockito.when(mockClient.loadHeadCounts(anyLong())).thenReturn((ArrayNode) testData.get("headcounts"));
        Mockito.when(mockClient.isSyncEnabled()).thenReturn(true);
    }


}
