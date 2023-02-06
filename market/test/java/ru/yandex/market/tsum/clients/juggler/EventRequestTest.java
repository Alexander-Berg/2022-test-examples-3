package ru.yandex.market.tsum.clients.juggler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.asynchttpclient.Param;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 02/10/2017
 */
public class EventRequestTest {
    @Test
    public void formatSearchExpression() throws Exception {
        Assert.assertEquals(
            "(host=hostA)&",
            EventRequest.formatSearchExpression("host", Collections.singleton("hostA"))
        );
        Assert.assertEquals(
            "(host=hostA|host=hostB)&",
            EventRequest.formatSearchExpression("host", Arrays.asList("hostA", "hostB"))
        );
    }

    @Test
    public void apiRequestBuilder() throws Exception {
        List<String> expectedParams = Arrays.asList("host_name", "service_name", "tag_name", "status", "do", "format",
            "include_description");
        EventRequest eventRequest = EventRequest.newBuilder()
            .addHost("test_host")
            .addService("test_serviceA")
            .addService("test_serviceB")
            .addTag("test_tag")
            .addStatus(EventStatus.CRIT)
            .build();
        List<Param> actualParams = eventRequest.apiRequestBuilder().build().getQueryParams();

        Assert.assertTrue(actualParams.stream().allMatch(param -> expectedParams.contains(param.getName()))
        );

        Assert.assertTrue(actualParams.contains(new Param("host_name", "test_host")));
        Assert.assertTrue(actualParams.contains(new Param("tag_name", "test_tag")));
        Assert.assertTrue(actualParams.contains(new Param("status", EventStatus.CRIT.toString())));
        Assert.assertEquals(
            2,
            actualParams.stream().filter(param -> ("service_name".equals(param.getName()))).count()
        );
    }

    @Test
    public void formatAPIParamName() throws Exception {
        Assert.assertEquals("host_name", EventRequest.formatAPIParamName("host"));
        Assert.assertEquals("service_name", EventRequest.formatAPIParamName("service"));
        Assert.assertEquals("tag_name", EventRequest.formatAPIParamName("tag"));
        Assert.assertEquals("status", EventRequest.formatAPIParamName("status"));
    }

    @Test
    public void humanFriendlyRequestBuilder() throws Exception {
        EventRequest eventRequest = EventRequest.newBuilder()
            .addHost("test_host")
            .addTag("test_tag")
            .addService("test_serviceB")
            .addService("test_serviceA")
            .build();
        Param param = new Param(
            "query",
            "%28host%3Dtest_host%29%26%28tag%3Dtest_tag%29%26"
                + "%28service%3Dtest_serviceB%7Cservice%3Dtest_serviceA%29%26"
        );
        Assert.assertEquals(param, eventRequest.humanFriendlyRequestBuilder().build().getQueryParams().get(0));
    }
}
