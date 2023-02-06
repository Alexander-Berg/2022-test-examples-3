package ru.yandex.market.mboc.tms.executors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.golden.GoldenMatrixService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

@SuppressWarnings("checkstyle:magicnumber")
public class CheckOffersInGoldenMatrixExecutorTest extends BaseDbTestClass {
    @Autowired
    private OfferRepositoryImpl offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private OfferBatchProcessor offerBatchProcessor;

    private GoldenMatrixService goldenMatrixService;
    private CheckOffersInGoldenMatrixExecutor executor;

    @Before
    public void setup() {
        offerRepository = Mockito.spy(offerRepository);
        goldenMatrixService = Mockito.mock(GoldenMatrixService.class);
        executor = new CheckOffersInGoldenMatrixExecutor(goldenMatrixService, offerBatchProcessor);
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    private void setupMatrixMock(long modelId, boolean isGolden) {
        Mockito.when(goldenMatrixService.isModelInGoldenMatrix(Mockito.eq(modelId)))
            .thenReturn(isGolden);
    }

    private Offer setupOffer(Long modelId, Boolean isGolden) {
        Offer offer = OfferTestUtils.nextOffer()
            .setModelId(modelId)
            .setGolden(isGolden);
        offerRepository.insertOffer(offer);
        return offer;
    }

    private void assertGolden(Boolean... isGolden) {
        Assertions.assertThat(offerRepository.findOffers(new OffersFilter().setOrderBy(OffersFilter.Field.MODEL_ID)))
            .extracting(Offer::getGolden)
            .containsExactly(isGolden);
    }

    @Test
    public void shouldNotUpdateOfferIfNotChanged() {
        // offers and matrix
        setupOffer(1L, false);
        setupMatrixMock(1L, false);

        setupOffer(2L, true);
        setupMatrixMock(2L, true);

        setupOffer(3L, true);
        setupMatrixMock(3L, true);


        executor.execute();


        Mockito.verify(offerRepository, Mockito.times(0))
            .updateOffers(Mockito.anyCollection());
        assertGolden(false, true, true);
    }


    @Test
    public void shouldUpdateOffersIfChanged() {
        // offers
        setupOffer(1L, false);
        setupMatrixMock(1L, true);
        setupOffer(2L, false);
        setupMatrixMock(2L, true);

        setupOffer(3L, true);
        setupMatrixMock(3L, false);
        setupOffer(4L, true);
        setupMatrixMock(4L, true);


        setupOffer(5L, true);
        setupMatrixMock(5L, false);
        setupOffer(6L, true);
        setupMatrixMock(6L, true);


        executor.execute();


        assertGolden(true, true, false, true, false, true);
    }

    @Test
    public void shouldConsiderNullAsFalse() {
        // offers and matrix
        setupOffer(1L, null);
        setupMatrixMock(1L, false);


        executor.execute();

        Mockito.verify(offerRepository, Mockito.times(0))
            .updateOffers(Mockito.anyCollection());
        assertGolden((Boolean) null);
    }
}
