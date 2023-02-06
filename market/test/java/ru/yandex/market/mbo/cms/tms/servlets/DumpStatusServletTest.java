package ru.yandex.market.mbo.cms.tms.servlets;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.core.dashboard.DumpGroupStat;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpGroupData;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class DumpStatusServletTest {

    @Mock
    private DumpGroupStat dumpGroupStat;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private DumpStatusServlet dumpStatusServlet;

    @Before
    public void init() {
        dumpStatusServlet = new DumpStatusServlet(dumpGroupStat);
    }

    @Test
    public void testMonitoringCheckOk() throws Exception {
        test("check", Optional.empty(), "0;OK\n");
    }

    @Test
    public void testMonitoringCheckFailed() throws Exception {
        test("check", Optional.of("test fail"), "2;test fail\n");
    }

    @Test(expected = IllegalStateException.class)
    public void testMonitoringWithoutAction() throws Exception {
        test(null, null, null);
    }

    private void test(String action, Optional<String> statusResponse, String expectedAnswer) throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        Mockito.when(dumpGroupStat.checkFailedExport(DumpGroupData.Type.CMS)).thenReturn(statusResponse);
        Mockito.when(request.getParameter("action")).thenReturn(action);
        Mockito.when(response.getWriter()).thenReturn(writer);

        dumpStatusServlet.doGet(request, response);

        assertThat(stringWriter.toString(), equalTo(expectedAnswer));
    }
}
