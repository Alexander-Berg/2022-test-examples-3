package ru.yandex.market.partner.mvc.controller.business.access;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "BusinessPartnerAccessFunctionalTest.before.csv")
public class BusinessPartnerAccessControllerTest extends FunctionalTest {
    private static final String URL = "/businesses/{businessId}/partners/access";
    private static final Long BUSINESS_ID = 100L;
    private static final long CLIENT_ID = 333333L;

    @Autowired
    private BalanceService balanceService;

    public static Stream<Arguments> testAccessArguments() {
        return Stream.of(
                Arguments.of(1001, null, "json/allByBusiness.json"),
                Arguments.of(1001, 134820L, "json/shopAndSupplier.json"),
                Arguments.of(1001, 999L, "json/empty.json"),
                Arguments.of(1002, null, "json/shopAndSupplier.json")
        );
    }

    @ParameterizedTest
    @MethodSource("testAccessArguments")
    public void testAccess(int userId, Long appRequestId, String expectedResponsePath) {
        when(balanceService.getClientByUid(4001))
                .thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO, false, 0));
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + URL + "?_user_id=" + userId +
                        (appRequestId == null ? "" : "&application_request_id=" + appRequestId),
                BUSINESS_ID);
        JsonTestUtil.assertEquals(response, getClass(), expectedResponsePath);
    }
}
