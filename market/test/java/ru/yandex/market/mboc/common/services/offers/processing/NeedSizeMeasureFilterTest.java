package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.proto.SizeMeasureHelper;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.CategorySizeMeasureServiceStub;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.mockito.Mockito.spy;

@SuppressWarnings("checkstyle:magicNumber")
public class NeedSizeMeasureFilterTest extends BaseDbTestClass {

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    private CategorySizeMeasureServiceStub categorySizeMeasureService;
    private SizeMeasureHelper sizeMeasureHelper;
    private NeedSizeMeasureFilter needSizeMeasureFilter;

    @Before
    public void init() {
        offerRepository = spy(offerRepository);
        categorySizeMeasureService = new CategorySizeMeasureServiceStub();

        sizeMeasureHelper = new SizeMeasureHelper(categorySizeMeasureService, categorySizeMeasureService);
        needSizeMeasureFilter = new NeedSizeMeasureFilter(sizeMeasureHelper, offerRepository);

        supplierRepository.insert(
            new Supplier(2, "Test Supplier2")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real"));
        supplierRepository.insert(
            new Supplier(0, "Test Supplier1")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real"));

        offerRepository.insertOffers(Arrays.asList(
            OfferTestUtils.nextOffer()
                .setId(1L)
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
                .setVendorId(1)
                .setBusinessId(2),
            OfferTestUtils.nextOffer()
                .setId(2L)
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
                .setVendorId(2)
                .setBusinessId(2),
            OfferTestUtils.nextOffer()
                .setId(3L)
                .setCategoryIdForTests(4L, Offer.BindingKind.SUGGESTED)
                .setVendorId(1)
                .setBusinessId(0),
            OfferTestUtils.nextOffer()
                .setId(4L)
                .setCategoryIdForTests(4L, Offer.BindingKind.SUGGESTED)
                .setVendorId(2)
                .setBusinessId(0)
        ));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Test
    public void shouldChange2Offers() {
        List<Offer> offers = offerRepository.findAll();
        categorySizeMeasureService.initializeScaleInfos(
            ImmutableMap.of(
                1L, Collections.singletonList(MboSizeMeasures.ScaleInfo.newBuilder()
                    .setScaleId(1L)
                    .setVendorId(1)
                    .build())));
        List<Offer> filteredOffers = needSizeMeasureFilter.createNeedSizeMeasureTickets(offers);

        Assertions.assertThat(filteredOffers.size()).isEqualTo(3);
        Assertions.assertThat(filteredOffers).extracting(Offer::getId).contains(1L, 3L, 4L);

        ArgumentCaptor<Collection<Offer>> requestCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(offerRepository).updateOffers(requestCaptor.capture());

        Assertions.assertThat(requestCaptor.getValue()).extracting(Offer::getProcessingStatus)
            .containsExactlyInAnyOrder(Offer.ProcessingStatus.NEED_SIZE_MEASURE);
    }

    @Test
    public void shouldNotFilterOffersWithoutCategoryOrVendorIds() {
        Offer offerWithoutCategory = OfferTestUtils.nextOffer()
            .setBusinessId(0)
            .setVendorId(2);
        Offer offerWithoutVendor = OfferTestUtils.nextOffer()
            .setVendorId(null)
            .setBusinessId(0)
            .setCategoryIdForTests(4L, Offer.BindingKind.SUGGESTED);
        Offer offerWithVendorIdZero = OfferTestUtils.nextOffer()
            .setVendorId(0)
            .setBusinessId(0)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);

        offerRepository.insertOffers(Arrays.asList(offerWithoutCategory, offerWithoutVendor, offerWithVendorIdZero));
        List<Offer> offers = offerRepository.findAll();
        categorySizeMeasureService.initializeScaleInfos(
            ImmutableMap.of(
                1L, Collections.singletonList(MboSizeMeasures.ScaleInfo.newBuilder()
                    .setScaleId(1L)
                    .setVendorId(1)
                    .build())));
        List<Offer> filteredOffers = needSizeMeasureFilter.createNeedSizeMeasureTickets(offers);

        Assertions.assertThat(filteredOffers.size()).isEqualTo(6);
    }
}
