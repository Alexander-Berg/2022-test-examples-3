package ru.yandex.market.clab.common.service.requested.good;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicateGoodObserverTest extends BasePgaasIntegrationTest {

    @Autowired
    private RequestedGoodRepository goodRepository;

    @Test
    public void testAddingGoodToMovementMarksDuplicate() {
        RequestedGood good1 = createAndSaveGood(RequestedGoodState.NEW, 1L, null);
        RequestedGood good2 = createAndSaveGood(RequestedGoodState.NEW, 1L, null);
        RequestedGood good3 = createAndSaveGood(RequestedGoodState.NEW, 2L, null);

        good1.setRequestedMovementId(1L);

        goodRepository.save(good1);

        RequestedGood savedGood1 = goodRepository.getById(good1.getId());
        RequestedGood savedGood2 = goodRepository.getById(good2.getId());
        RequestedGood savedGood3 = goodRepository.getById(good3.getId());

        assertThat(savedGood1)
            .extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.NEW);
        assertThat(savedGood2)
            .extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.DUPLICATE_MSKU);
        assertThat(savedGood3)
            .extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.NEW);
    }

    @Test
    public void testRemovingGoodFromMovementUnmarksDuplicate() {
        RequestedGood good1 = createAndSaveGood(RequestedGoodState.NEW, 1L, 1L);
        RequestedGood good2 = createAndSaveGood(RequestedGoodState.DUPLICATE_MSKU, 1L, null);

        good1.setRequestedMovementId(null);

        goodRepository.save(good1);

        RequestedGood savedGood1 = goodRepository.getById(good1.getId());
        RequestedGood savedGood2 = goodRepository.getById(good2.getId());

        assertThat(savedGood1)
            .extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.NEW);
        assertThat(savedGood2)
            .extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.NEW);
    }

    @Test
    public void testCreatingNewGoodMarkedDuplicate() {
        RequestedGood good1 = createAndSaveGood(RequestedGoodState.NEW, 1L, 1L);
        RequestedGood good2 = createGood(RequestedGoodState.DUPLICATE_MSKU, 1L, null);

        good2 = goodRepository.save(good2);

        RequestedGood savedGood2 = goodRepository.getById(good2.getId());

        assertThat(savedGood2)
            .extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.DUPLICATE_MSKU);
    }

    @Test
    public void testMskuIdChanges() {
        RequestedGood good1 = createAndSaveGood(RequestedGoodState.PROCESSING, 1L, 1L);
        RequestedGood good2 = createAndSaveGood(RequestedGoodState.DUPLICATE_MSKU, 1L, null);
        RequestedGood good3 = createAndSaveGood(RequestedGoodState.NEW, 2L, null);

        good1.setMskuId(2L);

        goodRepository.save(good1);

        RequestedGood savedGood2 = goodRepository.getById(good2.getId());
        RequestedGood savedGood3 = goodRepository.getById(good3.getId());

        assertThat(savedGood2)
            .extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.NEW);
        assertThat(savedGood3)
            .extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.DUPLICATE_MSKU);
    }

    @Test
    public void testDuplicateMskusDone() {
        RequestedGood good1 = createAndSaveGood(RequestedGoodState.PROCESSING, 1L, null);
        RequestedGood good2 = createAndSaveGood(RequestedGoodState.DUPLICATE_MSKU, 1L, null);

        good1.setState(RequestedGoodState.PROCESSED);

        goodRepository.save(good1);

        RequestedGood savedGood2 = goodRepository.getById(good2.getId());

        assertThat(savedGood2)
            .extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.DONE);
    }

    private RequestedGood createGood(RequestedGoodState state, Long mskuId, Long requestedMovementId) {
        return RandomTestUtils.randomObject(RequestedGood.class, "id", "modifiedDate")
            .setState(state)
            .setRequestedMovementId(requestedMovementId)
            .setMskuId(mskuId);
    }

    private RequestedGood createAndSaveGood(RequestedGoodState state, Long mskuId, Long requestedMovementId) {
        RequestedGood requestedGood = createGood(state, mskuId, requestedMovementId);

        return goodRepository.save(requestedGood);
    }
}
