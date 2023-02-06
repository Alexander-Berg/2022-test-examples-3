package ru.yandex.canvas.service.video;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.SessionParams;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class SessionParamsTest {

    @Test
    public void testCpcParams() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("shared_data")).thenReturn("{\"isCompact\":true,\"clientId\":\"103997791\","
                + "\"creativeType\":\"videoAddition\",\"cpc\":1}");

        SessionParams sessionParams = new SessionParams(request);

        assertEquals(sessionParams.getCreativeType(), VideoCreativeType.CPC);
    }

    @Test
    public void testCpmParams() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("shared_data")).thenReturn("{\"isCompact\":true,\"clientId\":\"103997791\","
                + "\"creativeType\":\"videoAddition\",\"cpm\":1}");

        SessionParams sessionParams = new SessionParams(request);

        assertEquals(sessionParams.getCreativeType(), VideoCreativeType.CPM);
    }

    @Test
    public void testMObContentParams() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("shared_data")).thenReturn("{\"isCompact\":true,\"clientId\":\"103997791\","
                + "\"creativeType\":\"videoAddition\",\"mobile_content\":1}");

        SessionParams sessionParams = new SessionParams(request);

        assertEquals(sessionParams.getCreativeType(), VideoCreativeType.MOBILE_CONTENT);
    }

    @Test
    public void testDefaultParams() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("shared_data")).thenReturn("{\"isCompact\":true,\"clientId\":\"103997791\","
                + "\"creativeType\":\"videoAddition\"}");

        SessionParams sessionParams = new SessionParams(request);

        assertEquals(sessionParams.getCreativeType(), VideoCreativeType.TEXT);
    }

    @Test
    public void testVideoFrontpageParams() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("shared_data")).thenReturn("{\"clientId\":\"34617988\"," +
                "\"creativeType\":\"videoAddition\",\"productType\":\"CPM_PRICE\",\"videoPresetIds\":[406]," +
                "\"cpm_price\":1,\"cpm_yndx_frontpage\":1}");

        SessionParams sessionParams = new SessionParams(request);

        assertEquals(SessionParams.SessionTag.CPM_YNDX_FRONTPAGE, sessionParams.getSessionType());
    }
}
