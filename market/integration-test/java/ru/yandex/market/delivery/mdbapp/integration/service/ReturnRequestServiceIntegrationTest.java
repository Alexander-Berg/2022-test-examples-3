package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnRequestState;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.ReturnRequestRepository;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.EXPIRATION_DATE;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SKU_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SUPPLIER_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PRICE_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID_STR;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.item;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.pickupPoint;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequest;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestUpdateDto;

@Sql(
    value = {
        "/data/repository/returnRequestItem/cleanup.sql",
        "/data/repository/returnRequest/cleanup.sql",
        "/data/repository/pickupPoint/cleanup.sql",
    },
    executionPhase = AFTER_TEST_METHOD
)
public class ReturnRequestServiceIntegrationTest extends MockContextualTest {

    @Autowired
    ReturnRequestService service;
    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Test
    @Transactional
    @Sql("/data/repository/returnRequest/return-request-creating-request.sql")
    public void updateReturnRequest__shouldUpdateReturnRequest() {
        // given:
        final ReturnRequest expected = returnRequest(2L)
            .setStatus(ReturnStatus.NEW)
            .setExpirationDate(EXPIRATION_DATE)
            .setState(ReturnRequestState.FINAL);
        expected.addReturnRequestItem(item(1L, PRICE_1, ITEM_SKU_1, ITEM_SUPPLIER_1));
        pickupPoint().addReturnRequest(expected);

        // when:
        service.updateReturnRequest(returnRequestUpdateDto());

        // then:
        returnRequestRepository.flush();
        final Optional<ReturnRequest> actual = returnRequestRepository.findByReturnId(RETURN_ID_STR);
        softly.assertThat(actual).isPresent();
        softly.assertThat(actual.get()).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void updateReturnRequest__shouldDoNothing_whenNoReturnRequest() {
        // when:
        service.updateReturnRequest(returnRequestUpdateDto());

        // then:
        returnRequestRepository.flush();
        final Optional<ReturnRequest> actual = returnRequestRepository.findByReturnId(RETURN_ID_STR);
        softly.assertThat(actual).isEmpty();
    }
}
