package ru.yandex.market.mboc.common.services.offers.mapping;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.mapping;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.simpleOffer;

/**
 * @author danfertev
 * @since 27.12.2018
 */
public class CheckMappingServiceTest {
    private static final long CATEGORY_ID = 200;
    private static final long MAPPING_ID = 1L;

    private CheckMappingService checkMappingService;
    private Map<Long, Model> models = new HashMap<>();

    @Before
    public void setUp() {
        this.checkMappingService = new CheckMappingService();
    }

    @Test
    public void testNoMapping() {
        CheckMappingResult result = checkMapping(simpleOffer(), Offer.MappingType.APPROVED);
        assertThat(result.getResultType()).isEqualTo(CheckMappingResult.Type.NO_MAPPING);
    }

    @Test
    public void testNotFoundModel() {
        Offer offer = simpleOffer()
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(mapping(MAPPING_ID), CONTENT);
        CheckMappingResult result = checkMapping(offer, Offer.MappingType.APPROVED);
        assertCause(result, CheckMappingResult.CauseType.NOT_FOUND_MODEL, MAPPING_ID);
    }


    @Test
    public void testUnpublishedModel() {
        Offer offer = simpleOffer()
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(mapping(MAPPING_ID), CONTENT);
        addModel(new Model()
            .setCategoryId(CATEGORY_ID)
            .setId(MAPPING_ID)
            .setPublishedOnBlueMarket(false)
            .setSkuModel(true));

        CheckMappingResult result = checkMapping(offer, Offer.MappingType.APPROVED);
        assertCause(result, CheckMappingResult.CauseType.UNPUBLISHED_MODEL, MAPPING_ID);
    }

    @Test
    public void testNotSkuModel() {
        Offer offer = simpleOffer()
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(mapping(MAPPING_ID), CONTENT);
        addModel(new Model()
            .setCategoryId(CATEGORY_ID)
            .setId(MAPPING_ID)
            .setPublishedOnBlueMarket(true)
            .setSkuModel(false));

        CheckMappingResult result = checkMapping(offer, Offer.MappingType.APPROVED);
        assertCause(result, CheckMappingResult.CauseType.NOT_SKU_MODEL, MAPPING_ID);
    }

    @Test
    public void testCorrectMapping() {
        Offer offer = simpleOffer()
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(mapping(MAPPING_ID), CONTENT);
        addModel(new Model()
            .setCategoryId(CATEGORY_ID)
            .setId(MAPPING_ID)
            .setPublishedOnBlueMarket(true)
            .setSkuModel(true));

        CheckMappingResult result = checkMapping(offer, Offer.MappingType.APPROVED);
        assertThat(result.getResultType()).isEqualTo(CheckMappingResult.Type.CORRECT);
        assertThat(result.getCauseList()).isEmpty();
    }

    private CheckMappingResult checkMapping(Offer offer, Offer.MappingType mappingType) {
        return checkMappingService.checkMapping(models, Collections.emptyMap(), offer, mappingType);
    }

    private void assertCause(CheckMappingResult result, CheckMappingResult.CauseType causeType,
                             long id, String message) {
        assertThat(result.getResultType()).isEqualTo(CheckMappingResult.Type.INVALID);
        Optional<CheckMappingResult.Cause> causeOpt = result.getCauseList().stream()
            .filter(c -> c.getType() == causeType)
            .findFirst();
        assertThat(causeOpt.isPresent()).as("Check cause list contains %s", causeType).isTrue();
        CheckMappingResult.Cause cause = causeOpt.get();
        assertThat(cause.getId()).isEqualTo(id);
        if (message != null && message.isEmpty()) {
            assertThat(cause.getMessage()).isEqualTo(message);
        }
    }

    private void assertCause(CheckMappingResult result, CheckMappingResult.CauseType causeType, long id) {
        assertCause(result, causeType, id, "");
    }

    private void addModel(Model model) {
        models.put(model.getId(), model);
    }
}
