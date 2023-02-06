package ru.yandex.market.ff.service.implementation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.service.RequestAcceptanceService;


class RequestAcceptanceServiceImplTest extends IntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    RequestAcceptanceService service;

    @Test
    @DatabaseSetup("classpath:service/request-acceptance/before.xml")
    @ExpectedDatabase(value = "classpath:service/request-acceptance/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testRejectByService() {
        long requestId = 123;
        service.rejectByService(requestId, null, false);
    }

    @Test
    @DatabaseSetup("classpath:service/request-acceptance/before-valid-unredeemed.xml")
    @ExpectedDatabase(value = "classpath:service/request-acceptance/after-reject-valid-unredeemed.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testRejectByServiceForValidUnredeemedWithNotRetryableMessage() {
        long requestId = 123;
        service.rejectByService(requestId, "Error: DuplicateLogisticUnit", false);
    }

    @Test
    @DatabaseSetup("classpath:service/request-acceptance/before-valid-unredeemed.xml")
    @ExpectedDatabase(value = "classpath:service/request-acceptance/after-recreate-valid-unredeemed.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testRejectByServiceForValidUnredeemedWithMessageShouldBeRetried() {
        long requestId = 123;
        service.rejectByService(requestId, "some other error", false);
    }

    @Test
    @DatabaseSetup("classpath:service/request-acceptance/before.xml")
    @ExpectedDatabase(value = "classpath:service/request-acceptance/after-with-message.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testRejectByServiceForWithdrawWithNotRetryableMessage() {
        long requestId = 123;
        service.rejectByService(requestId, "Error: DuplicateLogisticUnit", false);
    }

    @Test
    @DatabaseSetup("classpath:service/request-acceptance-force/before.xml")
    @ExpectedDatabase(value = "classpath:service/request-acceptance-force/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testRejectByServiceForce() {
        long requestId = 123;
        service.rejectByService(requestId, null, true);
    }

    @Test
    @DatabaseSetup("/service/request-acceptance/update-child-requests/before.xml")
    @ExpectedDatabase(value = "/service/request-acceptance/update-child-requests/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void testAcceptByServiceUpdateChildRequest() {
        jdbcTemplate.execute("update request_subtype set use_parent_request_id_for_send_to_service = true" +
                " where subtype in ('CUSTOMER_RETURN_ENRICHMENT')");
        service.acceptByService(1L, "123");
    }

    @Test
    @DatabaseSetup("classpath:service/request-acceptance/update-child-requests/before-invalid-parent-status.xml")
    @ExpectedDatabase(
            value = "classpath:service/request-acceptance/update-child-requests/after-invalid-parent-status.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void testAcceptByServiceUpdateChildRequestInvalidParentStatus() {
        jdbcTemplate.execute("update request_subtype set use_parent_request_id_for_send_to_service = true" +
                " where subtype in ('CUSTOMER_RETURN_ENRICHMENT')");
        service.acceptByService(1L, "123");
    }
}
