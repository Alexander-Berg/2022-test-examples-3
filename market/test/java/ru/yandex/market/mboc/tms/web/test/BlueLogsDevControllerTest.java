package ru.yandex.market.mboc.tms.web.test;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.mboc.common.assertions.custom.OfferAssertions;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.SecurityContextAuthenticationHelper;

public class BlueLogsDevControllerTest extends BaseDbTestClass {

    private static final long CATEGORY_ID = 123L;
    private static final int SUPPLIER_ID = 1;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    private OffersProcessingStatusService offersProcessingStatusService;
    private TrackerService trackerService;


    private BlueLogsDevController controller;
    private EnhancedRandom random;
    private Msku msku;

    @Before
    public void setUp() {
        random = TestUtils.createMskuRandom();
        supplierRepository.insert(new Supplier().setId(SUPPLIER_ID)
            .setName("TEST_SUPPLIER"));
        msku = mskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID));
        // mocking calls
        offersProcessingStatusService = Mockito.mock(OffersProcessingStatusService.class);
        trackerService = Mockito.mock(TrackerService.class);

        controller = new BlueLogsDevController(offerRepository, supplierRepository, mskuRepository,
            offersProcessingStatusService, trackerService, offerDestinationCalculator);
        SecurityContextAuthenticationHelper.setAuthenticationToken();
    }

    @Test
    public void shouldInsertOfferNewOffer() {
        Offer offer = controller.createTestOffer(BlueLogsDevController.Destination.MODERATION,
            SUPPLIER_ID, CATEGORY_ID, null);
        Mockito.verify(offersProcessingStatusService, Mockito.times(1))
            .processAndUpdateOfferById(Mockito.eq(offer.getId()));
        OfferAssertions.assertThat(offerRepository.getOfferById(offer.getId()))
            .hasAcceptanceStatus(Offer.AcceptanceStatus.OK)
            .hasProcessingStatus(Offer.ProcessingStatus.OPEN)
            .hasCategoryId(CATEGORY_ID)
            .hasSupplierId(SUPPLIER_ID)
            .hasSuggestedMapping(msku.getMarketSkuId());
    }
}
