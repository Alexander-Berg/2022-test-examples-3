package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.upload.YtOfferUploadQueueRepository;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.upload.YtOfferUploadQueueServiceImpl;
import ru.yandex.market.mboc.common.test.YsonTestUtil;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.yt.YtOffersReader;
import ru.yandex.market.mboc.tms.executors.data.OffersFetchStatistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mboc.common.services.monitorings.offers.OutdatedYtOffersMonitoringStrategy.OUTDATED_MINUTES;
import static ru.yandex.market.mboc.common.services.monitorings.offers.OutdatedYtOffersMonitoringStrategy.TIME_UNIT;

/**
 * @author yuramalinov
 * @created 26.06.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class FetchYtOffersExecutorTest {
    private OfferRepositoryMock offerRepository;
    private YtOffersReader offersReader;
    private FetchYtOffersExecutor executor;
    private CategoryCachingServiceMock categoryCachingService;
    private SupplierRepositoryMock supplierRepository;
    private YtOfferUploadQueueRepository ytOfferUploadQueueRepository;

    @Before
    public void setup() {
        offerRepository = new OfferRepositoryMock();
        offersReader = mock(YtOffersReader.class);
        categoryCachingService = new CategoryCachingServiceMock();
        supplierRepository = new SupplierRepositoryMock();
        ytOfferUploadQueueRepository = mock(YtOfferUploadQueueRepository.class);
        var ytOfferUploadQueueService = new YtOfferUploadQueueServiceImpl(ytOfferUploadQueueRepository);
        executor = new FetchYtOffersExecutor(offerRepository, offersReader, supplierRepository,
                ytOfferUploadQueueService, 12);
    }

    @Test
    public void testOffersAreNotUpdated() {
        YTreeListNode mappings = YsonTestUtil.readJsonAsYson(this, "yt-mappings/yt-mappings.json");
        Mockito.doAnswer(call -> {
            Consumer<YTreeMapNode> consumer = call.getArgument(0);
            mappings.forEach(e -> consumer.accept(e.mapNode()));
            return null;
        }).when(offersReader).readOffers(Mockito.any());

        LocalDateTime dateInPast = LocalDateTime.of(2014, 1, 1, 0, 0);
        Offer offer = new Offer()
                .setBusinessId(42)
                .setShopSku("test")
                .setTitle("Test title")
                .setShopCategoryName("Cat")
                .setIsOfferContentPresent(true)
                .storeOfferContent(OfferContent.initEmptyContent())
                .updateApprovedSkuMapping(new Offer.Mapping(3, DateTimeUtils.dateTimeNow()),
                        Offer.MappingConfidence.CONTENT)
                .setContentSkuMapping(new Offer.Mapping(3, dateInPast))
                .setUpdated(dateInPast);
        offerRepository.insertOffer(offer);

        categoryCachingService.addCategory(102, "Test category");
        supplierRepository.insert(new Supplier(42, "Test"));

        Offer before = offerRepository.getOfferById(offer.getId()); // refresh

        doReturn(List.of()).when(ytOfferUploadQueueRepository).findEnqueuedInPeriod(
                argThat(nearEnough(LocalDateTime.now().minus(OUTDATED_MINUTES, TIME_UNIT))), any());

        OffersFetchStatistics statistics = executor.scanYtMappings(null);
        assertEquals(1, statistics.getWrongSupplier());
        assertEquals(1, statistics.getWrongMapping());
        assertEquals(1, statistics.getEmptyTitle());
        assertEquals(0, statistics.getNotSyncedYet());
        assertEquals(7, statistics.getTotal());

        Offer after = offerRepository.getOfferById(offer.getId());

        // Обновляем офферы при заборе из YT
        Assertions.assertThat(before.getLastVersion()).isEqualTo(after.getLastVersion());
        assertEquals("Test title", after.getTitle());
        assertEquals("Cat", after.getShopCategoryName());
        assertNull(after.getVendor());
        assertNull(after.getVendorCode());
        assertNull(after.getBarCode());
        assertNull(after.extractOfferContent().getDescription());
    }

    private ArgumentMatcher<LocalDateTime> nearEnough(LocalDateTime targetTime) {
        return givenTime -> {
            // Error up to 10 sec
            return Math.abs(givenTime.toEpochSecond(ZoneOffset.UTC) - targetTime.toEpochSecond(ZoneOffset.UTC)) <= 10;
        };
    }
}
