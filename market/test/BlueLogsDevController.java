package ru.yandex.market.mboc.tms.web.test;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.application.properties.utils.Environments;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mbo.jooq.repo.Sorting;
import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.mboc.app.security.SecuredRoles;
import ru.yandex.market.mboc.common.availability.msku.MskuFilter;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.OfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.users.UserRoles;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.SecurityUtil;

/**
 * Создает и новые офферы и форсит их в нужный для синих логов статус.
 * Использовать стоит только для тестирования.
 *
 * @author prediger
 */
@RestController
@SecuredRoles(UserRoles.DEVELOPER)
@RequestMapping("/api/dev/blue-logs")
public class BlueLogsDevController {
    private static final int ONE_THOUSAND = 1000;
    private final OfferRepository offerRepository;
    private final SupplierRepository supplierRepository;
    private final MskuRepository mskuRepository;
    private final OffersProcessingStatusService offersProcessingStatusService;
    private final TrackerService trackerService;
    private final OfferDestinationCalculator offerDestinationCalculator;

    public BlueLogsDevController(OfferRepository offerRepository,
                                 SupplierRepository supplierRepository,
                                 MskuRepository mskuRepository,
                                 OffersProcessingStatusService offersProcessingStatusService,
                                 TrackerService trackerService, OfferDestinationCalculator offerDestinationCalculator) {
        this.offerRepository = offerRepository;
        this.supplierRepository = supplierRepository;
        this.mskuRepository = mskuRepository;
        this.offersProcessingStatusService = offersProcessingStatusService;
        this.trackerService = trackerService;
        this.offerDestinationCalculator = offerDestinationCalculator;
    }

    @GetMapping("/create-offer")
    public Offer createTestOffer(@RequestParam("destination") Destination destination,
                                 @RequestParam("supplierId") int supplierId,
                                 @RequestParam("categoryId") long categoryId,
                                 @RequestParam(value = "mskuId", required = false) Long mskuId) {
        if (Environments.IS_PRODUCTION) {
            throw new IllegalStateException("Not allowed in production environment.");
        }
        String ssku = "RandomSSKU-" + System.currentTimeMillis() / ONE_THOUSAND;
        Supplier supplier = supplierRepository.findById(supplierId);
        Offer offer = new Offer()
            .setBusinessId(supplierId)
            .setCategoryId(offerDestinationCalculator, categoryId, Offer.BindingKind.SUGGESTED)
            .setCreatedByLogin(SecurityUtil.getCurrentUserLogin())
            .setShopSku(ssku)
            .setTitle("title of " + ssku)
            .setShopCategoryName("shop_category")
            .storeOfferContent(OfferContent.initEmptyContent())
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .addNewServiceOfferIfNotExists(offerDestinationCalculator, supplier)
            .updateAcceptanceStatus(offerDestinationCalculator, supplierId, Offer.AcceptanceStatus.OK);
        switch (destination) {
            case MODERATION:
                // getting some msku if nothing provided
                if (mskuId == null) {
                    List<Msku> mskus = mskuRepository.find(new MskuFilter().setCategoryIds(categoryId),
                        Sorting.notSorting(), new OffsetFilter().setLimit(ONE_THOUSAND));
                    mskuId = mskus
                        .get((int) (Math.random() * mskus.size()))
                        .getMarketSkuId();
                }
                offer.setSuggestSkuMapping(new Offer.Mapping(mskuId, DateTimeUtils.dateTimeNow()));
                break;
            case CLASSIFICATION:
                offer.setBindingKind(Offer.BindingKind.SUGGESTED);
                break;
            case MATCHING:
                offer.setBindingKind(Offer.BindingKind.APPROVED);
                break;
            default:
                throw new RuntimeException("Unknown offer destination:" + destination);
        }
        offerRepository.insertOffer(offer);
        // push to new status
        offersProcessingStatusService.processAndUpdateOfferById(offer.getId());
        Offer offerWithTicket = offerRepository.getOfferById(offer.getId());
        LocalDate deadline = LocalDate.now().plusDays(1);
        trackerService.setDeadline(offerWithTicket.getTrackerTicket(), deadline);
        offerRepository.updateOffer(offerWithTicket.setTicketDeadline(deadline));
        return offerRepository.getOfferById(offer.getId());
    }

    public enum Destination {
        CLASSIFICATION,
        MODERATION,
        MATCHING
    }
}
