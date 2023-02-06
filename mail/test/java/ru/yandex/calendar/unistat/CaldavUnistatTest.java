package ru.yandex.calendar.unistat;

import java.util.Arrays;

import lombok.SneakyThrows;
import lombok.val;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.apache.jackrabbit.webdav.WebdavResponseImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.calendar.frontend.caldav.impl.CaldavJackrabbitServlet;
import ru.yandex.calendar.frontend.caldav.impl.CaldavService;
import ru.yandex.calendar.frontend.caldav.proto.CaldavLocatorFactory;
import ru.yandex.calendar.frontend.caldav.proto.caldav.CaldavConstants;
import ru.yandex.calendar.frontend.caldav.proto.facade.AbstractCaldavTest;
import ru.yandex.calendar.frontend.caldav.proto.webdav.DavHref;
import ru.yandex.calendar.frontend.caldav.proto.webdav.WebdavConstants;
import ru.yandex.calendar.frontend.caldav.proto.webdav.xml.MultiStatus2;
import ru.yandex.calendar.frontend.caldav.proto.webdav.xml.MultiStatusResponse2;
import ru.yandex.calendar.frontend.caldav.proto.webdav.xml.MultiStatusUtils;
import ru.yandex.calendar.frontend.caldav.proto.webdav.xml.Prop;
import ru.yandex.calendar.frontend.caldav.proto.webdav.xml.PropStat;
import ru.yandex.calendar.frontend.caldav.proto.webdav.xml.Status;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

public class CaldavUnistatTest extends AbstractCaldavTest {
    @Autowired
    private CaldavJackrabbitServlet servlet;
    @Autowired
    private CaldavService caldavService;

    @Test
    @SneakyThrows
    public void serviceMetricsSimpleTest() {
        val req = new MockHttpServletRequest("GET", "/calendars/alexandr21@yandex-team.ru");
        val resp = new MockHttpServletResponse();

        servlet.service(req, resp);
        checkCounterValue("application.request.caldav.get.calendars.200", 1.);
        checkTimer("application.request.time.caldav.get.calendars");
    }

    @Test
    @SneakyThrows
    public void serviceMetricsExceptionTest() {
        val req = new MockHttpServletRequest("PUT", "/calendars/alexandr21@yandex-team.ru");
        val resp = new MockHttpServletResponse();

        Mockito.doThrow(RuntimeException.class).when(caldavService).execute(req, resp);
        servlet.service(req, resp);
        checkCounterValue("application.request.caldav.put.calendars.500", 1.);
        checkTimer("application.request.time.caldav.put.calendars");
    }

    @Test
    public void sendMultiStatusUtilsTest() {
        val req = new WebdavRequestImpl(new MockHttpServletRequest("PUT", "/report/alexandr21@yandex-team.ru"),
                Mockito.mock(CaldavLocatorFactory.class));
        val resp = new WebdavResponseImpl(new MockHttpServletResponse());

        val hrefCaldavResponse = MultiStatusResponse2.hrefResponse(DavHref.fromEncoded("111"), 404);
        val propStat1 = new PropStat(Status.STATUS_200_OK, new Prop(WebdavConstants.DAV_DISPLAYNAME_PROP));
        val propStat2 = new PropStat(Status.STATUS_404_NOT_FOUND, new Prop(CaldavConstants.CALENDARSERVER_FIRST_NAME_PROP));

        val propStatReponse = MultiStatusResponse2.propStatResponse(DavHref.fromEncoded("11111"), Arrays.asList(propStat1, propStat2));
        val multiStatus = new MultiStatus2(Arrays.asList(hrefCaldavResponse, propStatReponse));

        MultiStatusUtils.sendMultiStatus(multiStatus, req, resp, registry);

        checkCounterValue("application.request.caldav.put.report.200", 1.);
        checkCounterValue("application.request.caldav.put.report.404", 2.);
    }

    @Test
    public void sendMultiStatusUtilsTest2() {
        val req = new WebdavRequestImpl(new MockHttpServletRequest("PUT", "/report/alexandr21@yandex-team.ru"),
                Mockito.mock(CaldavLocatorFactory.class));
        val resp = new WebdavResponseImpl(new MockHttpServletResponse());

        val multiStatus = new MultiStatus();
        multiStatus.addResponse(new MultiStatusResponse("href1", 200));
        multiStatus.addResponse(new MultiStatusResponse("href2", 200));
        multiStatus.addResponse(new MultiStatusResponse("href3", 500));

        MultiStatusUtils.sendMultiStatus(multiStatus, req, resp, registry);

        checkCounterValue("application.request.caldav.put.report.200", 2.);
        checkCounterValue("application.request.caldav.put.report.500", 1.);
    }
}
