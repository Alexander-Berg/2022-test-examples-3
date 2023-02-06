package ru.yandex.market.billing.factoring.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.bcl.BclApiService;
import ru.yandex.market.billing.bcl.CreditorStatus;
import ru.yandex.market.billing.factoring.dao.FactorRejectDao;
import ru.yandex.market.billing.factoring.model.ClientInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParametersAreNonnullByDefault
class FactorRejectionsImportServiceTestInfo extends FunctionalTest {

    @Autowired
    private FactorRejectDao factorRejectDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private MockWebServer bclApi;
    private FactorRejectionsImportService service;

    public static ClientInfo createOrganization(String name, long inn, long ogrn, CreditorStatus status) {
        return ClientInfo.builder()
                .setName(name)
                .setInn(Long.toString(inn))
                .setOgrn(Long.toString(ogrn))
                .setStatus(status)
                .build();
    }

    private static String buildSuccessfulResponse(List<ClientInfo> orgs) {
        return String.format(
                "{\"meta\": {\"built\": \"2019-12-09T11:17:12.091\"}, \"data\": %s, \"errors\": []}",
                StreamEx.of(orgs).map(FactorRejectionsImportServiceTestInfo::buildCreditorJson).joining(",", "[", "]")
        );
    }

    private static String buildErrorResponse(List<String> errors) {
        return String.format(
                "{\"meta\": {\"built\": \"2019-12-09T11:17:12.091\"}, \"data\": {}, \"errors\": %s}",
                StreamEx.of(errors).map(msg -> "{\"msg\": \"" + msg + "\"}").joining(",", "[", "]")
        );
    }

    private static String buildCreditorJson(ClientInfo org) {
        return String.format("" +
                        "{" +
                        "  \"id\": \"test-id\", " +
                        "  \"fullNameRus\": \"%s\", " +
                        "  \"inn\": %s, " +
                        "  \"ogrn\": %s, " +
                        "  \"state\": { \"status\": \"%s\", \"statusDate\": \"2021-12-31T12:12:12.111111\" }" +
                        "}",
                org.getName(), org.getInn(), org.getOgrn(), org.getStatus()
        );
    }

    @BeforeEach
    void setUp() throws IOException {
        bclApi = new MockWebServer();
        bclApi.start();
        var bclService = new BclApiService(WebClient.create("http://localhost:" + bclApi.getPort()));
        service = new FactorRejectionsImportService(factorRejectDao, transactionTemplate, bclService);
    }

    @AfterEach
    void tearDown() throws IOException {
        bclApi.close();
    }

    @Test
    void importRejectedClientsInfo() {
        var expected = createOrganization("TestOrg", 111, 1111, CreditorStatus.REJECTED);
        bclApi.enqueue(
                new MockResponse()
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(buildSuccessfulResponse(List.of(expected)))
        );
        assertThat(service.getFactorRejections(0)).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expected);
    }

    @Test
    void importRejectedClientsInfoRepeatIfError() {
        var expected = List.of(
                createOrganization("TestOrg", 111, 1111, CreditorStatus.REJECTED),
                createOrganization("TestOrg-2", 222, 2222, CreditorStatus.SENT_TO_CHECK)
        );

        bclApi.enqueue(new MockResponse().setResponseCode(404));
        bclApi.enqueue(new MockResponse().setResponseCode(500));
        bclApi.enqueue(
                new MockResponse()
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(buildSuccessfulResponse(expected))
        );

        assertThat(service.getFactorRejections(0)).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void importRejectedClientsInfoWhenSoftError() {
        var response = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(buildErrorResponse(List.of("Forbidden: Client '2029436' is not allowed")));

        bclApi.enqueue(response.clone());
        bclApi.enqueue(response.clone());
        bclApi.enqueue(response.clone());

        assertThrows(RuntimeException.class, () -> service.getFactorRejections(0));
    }

    @Test
    @DbUnitDataSet(
            before = "FactorRejectionsImportService.before.csv",
            after = "FactorRejectionsImportService.after.csv"
    )
    void updateRejectedClientsInfo() {
        service.updateFactorRejections(rejectedOrganizations(), LocalDate.parse("2021-09-28"));
    }

    @Test
    @DbUnitDataSet(
            before = "FactorRejectionsImportService.before.csv",
            after = "FactorRejectionsImportService.after.csv"
    )
    void updateRejectedClientsInfoDoubleUpdate() {
        service.updateFactorRejections(rejectedOrganizations(), LocalDate.parse("2021-09-28"));
        // если в тот же день запустим еще раз, это ничего не сломает
        service.updateFactorRejections(rejectedOrganizations(), LocalDate.parse("2021-09-28"));
    }

    private List<ClientInfo> rejectedOrganizations() {
        return List.of(
                createOrganization("ООО \"Рога и копыта\"", 111, 1111, CreditorStatus.SENT_TO_CHECK),
                createOrganization("ИП Петров", 222, 2222, CreditorStatus.FETCHED),
                createOrganization("ЗАО 'Лютик'", 333, 3333, CreditorStatus.REJECTED)
        );
    }
}
