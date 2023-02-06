package ru.yandex.market.partner.mvc.controller.placementstatus;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataSet(before = "placementStatusControllerTest.before.csv")
public class PlacementStatusControllerTest extends FunctionalTest {

    @Autowired
    private BalanceContactService balanceContactService;

    @BeforeEach
    void init() {
        Mockito.when(balanceContactService.getClientIdByUid(Mockito.eq(12300L))).thenReturn(11L);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void getBusinessStatus(String name, String url, String expected) {
        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(url));
        JsonTestUtil.assertEquals(response, expected);
    }

    static Stream<Arguments> getBusinessStatus() {
        return Stream.of(
                Arguments.of(
                        "Нет контакта",
                        "/placementStatus?euid=12345",
                        "{\"link\":\"https://partner.market.fslb.yandex.ru/welcome/partners\",\"partnerType\":\"NOT_CLIENT\"}"
                ),
                Arguments.of(
                        "У контакта нет достпуа к бизнесу и магазинам",
                        "/placementStatus?euid=12375",
                        "{\"link\":\"https://partner.market.fslb.yandex.ru/welcome/partners\",\"partnerType\":\"NOT_CLIENT\"}"
                ),
                Arguments.of(
                        "У контакта есть доступ к бизнесу с партнером незавершившим подключение",
                        "/placementStatus?euid=12341",
                        "{\"link\":\"https://partner.market.fslb.yandex.ru/business/333/dashboard\",\"partnerType\":\"BUSINESS\",\"partnerStatus\":\"TESTED\"}"
                ),
                Arguments.of(
                        "У контакта есть доступ к партнеру, незавершившего подключение",
                        "/placementStatus?euid=12342",
                        "{\"link\":\"https://partner.market.fslb.yandex.ru/shop/12003/onboarding\",\"partnerType\":\"PARTNER\",\"partnerStatus\":\"CONFIGURE\"}"
                ),
                Arguments.of(
                        "У контакта есть доступ к нескольким бизнесам с успешным подключенным магазином",
                        "/placementStatus?euid=12346",
                        "{\"link\":\"https://partner.market.fslb.yandex.ru/businesses\",\"partnerStatus\":\"SUCCESS\",\"partnerType\":\"BUSINESS\"}"
                ),
                Arguments.of(
                        "У контакта есть доступ к нескольким парнтерам с отключенными магазинами (которые были раньше подключены)",
                        "/placementStatus?euid=12347",
                        "{\"link\":\"https://partner.market.fslb.yandex.ru/shop/13004/summary\",\"partnerStatus\":\"SUCCESS\",\"partnerType\":\"PARTNER\"}"
                ),
                Arguments.of(
                        "У контакта есть доступ к нескольким бизнесам с отключенными магазинами (которые были раньше подключены)",
                        "/placementStatus?euid=12348",
                        "{\"link\":\"https://partner.market.fslb.yandex.ru/business/608/dashboard\",\"partnerStatus\":\"FAIL\",\"partnerType\":\"BUSINESS\"}"
                ),
                Arguments.of(
                        "У контакта есть доступ к нескольким партнерам с работающим магазином",
                        "/placementStatus?euid=12349",
                        "{\"link\":\"https://partner.market.fslb.yandex.ru/supplier/13003/summary\",\"partnerStatus\":\"SUCCESS\",\"partnerType\":\"PARTNER\"}"
                ),
                Arguments.of(
                        "Контакт - представитель агенства",
                        "/placementStatus?euid=12300",
                        "{\"link\":\"https://partner.market.fslb.yandex.ru/agency\",\"partnerType\":\"AGENCY\"}"
                )
        );
    }

    private String getUrl(String method) {
        return baseUrl + method;
    }

}
