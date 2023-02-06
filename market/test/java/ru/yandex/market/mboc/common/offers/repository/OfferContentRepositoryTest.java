package ru.yandex.market.mboc.common.offers.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

public class OfferContentRepositoryTest extends BaseDbTestClass {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private OfferContentRepository offerContentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();


    @Before
    public void init() {
        supplierRepository.insertBatch(ImmutableSet.of(
            new Supplier(42, "Test1"),
            new Supplier(43, "Test2"),
            new Supplier(44, "Test3"),
            new Supplier(50, "Test4"),
            new Supplier(51, "Test5"),
            new Supplier(99, "Test6"),
            new Supplier(100, "Test7"),
            new Supplier(45, "Test8")
        ));
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/sample-offers.json");
        offers.forEach(offer -> offer.setIsOfferContentPresent(true));
        offerRepository.insertOffers(offers);

    }

    @Test
    public void testInsertContentDataDuringInsertOffer() {
        List<Offer> allOffers = offerRepository.findAll();
        List<OfferContent> allContent = offerContentRepository.findAll();

        Map<Long, List<Object>> contentPayload = allContent.stream().collect(Collectors.toMap(
            content -> content.getId(),
            content -> Arrays.asList(
                content.getDescription(),
                content.getExtraShopFields(),
                content.getUrls()
            )
        ));
        Map<Long, List<Object>> offerPayload = allOffers.stream().collect(Collectors.toMap(
            offer -> offer.getId(),
            offer -> Arrays.asList(
                offer.extractOfferContent().getDescription(),
                offer.extractOfferContent().getExtraShopFields(),
                offer.extractOfferContent().getUrls()
            )
        ));

        Assertions.assertThat(contentPayload).isEqualTo(offerPayload);
    }

    @Test
    public void testUpdateOfferContent() {
        List<Offer> forUpdate = offerRepository.findAll().stream().map(offer -> {
            OfferContent offerContent = offer.extractOfferContent();
            OfferContent.OfferContentBuilder offerContentBuilder = OfferContent.copyToBuilder(offerContent);
            offerContentBuilder.description(offerContent.getDescription() + 1);
            List<String> urls = new ArrayList<>(offerContent.getUrls());
            urls.add("test");
            offerContentBuilder.urls(urls);
            Map<String, String> extraShopFields = new HashMap<>(offerContent.getExtraShopFields());
            extraShopFields.put("new", "new");
            offerContentBuilder.extraShopFields(extraShopFields);
            offer.storeOfferContent(offerContentBuilder.build());
            return offer;
        }).collect(Collectors.toList());
        offerRepository.updateOffers(forUpdate);

        List<Offer> allOffers = offerRepository.findAll();
        List<OfferContent> allContent = offerContentRepository.findAll();

        Map<Long, List<Object>> contentPayload = allContent.stream().collect(Collectors.toMap(
            content -> content.getId(),
            content -> Arrays.asList(
                content.getDescription(),
                content.getExtraShopFields(),
                content.getUrls()
            )
        ));
        Map<Long, List<Object>> offerPayload = allOffers.stream().collect(Collectors.toMap(
            offer -> offer.getId(),
            offer -> Arrays.asList(
                offer.extractOfferContent().getDescription(),
                offer.extractOfferContent().getExtraShopFields(),
                offer.extractOfferContent().getUrls()
            )
        ));

        Assertions.assertThat(contentPayload).isEqualTo(offerPayload);
    }

    @Test
    public void testIncrementOfferVersionAfterUpdateContent() {
        Offer offer = offerRepository.findAll().get(0);
        offer.storeOfferContent(offer.getOfferContentBuilder().description("new").build());
        offerRepository.updateOffers(offer);
        Offer offerById = offerRepository.getOfferById(offer.getId());
        Assertions.assertThat(offerById.getLastVersion()).isGreaterThan(
            offer.getLastVersion());
        Assertions.assertThat(offerById.extractOfferContent().getDescription()).isEqualTo(
            offer.extractOfferContent().getDescription());
    }

    @Test
    public void testDeleteOfferContentAfterRemoveOffer() {
        Offer offer = offerRepository.findAll().get(0);
        Assertions.assertThat(offerContentRepository.findById(offer.getId())).isNotNull();
        offerRepository.removeOffer(offer);
        Assertions.assertThat(offerContentRepository.findByIds(Collections.singleton(offer.getId()))).isEmpty();
    }

    private OfferContent getOfferContent(java.sql.ResultSet rs) throws SQLException {
        try {
            String[] array = (String[]) ((PgArray) rs.getObject(3)).getArray();
            Map value = objectMapper.readValue(((PGobject) rs.getObject(4)).getValue(), Map.class);
            return OfferContent.builder()
                .id(rs.getLong(1))
                .description(rs.getString(2))
                .urls(Arrays.asList(array))
                .extraShopFields(value)
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Error during extract offer " + rs.getLong(1), e);
        }
    }


}
