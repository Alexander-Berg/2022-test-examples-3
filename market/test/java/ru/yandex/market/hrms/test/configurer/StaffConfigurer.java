package ru.yandex.market.hrms.test.configurer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class StaffConfigurer {
    private final Stubbing staffWireMockServer;

    public void mockGetPersons(String resource, long groupId, int page) {
        staffWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/v3/persons"))
                        .withQueryParam("_query", WireMock.equalTo(
                                String.format("memorial==None and groups.group.id==%1$d", groupId)))
                        .withQueryParam("_page", WireMock.equalTo(String.valueOf(page)))
                        .willReturn(WireMock.okJson(readResource(resource)))
        );
    }

    public void mockGetPersonsRecursively(String resource, long groupId, int page) {
        staffWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/v3/persons"))
                        .withQueryParam("_query",
                                WireMock.equalTo(
                                        String.format("memorial==None and " +
                                                        "(groups.group.id==%1$d or groups.group.ancestors.id==%1$d)",
                                                groupId)))
                        .withQueryParam("_page", WireMock.equalTo(String.valueOf(page)))
                        .willReturn(WireMock.okJson(readResource(resource)))
        );
    }

    public void mockPostGap(String resource, int page) {
        staffWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/gap-api/api/gaps_find"))
                        .withQueryParam("page",  WireMock.equalTo(String.valueOf(page)))
                        .willReturn(WireMock.okJson(readResource(resource)))
        );
    }

    public void mockGetGroups(String resource, long groupId, int page) {
        staffWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/v3/groups"))
                        .withQueryParam("_query", WireMock.equalTo(
                                String.format("id==%1$d or ancestors.id==%1$d", groupId)))
                        .withQueryParam("_page", WireMock.equalTo(String.valueOf(page)))
                        .willReturn(WireMock.okJson(readResource(resource)))
        );
    }

    public void mockGetPrehired(String resource) {
        staffWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/preprofile-api/export/helpdesk"))
                        .willReturn(WireMock.okJson(readResource(resource)))
        );
    }

    private String readResource(String resource) {
        try {
            return IOUtils.toString(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + resource, e);
        }
    }
}
