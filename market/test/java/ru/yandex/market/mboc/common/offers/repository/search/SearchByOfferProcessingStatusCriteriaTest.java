package ru.yandex.market.mboc.common.offers.repository.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepositoryImpl;
import ru.yandex.market.mboc.common.offers.mbi.ProcessingStatusConverter;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.SupplierOffer;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.mapping;

@SuppressWarnings("checkstyle:MagicNumber")
public class SearchByOfferProcessingStatusCriteriaTest extends BaseDbTestClass {

    private static final int SEED = 42;
    private static final int AMOUNT = 1000;
    private static final int GUARANTEED_AMOUNT = 100;

    @Autowired
    private OfferRepositoryImpl offerRepository;
    @Autowired
    private SupplierRepositoryImpl supplierRepository;
    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(SEED)
            .build();
    }

    @Test
    public void testCriteria() {
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        List<Offer> offers = new ArrayList<>();
        Offer.ProcessingStatus[] processingStatuses = Offer.ProcessingStatus.values();
        for (int i = 0; i < AMOUNT; i++) {
            Offer offer = OfferTestUtils.simpleOffer();
            offer.setShopSku("shop-sku-" + i);
            offer.setCategoryIdForTests(99L, Offer.BindingKind.APPROVED);
            // first 100 is guaranteed, after that - random
            if (i < GUARANTEED_AMOUNT) {
                offer.updateProcessingStatusIfValid(
                    processingStatuses[i % processingStatuses.length]);
            } else {
                if (random.nextDouble() > 0.25) {
                    offer.updateProcessingStatusIfValid(random.nextObject(Offer.ProcessingStatus.class));
                }
            }
            offer.updateAcceptanceStatusForTests(random.nextObject(Offer.AcceptanceStatus.class));
            if (random.nextDouble() > 0.75) {
                offer.updateApprovedSkuMapping(mapping(Math.abs(random.nextLong())),
                    Offer.MappingConfidence.CONTENT);
            } else if (random.nextDouble() > 0.9) {
                offer.updateApprovedSkuMapping(mapping(0), Offer.MappingConfidence.CONTENT);
            }

            if (random.nextDouble() > 0.9) {
                offer.setSupplierSkuMapping(mapping(Math.abs(random.nextLong())));
            }

            offers.add(offer);
        }
        offerRepository.insertOffers(offers);

        Map<SupplierOffer.OfferProcessingStatus, Set<Long>> idsByStatus = offers.stream()
            .collect(groupingBy(ProcessingStatusConverter::getOfferProcessingStatusNew,
                Collectors.mapping(Offer::getId, toSet())));

        // Check each and every status
        SupplierOffer.OfferProcessingStatus[] statuses = SupplierOffer.OfferProcessingStatus.values();
        Stream.of(statuses).forEach(status -> {
            List<Offer> selected = offerRepository.findOffers(new OffersFilter()
                .addCriteria(new SearchByOfferProcessingStatusCriteria(
                    Collections.singletonList(status))));

            Assertions.assertThat(selected)
                .withFailMessage("Checking selected to contain only %s", status)
                .extracting(ProcessingStatusConverter::getOfferProcessingStatusNew)
                .allMatch(s -> s == status);

            Assertions.assertThat(idsByStatus.get(status))
                .withFailMessage("Checking %s status is generated", status)
                .isNotEmpty();

            Assertions.assertThat(selected).extracting(Offer::getId)
                .withFailMessage("Checking %s status to match ids", status)
                .containsExactlyInAnyOrderElementsOf(idsByStatus.get(status));
        });

        // Check each pair
        for (int i = 0; i < statuses.length; i++) {
            SupplierOffer.OfferProcessingStatus status1 = statuses[i];
            for (int j = i + 1; j < statuses.length; j++) {
                SupplierOffer.OfferProcessingStatus status2 = statuses[j];

                List<Offer> selected = offerRepository.findOffers(new OffersFilter()
                    .addCriteria(new SearchByOfferProcessingStatusCriteria(
                        Arrays.asList(status1, status2))));

                Assertions.assertThat(selected)
                    .withFailMessage("Checking selected to contain only {} and {}", status1, status2)
                    .extracting(ProcessingStatusConverter::getOfferProcessingStatusNew)
                    .allMatch(s -> s == status1 || s == status2);

                Assertions.assertThat(selected).extracting(Offer::getId)
                    .withFailMessage("Checking {} and {} status to match ids", status1, status2)
                    .containsExactlyInAnyOrderElementsOf(
                        Sets.union(idsByStatus.get(status1), idsByStatus.get(status2)));
            }
        }
    }
}
