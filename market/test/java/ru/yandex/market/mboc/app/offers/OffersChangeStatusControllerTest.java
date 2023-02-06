package ru.yandex.market.mboc.app.offers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mboc.app.offers.models.OffersWebFilter;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.web.Result;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 17.06.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OffersChangeStatusControllerTest extends BaseMbocAppTest {

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;

    @Autowired
    private SupplierRepository supplierRepository;

    private OffersChangeStatusController controller;

    @Before
    public void setup() {
        BusinessSupplierService businessSupplierService =
            new BusinessSupplierService(supplierRepository, offerRepository);
        OffersWebService offersWebService = new OffersWebService(offerRepository, null, null,
            businessSupplierService, TransactionHelper.MOCK);

        supplierRepository.insertBatch(
            new Supplier(42, "Supplier 42"),
            new Supplier(43, "Supplier 43"),
            new Supplier(60, "Supplier 60")
        );
        offerRepository.insertOffers(YamlTestUtil.readOffersFromResources("app-offers/change-status.yml"));

        var supplierService = new SupplierService(supplierRepository);
        var categoryCachingServiceMock = new CategoryCachingServiceMock();
        var needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));

        OffersProcessingStatusService offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor,
            needContentStatusService,
            supplierService,
            null,
            null,
            null,
            null,
            null,
            offerDestinationCalculator,
            storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService),
            false, false, 3, categoryInfoCache);

        controller = new OffersChangeStatusController(offersWebService, offersProcessingStatusService);
    }

    @Test
    public void testConditions() {
        Assertions.assertThatThrownBy(() -> {
            controller.changeStatus(new OffersChangeStatusController.ChangeStatusRequest()
                .setNewStatus(Offer.ProcessingStatus.NEED_INFO)
                .setFilter(new OffersWebFilter())
                .setOfferIds(Arrays.asList(1L, 2L)));
        }).hasMessageContaining("Only one of filter or offerIds must be set");

        Assertions.assertThatThrownBy(() -> {
            controller.changeStatus(new OffersChangeStatusController.ChangeStatusRequest()
                .setNewStatus(Offer.ProcessingStatus.NEED_INFO));
        }).hasMessageContaining("Either filter or offerIds must be set");
    }

    @Test
    public void testByFilterOk() {
        Result result = controller.changeStatus(new OffersChangeStatusController.ChangeStatusRequest()
            .setNewStatus(Offer.ProcessingStatus.NEED_INFO)
            .setSupplierId(60)
            .setFilter(new OffersWebFilter().setSupplierId(60)));

        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        assertThat(result.getDetailsTitle()).isNull();
    }

    @Test
    public void testByFilterError() {
        Result result = controller.changeStatus(new OffersChangeStatusController.ChangeStatusRequest()
            .setNewStatus(Offer.ProcessingStatus.NEED_INFO)
            .setSupplierId(42)
            .setFilter(new OffersWebFilter().setProcessingStatus(Offer.ProcessingStatus.OPEN)));

        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        assertThat(result.getDetailsTitle()).containsIgnoringCase("1 оффер");
    }

    @Test
    public void testByFilterPartial() {
        Result result = controller.changeStatus(new OffersChangeStatusController.ChangeStatusRequest()
            .setNewStatus(Offer.ProcessingStatus.NEED_INFO)
            .setSupplierId(42)
            .setFilter(new OffersWebFilter().setSupplierId(42)));

        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        // OPEN/REOPEN нельзя переставить
        assertThat(result.getMessage()).containsIgnoringCase("1 оффер");
        assertThat(result.getDetailsTitle()).containsIgnoringCase("2 оффер");
    }

    @Test
    public void testByIdsOk() {
        Result result = controller.changeStatus(new OffersChangeStatusController.ChangeStatusRequest()
            .setNewStatus(Offer.ProcessingStatus.NEED_INFO)
            .setSupplierId(60)
            .setOfferIds(Collections.singletonList(4L)));

        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        assertThat(result.getDetailsTitle()).isNull();
    }

    @Test
    public void testByIdsError() {
        Result result = controller.changeStatus(new OffersChangeStatusController.ChangeStatusRequest()
            .setNewStatus(Offer.ProcessingStatus.NEED_INFO)
            .setSupplierId(42)
            .setOfferIds(Collections.singletonList(1L)));

        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        assertThat(result.getDetailsTitle()).containsIgnoringCase("1 оффер");
    }

    @Test
    public void testByIdsPartial() {
        Result result = controller.changeStatus(new OffersChangeStatusController.ChangeStatusRequest()
            .setNewStatus(Offer.ProcessingStatus.NEED_INFO)
            .setSupplierId(42)
            .setOfferIds(Arrays.asList(1L, 2L, 3L)));

        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        // OPEN/REOPEN нельзя переставить
        assertThat(result.getMessage()).containsIgnoringCase("1 оффер");
        assertThat(result.getDetailsTitle()).containsIgnoringCase("2 оффер");
    }

    @Test
    public void testReopenNeedInfo() {
        Offer offer = offerRepository.getOffersByIds(5L).get(0);
        offer.setContentComments(new ContentComment(ContentCommentType.NEED_VENDOR));
        offerRepository.updateOffer(offer);

        Result result = controller.changeStatus(new OffersChangeStatusController.ChangeStatusRequest()
            .setNewStatus(Offer.ProcessingStatus.REOPEN)
            .setSupplierId(43)
            .setOfferIds(List.of(5L)));

        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        assertThat(result.getMessage()).containsIgnoringCase("1 оффер");
        assertThat(result.getDetailsTitle()).isNull();

        offer = offerRepository.getOffersByIds(5L).get(0);
        MbocAssertions.assertThat(offer)
            .hasProcessingStatus(Offer.ProcessingStatus.REOPEN)
            .hasNoContentComments();
    }
}
