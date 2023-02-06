package ru.yandex.market.mboc.common.offers.repository;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author apluhin
 * @created 2/16/21
 */
public class OfferLogIdServiceTest extends BaseDbTestClass {

    private OfferLogIdService offerLogIdService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @Before
    public void setUp() throws Exception {
        offerLogIdService = new OfferLogIdService(jdbcTemplate);
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
    }

    @Test
    public void testUpdateTriggerAfterDeleteOffer() {
        offerRepository.insertOffers(OfferTestUtils.simpleOffer());
        Assertions.assertThat(offerLogIdService.getNewModifiedSequenceCount()).isEqualTo(1);
        offerLogIdService.updateModifiedSequence();
        Assertions.assertThat(offerLogIdService.getNewModifiedSequenceCount()).isEqualTo(0);
        offerRepository.deleteAllInTest();
        Assertions.assertThat(offerRepository.findAll().size()).isEqualTo(0);
        Assertions.assertThat(offerLogIdService.getNewModifiedSequenceCount()).isEqualTo(1);

    }

    @Test
    public void testMarkOffersModified() {
        var offer1 = offerRepository.insertAndGetOffer(OfferTestUtils.nextOffer());
        var offer2 = offerRepository.insertAndGetOffer(OfferTestUtils.nextOffer());
        jdbcTemplate.update(
            "update mbo_category.log_id_offer set modified_seq_id = nextval('mbo_category.log_id_offer_seq')"
        );

        offerLogIdService.markOffersModified(List.of(offer1, offer2));

        Assertions.assertThat(findModifiedSeqId(List.of(offer1, offer2)))
            .hasSize(2)
            .containsOnlyNulls();
    }

    private List<Long> findModifiedSeqId(List<Offer> baseOffers) {
        var idsStr = baseOffers.stream().map(Offer::getId)
            .map(Object::toString)
            .collect(Collectors.joining(",", "(", ")"));
        return jdbcTemplate.queryForList(
            "select modified_seq_id from mbo_category.log_id_offer where id in " + idsStr, Long.class);
    }
}
