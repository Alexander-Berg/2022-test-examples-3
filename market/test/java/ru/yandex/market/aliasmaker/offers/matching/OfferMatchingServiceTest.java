package ru.yandex.market.aliasmaker.offers.matching;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.cache.models.CategoryModelsCache;
import ru.yandex.market.aliasmaker.cache.models.ModelsShadowReloadingCache;
import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.matcher.be.OfferCopy;
import ru.yandex.matcher.be.formalized.ParamBag;

/**
 * @author york
 * @since 22.04.2019
 */
public class OfferMatchingServiceTest {
    private static final int LOCAL_VENDOR_1 = 1;
    private static final int LOCAL_VENDOR_2 = 2;
    private static final int LOCAL_VENDOR_3 = 3;

    private static final int ENUM_PARAM_1 = 1;
    private static final int NUMERIC_PARAM_2 = 2;

    private static final int PARAM_1_VALUE_1 = 1;
    private static final int PARAM_1_VALUE_2 = 2;
    private static final int PARAM_1_VALUE_3 = 2;

    private static int idSeq = 1;

    private OffersMatchingService service = new OffersMatchingService();


    @Test
    public void testBaseFilter() {
        Offer priceConflict = offer();
        priceConflict.setMatchType(Matcher.MatchType.PRICE_CONFLICT);
        Offer signalNotMatched = offer();
        signalNotMatched.setSignalSize(2);
        Offer signalMatched = offer();
        signalMatched.setSignalSize(3);
        signalMatched.setModelId(1);
        Offer signalSkutched = offer();
        signalSkutched.setSignalSize(5);
        signalSkutched.setModelId(1);
        signalSkutched.setMarketSkuId(2L);
        Offer notSignalSkutched = offer();
        notSignalSkutched.setModelId(1);
        notSignalSkutched.setMarketSkuId(2L);

        List<Offer> offers = Arrays.asList(
                priceConflict,
                signalMatched,
                signalNotMatched,
                signalSkutched,
                notSignalSkutched
        );

        Filter filter = new Filter().setWithPriceConflict(true);
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(priceConflict);

        filter = new Filter().setSignal(true);
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(signalMatched, signalNotMatched, signalSkutched);

        filter = new Filter().setSignal(false);
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(notSignalSkutched);

        filter = new Filter().setMatched(true);
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(signalMatched, signalSkutched, notSignalSkutched);
        //all offers without price conflict
        Assertions.assertThat(doFilter(offers, filter, true))
                .containsExactlyInAnyOrder(signalMatched, signalNotMatched, signalSkutched, notSignalSkutched);

        filter = new Filter().setMatched(false);
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(signalNotMatched);

        filter = new Filter().setSkutched(false);
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(signalMatched, signalNotMatched);

        filter = new Filter().setSkutched(true);
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(signalSkutched, notSignalSkutched);

        //all offers without price conflict
        Assertions.assertThat(doFilter(offers, filter, true))
                .containsExactlyInAnyOrder(signalMatched, signalNotMatched, signalSkutched, notSignalSkutched);
    }

    @Test
    public void testInnerFilters() {
        Offer withDatasourceParam1 = offer(LOCAL_VENDOR_1);
        withDatasourceParam1.setShopName("datasource");
        addParamValues(withDatasourceParam1, ENUM_PARAM_1, PARAM_1_VALUE_1);

        Offer withDatasource = offer(LOCAL_VENDOR_1);
        withDatasource.setShopName("datasource");

        Offer withDatasource2 = offer(LOCAL_VENDOR_2);
        withDatasource2.setShopName("datasource2");

        Offer multiValues = offer(LOCAL_VENDOR_2);
        addParamValues(multiValues, ENUM_PARAM_1, PARAM_1_VALUE_1, PARAM_1_VALUE_3);

        Offer twoParams = offer(LOCAL_VENDOR_1);
        addParamValues(twoParams, ENUM_PARAM_1, PARAM_1_VALUE_2);
        addNumericValue(twoParams, NUMERIC_PARAM_2, 1d);

        List<Offer> offers = Arrays.asList(
                withDatasourceParam1,
                withDatasource,
                withDatasource2,
                multiValues,
                twoParams
        );

        Filter filter = new Filter().addInnerFilter(
                Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                        .setSourceId(Markup.OfferFilter.Property.SITE.getNumber())
                        .setStringValue("datasource")
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(withDatasourceParam1, withDatasource);

        filter = new Filter().addInnerFilter(
                Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                        .setSourceId(Markup.OfferFilter.Property.VENDOR.getNumber())
                        .addValueIds(LOCAL_VENDOR_1)
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(withDatasourceParam1, withDatasource, twoParams);
        Assertions.assertThat(doFilter(offers, filter, true))
                .containsExactlyInAnyOrder(offers.toArray(new Offer[0]));

        filter = new Filter().addInnerFilter(
                Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PARAMETER)
                        .setOperator(Markup.OfferFilter.Operator.FORMALIZED)
                        .setSourceId(ENUM_PARAM_1)
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(withDatasourceParam1, multiValues, twoParams);

        filter = new Filter().addInnerFilter(
                Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PARAMETER)
                        .setOperator(Markup.OfferFilter.Operator.NOT_FORMALIZED)
                        .setSourceId(ENUM_PARAM_1)
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(withDatasource, withDatasource2);

        filter = new Filter().addInnerFilter(
                Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PARAMETER)
                        .setOperator(Markup.OfferFilter.Operator.MATCHES)
                        .setSourceId(ENUM_PARAM_1)
                        .addValueIds(PARAM_1_VALUE_1)
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(withDatasourceParam1, multiValues);

        filter = new Filter().addInnerFilter(
                Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PARAMETER)
                        .setSourceId(ENUM_PARAM_1)
                        .setOperator(Markup.OfferFilter.Operator.MATCHES)
                        .addValueIds(PARAM_1_VALUE_2)
                        .addValueIds(PARAM_1_VALUE_3)
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(multiValues, twoParams);

        filter = new Filter().addInnerFilter(
                Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PARAMETER)
                        .setSourceId(ENUM_PARAM_1)
                        .setOperator(Markup.OfferFilter.Operator.MISMATCHES)
                        .addValueIds(PARAM_1_VALUE_1)
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(withDatasource, withDatasource2, twoParams);

        filter = new Filter().addInnerFilter(
                Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PARAMETER)
                        .setSourceId(NUMERIC_PARAM_2)
                        .setOperator(Markup.OfferFilter.Operator.MATCHES)
                        .setNumericValue(1d)
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(twoParams);

        filter = new Filter().addInnerFilter(
                Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PARAMETER)
                        .setSourceId(NUMERIC_PARAM_2)
                        .setOperator(Markup.OfferFilter.Operator.MATCHES)
                        .setNumericValue(2d)
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .isEmpty();
    }

    @Test
    public void testCutOff() {
        Offer offer = offer(LOCAL_VENDOR_1);
        Offer offer2 = offer(LOCAL_VENDOR_2);
        Offer cutOff = offer(LOCAL_VENDOR_2);
        cutOff.setMatchType(Matcher.MatchType.CUT_OF_WORDS);

        List<Offer> offers = Arrays.asList(
                offer,
                offer2,
                cutOff
        );

        Filter filter = new Filter();
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(offer, offer2);
        Assertions.assertThat(doFilter(offers, filter, true))
                .containsExactlyInAnyOrder(offer, offer2);

        filter.setWithCutOff(true);
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(cutOff);
        Assertions.assertThat(doFilter(offers, filter, true))
                .containsExactlyInAnyOrder(cutOff);
    }

    @Test
    public void testVendorsFilter() {
        Offer noVendorOffer = offer(0);
        Offer v1offer1 = offer(LOCAL_VENDOR_1);
        Offer v2offer1 = offer(LOCAL_VENDOR_2);
        Offer v2offer2 = offer(LOCAL_VENDOR_2);
        Offer v3offer1 = offer(LOCAL_VENDOR_3);

        List<Offer> offers = Arrays.asList(
                noVendorOffer,
                v1offer1,
                v2offer1,
                v2offer2,
                v3offer1
        );
        Filter filter = new Filter();

        filter.addInnerFilter(Markup.OfferFilter.newBuilder()
                .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                .setSourceId(Markup.OfferFilter.Property.VENDOR_VALUE)
                .setOperator(Markup.OfferFilter.Operator.MATCHES)
        );
        // == [] i.e. not matched to vendor
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(noVendorOffer);

        filter.clearInnerFilters().addInnerFilter(Markup.OfferFilter.newBuilder()
                .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                .setSourceId(Markup.OfferFilter.Property.VENDOR_VALUE)
                .setOperator(Markup.OfferFilter.Operator.MISMATCHES)
        );
        // != [] i.e. matched to vendor
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(v1offer1, v2offer1, v2offer2, v3offer1);

        filter.clearInnerFilters().addInnerFilter(Markup.OfferFilter.newBuilder()
                .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                .setSourceId(Markup.OfferFilter.Property.VENDOR_VALUE)
                .setOperator(Markup.OfferFilter.Operator.MATCHES)
                .addValueIds((long) LOCAL_VENDOR_1)
                .addValueIds((long) LOCAL_VENDOR_3)
        );
        // == [1, 3]
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(v1offer1, v3offer1);

        filter.clearInnerFilters().addInnerFilter(Markup.OfferFilter.newBuilder()
                .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                .setSourceId(Markup.OfferFilter.Property.VENDOR_VALUE)
                .setOperator(Markup.OfferFilter.Operator.MISMATCHES)
                .addValueIds((long) LOCAL_VENDOR_1)
                .addValueIds((long) LOCAL_VENDOR_2)
        );
        // != [1, 2]
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(noVendorOffer, v3offer1);
    }

    @Test
    public void testShopFilter() {
        Offer offer1 = offer(LOCAL_VENDOR_1);
        offer1.setShopName("sh1");
        Offer offer2 = offer(LOCAL_VENDOR_1);
        offer2.setShopName("sh2");
        Offer offer3 = offer(LOCAL_VENDOR_1);
        offer3.setShopName("sh3");

        List<Offer> offers = Arrays.asList(
                offer1,
                offer2,
                offer3
        );
        Filter filter = new Filter();

        filter.addInnerFilter(Markup.OfferFilter.newBuilder()
                .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                .setSourceId(Markup.OfferFilter.Property.SITE_VALUE)
                .setOperator(Markup.OfferFilter.Operator.MATCHES)
                .setStringValue("sh1")
        );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(offer1);

        filter.clearInnerFilters()
                .addInnerFilter(Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                        .setSourceId(Markup.OfferFilter.Property.SITE_VALUE)
                        .setOperator(Markup.OfferFilter.Operator.MISMATCHES)
                        .setStringValue("sh1")
                );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(offer2, offer3);

        filter.clearInnerFilters()
                .addInnerFilter(Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                        .setSourceId(Markup.OfferFilter.Property.SITE_VALUE)
                        .setOperator(Markup.OfferFilter.Operator.MATCHES)
                        .addAllStringValues(Arrays.asList("sh1", "sh2"))
                );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(offer1, offer2);

        filter.clearInnerFilters()
                .addInnerFilter(Markup.OfferFilter.newBuilder()
                        .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                        .setSourceId(Markup.OfferFilter.Property.SITE_VALUE)
                        .setOperator(Markup.OfferFilter.Operator.MISMATCHES)
                        .addAllStringValues(Arrays.asList("sh2", "sh3"))
                );
        Assertions.assertThat(doFilter(offers, filter, false))
                .containsExactlyInAnyOrder(offer1);
    }

    @Test
    public void testMatchedOnOperatorModelFilter() {
        int categoryId = 1;
        int modelId11 = 11;
        int modelId22 = 22;
        Offer offer1 = offer();
        offer1.setModelId(modelId11);
        Offer offer2 = offer();
        offer2.setModelId(modelId22);
        Offer offer3 = offer();

        List<Offer> offers = Arrays.asList(offer1, offer2, offer3);

        CategoryModelsCache categoryModelsCache = Mockito.mock(CategoryModelsCache.class);


        ModelsShadowReloadingCache modelCache = Mockito.mock(ModelsShadowReloadingCache.class);
        Mockito.when(modelCache.isOperatorQualityModel(Mockito.eq(categoryId), Mockito.eq((long) modelId11)))
                .thenReturn(false);
        Mockito.when(modelCache.isOperatorQualityModel(Mockito.eq(categoryId), Mockito.eq((long) modelId22)))
                .thenReturn(true);
        Mockito.when(modelCache.getGuruModelsCache(Mockito.anyInt())).thenReturn(categoryModelsCache);

        service.setModelsCache(modelCache);

        Filter filter = new Filter();

        Assertions.assertThat(doMatchedOnModelFilter(offers, filter, categoryId))
                .containsExactlyInAnyOrder(offer1, offer2, offer3);

        filter.setMatchedOnOperatorModel(true);

        Assertions.assertThat(doMatchedOnModelFilter(offers, filter, categoryId))
                .containsExactlyInAnyOrder(offer2, offer3);

        filter.setMatchedOnOperatorModel(false);

        Assertions.assertThat(doMatchedOnModelFilter(offers, filter, categoryId))
                .containsExactlyInAnyOrder(offer1, offer3);
    }

    private void addParamValues(Offer offer, int paramId, int... optionIds) {
        for (int optionId : optionIds) {
            offer.getOfferCopy().getParamBag().addOptionValue(paramId, optionId);
        }
    }

    private void addNumericValue(Offer offer, int paramId, double value) {
        offer.getOfferCopy().getParamBag().addNumberValue(paramId, 0, value);
    }

    private List<Offer> doFilter(List<Offer> offers, Filter filter, boolean skipMatchingCheck) {
        return offers.stream().filter(o -> service.matchesBaseFilter(o, filter, skipMatchingCheck))
                .collect(Collectors.toList());
    }

    private List<Offer> doMatchedOnModelFilter(List<Offer> offers, Filter filter, int categoryId) {
        return offers.stream().filter(o -> service.matchesMatchedOnModelFilter(filter, o, categoryId))
                .collect(Collectors.toList());
    }

    private Offer offer() {
        return offer(LOCAL_VENDOR_1);
    }

    private Offer offer(int vendorId) {
        Offer offer = new Offer();
        String id = String.valueOf(idSeq++);
        offer.setClassifierMagicId(id);
        offer.setLocalVendorId(vendorId);
        OfferCopy offerCopy = OfferCopy.newBuilder()
                .setTitle(id)
                .setParamBag(new ParamBag(2))
                .build();
        offer.setOfferCopy(offerCopy);
        return offer;
    }

}
