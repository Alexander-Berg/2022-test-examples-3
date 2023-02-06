package ru.yandex.market.delivery.mdbapp.components.storage.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequestItem;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnClientType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnRequestState;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnStatus;

@Sql(value = {"/data/repository/returnRequestItem/cleanup.sql", "/data/repository/returnRequest/cleanup.sql"})
public class ReturnRequestRepositoryTest extends MockContextualTest {

    @Autowired
    private ReturnRequestRepository repository;
    @Autowired
    private ReturnRequestItemRepository returnRequestItemRepository;

    @Test
    public void testSave() {
        // when:
        ReturnRequest actual = repository.saveAndFlush(returnRequest());

        // then:
        ReturnRequest expected = returnRequest().setId(1L);
        softly.assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testRemoveReturnRequestItem() {
        // given:
        ReturnRequest returnRequest = repository.saveAndFlush(returnRequest());
        ReturnRequestItem returnRequestItem1 =
            returnRequestItemRepository.saveAndFlush(ReturnRequestItemRepositoryTest.returnRequestItem(returnRequest));
        returnRequestItemRepository.saveAndFlush(ReturnRequestItemRepositoryTest.returnRequestItem(returnRequest));
        returnRequest = repository.saveAndFlush(returnRequest);

        // when:
        returnRequest.removeReturnRequestItem(returnRequestItem1);
        ReturnRequest actual = repository.saveAndFlush(returnRequest);

        // then:
        ReturnRequest expected = returnRequest().setId(1L);
        expected.addReturnRequestItem(ReturnRequestItemRepositoryTest.returnRequestItem(expected).setId(2L));
        softly.assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testFindByReturnId() {
        // given:
        String returnId = "9586";
        ReturnRequest expected = repository.saveAndFlush(returnRequest(returnId));

        // when:
        var actual = repository.findByReturnId(returnId);

        // then:
        softly.assertThat(actual).isPresent();
        softly.assertThat(actual.get()).isEqualTo(expected);
    }

    @Test
    public void testFindAllByState() {
        // given:
        repository.saveAndFlush(returnRequest("9586"));
        final ReturnRequest expected = repository.saveAndFlush(
            returnRequest("5387").setState(ReturnRequestState.CREATING_REQUESTS)
        );

        // when:
        final List<ReturnRequest> actual = repository.findAllByState(ReturnRequestState.CREATING_REQUESTS);

        // then:
        softly.assertThat(actual).containsExactlyInAnyOrder(expected);
    }

    static ReturnRequest returnRequest() {
        String returnId = "156437";
        return returnRequest(returnId);
    }

    static ReturnRequest returnRequest(final String returnId) {
        LocalDate requestDate = LocalDate.of(2021, 2, 13);
        return new ReturnRequest()
            .setReturnId(returnId)
            .setBarcode("VOZVRAT_SF_PVZ_" + returnId)
            .setExternalOrderId(223225L)
            .setBuyerName("Петр Иванов")
            .setClientType(ReturnClientType.CLIENT)
            .setRequestDate(requestDate)
            .setStatus(ReturnStatus.NEW)
            .setExpirationDate(requestDate.plusDays(3))
            .setArrivedAt(LocalDateTime.of(requestDate, LocalTime.of(16, 35, 56)))
            .setState(ReturnRequestState.AWAITING_FOR_DATA);
    }
}
