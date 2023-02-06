package ru.yandex.market.mboc.common.services.monitorings.offers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.monitorings.offers.OfferMonitoringStrategy.Resolution;
import ru.yandex.market.mboc.common.services.offers.upload.YtOfferUploadQueueService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.services.monitorings.offers.OutdatedYtOffersMonitoringStrategy.OUTDATED_MINUTES;
import static ru.yandex.market.mboc.common.services.monitorings.offers.OutdatedYtOffersMonitoringStrategy.TIME_UNIT;

@SuppressWarnings("checkstyle:MagicNumber")
public class OutdatedYtOffersMonitoringStrategyTest extends BaseDbTestClass {

    @Resource
    private SupplierRepository supplierRepository;
    @Resource
    private OfferRepository offerRepository;
    @Resource
    private YtOfferUploadQueueService ytOfferUploadQueueService;
    private OutdatedYtOffersMonitoringStrategy strategy;

    @Before
    public void setUp() {
        supplierRepository.insert(new Supplier().setId(1).setName("supplier"));
        strategy = new OutdatedYtOffersMonitoringStrategy(null, offerRepository, ytOfferUploadQueueService);
    }

    @Test
    public void testAllOffersAreUpToDate() {
        initUpToDateOffers();
        Resolution result = strategy.monitorOffers();
        assertThat(result.getOverallStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testSomeOffersAreOutOfDate() {
        initUpToDateOffers();
        List<Offer> outdatedOffers = initOutOfDateOffers();
        Resolution result = strategy.monitorOffers();

        Assertions.assertThat(result.getOverallStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        List<ShopSkuKey> outdatedSamples = outdatedOffers.stream()
                .map(Offer::getShopSkuKey).collect(Collectors.toList());
        assertThat(result.getSamples()).containsExactlyInAnyOrderElementsOf(outdatedSamples);
        assertThat(strategy.getCritMessage()).contains("2 offers are not uploaded to YT in 15 minutes.");
    }

    @Test
    public void testOutOfDateOffersWithoutMappingsWontAffectStatus() {
        initUpToDateOffers();
        initOutOfDateOffers(o -> o.setApprovedSkuMappingInternal(null));
        Resolution result = strategy.monitorOffers();
        assertThat(result.getOverallStatus()).isEqualTo(MonitoringStatus.OK);
    }

    private List<Offer> initUpToDateOffers() {
        LocalDateTime now = DateTimeUtils.dateTimeNow();
        Offer first = new Offer()
                .setShopCategoryName("category")
                .setBusinessId(1).setShopSku("ssku-1").storeOfferContent(OfferContent.initEmptyContent())
                .setTitle("sku")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(new Offer.Mapping(1, LocalDateTime.parse("2018-01-01T01:01")))
                .approve(Offer.MappingType.APPROVED, Offer.MappingConfidence.CONTENT);
        Offer second = new Offer()
                .setShopCategoryName("category")
                .setBusinessId(1).setShopSku("ssku-2").storeOfferContent(OfferContent.initEmptyContent())
                .setTitle("sku")
                .setApprovedSkuMappingInternal(new Offer.Mapping(1, now.minusMinutes(2)))
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .approve(Offer.MappingType.APPROVED, Offer.MappingConfidence.CONTENT);

        List<Offer> offers = List.of(first, second);
        offerRepository.insertOffers(offers);
        ytOfferUploadQueueService.enqueueNow(offers);
        return offers;
    }

    private List<Offer> initOutOfDateOffers() {
        return initOutOfDateOffers(o -> {
        });
    }

    private List<Offer> initOutOfDateOffers(Consumer<Offer> apply) {
        LocalDateTime now = DateTimeUtils.dateTimeNow();
        Offer first = new Offer()
                .setShopCategoryName("category")
                .setBusinessId(1).setShopSku("ssku-3").storeOfferContent(OfferContent.initEmptyContent())
                .setTitle("sku")
                .setApprovedSkuMappingInternal(new Offer.Mapping(1, LocalDateTime.parse("2018-01-01T01:01")))
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .approve(Offer.MappingType.APPROVED, Offer.MappingConfidence.CONTENT);
        apply.accept(first);
        Offer second = new Offer()
                .setShopCategoryName("category")
                .setBusinessId(1).setShopSku("ssku-4").storeOfferContent(OfferContent.initEmptyContent())
                .setTitle("sku")
                .setApprovedSkuMappingInternal(new Offer.Mapping(1, now.minusMinutes(2)))
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .approve(Offer.MappingType.APPROVED,
                        Offer.MappingConfidence.CONTENT);
        apply.accept(second);
        List<Offer> offers = List.of(first, second);
        offerRepository.insertOffers(offers);
        ytOfferUploadQueueService.dequeueOffers(offers);
        ytOfferUploadQueueService.enqueue(List.of(first), LocalDateTime.parse("2018-01-01T01:01"));
        ytOfferUploadQueueService.enqueue(List.of(second), LocalDateTime.now().minus(OUTDATED_MINUTES + 1, TIME_UNIT));
        return offers;
    }
}
