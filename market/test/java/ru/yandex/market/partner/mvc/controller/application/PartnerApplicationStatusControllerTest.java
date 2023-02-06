package ru.yandex.market.partner.mvc.controller.application;

import java.util.List;

import com.jayway.jsonpath.JsonPath;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Проверяем изменение статуса заявки для поставщика.
 */
@ExtendWith(MockitoExtension.class)
@DbUnitDataSet(before = "data/PartnerApplicationStatusControllerTest.csv")
class PartnerApplicationStatusControllerTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private CheckouterClient checkouterClient;

    @Mock
    private CheckouterShopApi shopApi;

    @BeforeEach
    void setUp() {
        environmentService.setValue("suppliers.agencyCommission", "200");
        when(checkouterClient.shops()).thenReturn(shopApi);
    }

    /**
     * Проверяет, что нельзя перевести в статус отличный от INIT
     */
    @Test
    void changeToNotAllowedInProgressStatusTest() {
        MatcherAssert.assertThat(
                Assertions.assertThrows(HttpClientErrorException.class,
                        () -> sendStatusChangeRequest(10774L, "{\"status\": \"1\"}" /*IN_PROGRESS*/)),
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
    }

    /**
     * Проверяет, что переводит в INIT
     */
    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationStatusControllerTest.withReturnContact.before.csv",
            after = "data/PartnerApplicationStatusControllerTest.after.csv"
    )
    void changeToInitStatusTest() {
        sendStatusChangeRequest(10774L, "{\"status\": \"0\"}" /*INIT*/);
    }

    /**
     * Проверяет, что переводит в INIT
     */
    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationStatusControllerTest.whiteShop.before.csv",
            after = "data/PartnerApplicationStatusControllerTest.whiteShop.after.csv"
    )
    void changeWhiteShopPrepayToInitStatusTest() {
        sendStatusChangeRequest(10001L, "{\"status\": \"0\"}" /*INIT*/);
    }

    /**
     * Проверяет, обязатальность контактов для возврата
     */
    @Test
    void changeToInitStatusNoReturnContactTest() {
        HttpClientErrorException error = Assertions.assertThrows(HttpClientErrorException.class,
                () -> sendStatusChangeRequest(10774L, "{\"status\": \"0\"}" /*INIT*/));

        assertEquals(
                "PREPAY_REQUEST_REQUEST_INCONSISTENCY",
                JsonPath.<List<String>>read(error.getResponseBodyAsString(), ".errors[0].code").get(0)
        );
        assertEquals(
                "returnContact",
                JsonPath.<List<String>>read(error.getResponseBodyAsString(), ".errors[0].message").get(0)
        );
    }

    /**
     * Проверяет, обязатальность непустых контактов для возврата
     */
    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationStatusControllerTest.withEmptyReturnContact.before.csv")
    void changeToInitStatusEmptyReturnContactTest() {
        HttpClientErrorException error = Assertions.assertThrows(HttpClientErrorException.class,
                () -> sendStatusChangeRequest(10774L, "{\"status\": \"0\"}" /*INIT*/));
        assertEquals(
                "PREPAY_REQUEST_REQUEST_INCONSISTENCY",
                JsonPath.<List<String>>read(error.getResponseBodyAsString(), ".errors[0].code").get(0)
        );
        assertEquals(
                "firstName must not be null,lastName must not be null",
                JsonPath.<List<String>>read(error.getResponseBodyAsString(), ".errors[0].message").get(0)
        );
    }

    private void sendStatusChangeRequest(long campaignId, String requestStatus) {
        when(contactService.getClientIdByUid(100500L)).thenReturn(110777L);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(requestStatus, headers);
        FunctionalTestHelper.put(supplierApplicationEditStatusUrl(campaignId, 100500L), request);
    }

    private String supplierApplicationEditStatusUrl(long campaignId, long euid) {
        return baseUrl + "/partner/application/status?euid=" + euid + "&id=" + campaignId;
    }

}
