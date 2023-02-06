package ru.yandex.market.ff.dbqueue.service;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.dbqueue.exceptions.MissingParentException;
import ru.yandex.market.ff.dbqueue.exceptions.NotRetryableException;
import ru.yandex.market.ff.dbqueue.exceptions.RequestInWrongStatusException;
import ru.yandex.market.ff.dbqueue.exceptions.RequestOfWrongTypeException;
import ru.yandex.market.ff.model.dbqueue.FinishUpdatingRequestPayload;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.RequestItemError;
import ru.yandex.market.ff.service.RequestItemService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FinishUpdatingRequestProcessingServiceTest extends IntegrationTest {

    @Autowired
    private FinishUpdatingRequestProcessingService service;

    @Autowired
    private RequestItemService requestItemService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DatabaseSetup("classpath:db-queue/service/finish-updating-request/create-identifiers-before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/finish-updating-request/create-identifiers-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void createIdentifiersTest() {
        jdbcTemplate.execute("alter sequence unit_identifier_id_seq restart with 4");
        service.processPayload(new FinishUpdatingRequestPayload(2));
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/finish-updating-request/update-existing-identifiers-before.xml")
    @ExpectedDatabase(value =
            "classpath:db-queue/service/finish-updating-request/update-existing-identifiers-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateIdentifiersTest() {
        service.processPayload(new FinishUpdatingRequestPayload(3));
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/finish-updating-request/delete-existing-identifiers-before.xml")
    @ExpectedDatabase(value =
            "classpath:db-queue/service/finish-updating-request/delete-existing-identifiers-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteIdentifiersTest() {
        service.processPayload(new FinishUpdatingRequestPayload(3));
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/finish-updating-request/invalid-type.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/finish-updating-request/invalid-type.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void invalidTypeTest() {
        int requestId = 3;
        NotRetryableException exception = assertThrows(RequestOfWrongTypeException.class,
                () -> service.processPayload(new FinishUpdatingRequestPayload(requestId)));

        assertions.assertThat(exception.getMessage()).isEqualTo(
                "Trying to finish updating request of type SUPPLY, requestId = " + requestId);
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/finish-updating-request/invalid-status.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/finish-updating-request/invalid-status.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void invalidStatusTest() {
        int requestId = 3;
        NotRetryableException exception = assertThrows(RequestInWrongStatusException.class,
                () -> service.processPayload(new FinishUpdatingRequestPayload(requestId)));

        assertions.assertThat(exception.getMessage()).isEqualTo(
                "Trying to finish updating request in status CREATED, requestId = " + requestId);
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/finish-updating-request/missing-parent.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/finish-updating-request/missing-parent.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void missingParentTest() {
        int requestId = 3;
        NotRetryableException exception = assertThrows(MissingParentException.class,
                () -> service.processPayload(new FinishUpdatingRequestPayload(requestId)));

        assertions.assertThat(exception.getMessage()).isEqualTo(
                "Trying to finish updating request with missing parent, requestId = " + requestId);
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/finish-updating-request/delete-request-items-errors-before.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/finish-updating-request/delete-request-items-errors-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteItemsErrorsTest() {
        service.processPayload(new FinishUpdatingRequestPayload(3));

        List<RequestItem> requestItems = requestItemService.findAllByRequestIdWithInternalErrorsFetched(1L);

        for (RequestItem requestItem : requestItems) {
            List<RequestItemError> actual = requestItem.getRequestItemErrorList();
            assertions.assertThat(actual).isEmpty();
        }
    }

}
