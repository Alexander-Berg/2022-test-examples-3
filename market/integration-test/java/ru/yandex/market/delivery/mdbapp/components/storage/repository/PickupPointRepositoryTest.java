package ru.yandex.market.delivery.mdbapp.components.storage.repository;

import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.PickupPoint;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;

import static ru.yandex.market.delivery.mdbapp.components.storage.repository.ReturnRequestRepositoryTest.returnRequest;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PICKUP_POINT_EXTERNAL_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.newPickupPoint;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.pickupPoint;

@Sql(value = {"/data/repository/returnRequest/cleanup.sql", "/data/repository/pickupPoint/cleanup.sql"})
public class PickupPointRepositoryTest extends MockContextualTest {

    @Autowired
    private PickupPointRepository repository;
    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Test
    public void testSave() {
        // when:
        PickupPoint savedPickupPoint = repository.saveAndFlush(newPickupPoint());

        // then:
        PickupPoint expected = pickupPoint();
        softly.assertThat(savedPickupPoint).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testAddReturnRequest() {
        // given:
        PickupPoint pickupPoint = repository.saveAndFlush(newPickupPoint());
        ReturnRequest returnRequest = returnRequestRepository.saveAndFlush(returnRequest());

        // when:
        pickupPoint.addReturnRequest(returnRequest);
        PickupPoint actual = repository.saveAndFlush(pickupPoint);

        // then:
        PickupPoint expected = pickupPoint();
        expected.addReturnRequest(returnRequest());
        softly.assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testRemoveReturnRequest() {
        // given:
        ReturnRequest returnRequest1 =
            returnRequestRepository.saveAndFlush(returnRequest());
        String returnId2 = "156438";
        ReturnRequest returnRequest2 =
            returnRequestRepository.saveAndFlush(returnRequest(returnId2));
        PickupPoint pickupPoint = newPickupPoint();
        pickupPoint.addReturnRequest(returnRequest1);
        pickupPoint.addReturnRequest(returnRequest2);
        pickupPoint = repository.saveAndFlush(pickupPoint);

        // when:
        pickupPoint.removeReturnRequest(returnRequest1);
        PickupPoint actual = repository.saveAndFlush(pickupPoint);

        // then:
        PickupPoint expected = pickupPoint();
        expected.addReturnRequest(returnRequest(returnId2));
        softly.assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testFindByPvzMarketId() {
        // given:
        final PickupPoint expected = repository.saveAndFlush(newPickupPoint());

        // when:
        final Optional<PickupPoint> actual = repository.findByPvzMarketId(PICKUP_POINT_EXTERNAL_ID);

        // then:
        softly.assertThat(actual).isPresent();
        softly.assertThat(actual.get()).isEqualTo(expected);
    }
}
