package ru.yandex.market.hrms.test.configurer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OebsApiConfigurer {

    @Autowired(required = false)
    private Stubbing oebsWireMockServer;

    public void mockGetHrOperations(String resource) {
        oebsWireMockServer.givenThat(WireMock.post("/rest/getScheduleFact")
                .willReturn(WireMock.okJson(readResource(resource))));
    }

    public void mockGetDepartmentSchedules(String resource) {
        oebsWireMockServer.givenThat(WireMock.post("/rest/getScheduleOrg")
                .willReturn(WireMock.okJson(readResource(resource))));
    }

    public void mockGetEmployeeSchedules(String resource) {
        oebsWireMockServer.givenThat(WireMock.post("/rest/getSchedule")
                .willReturn(WireMock.okJson(readResource(resource))));
    }

    public void mockGetDepartmentInfo(String resource, String departmentName, String date) {
        oebsWireMockServer.givenThat(WireMock.post("/rest/getDepartmentInfo")
                .withRequestBody(WireMock.containing(departmentName))
                .withRequestBody(WireMock.containing(date))
                .willReturn(WireMock.okJson(readResource(resource))));
    }

    private String readResource(String resource) {
        try {
            return IOUtils.toString(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + resource, e);
        }
    }
}
