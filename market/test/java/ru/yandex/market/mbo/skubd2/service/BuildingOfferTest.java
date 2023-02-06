package ru.yandex.market.mbo.skubd2.service;

import org.assertj.core.api.ListAssert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.skubd2.CategoryEntityUtils;
import ru.yandex.market.mbo.skubd2.load.dao.ParameterValue;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jkt on 27.07.18.
 */
public class BuildingOfferTest {

    private static final Consumer<MboParameters.Category.Builder> NOP = builder -> {
    };

    private static final long PARAMETER_ID = 1234567L;
    private static final long ANOTHER_PARAM_ID = 987654L;
    private static final long DELIVERY_WEIGHT_PARAM_ID = 10732698;

    private Skutcher skutcher;
    private ConfigurableSkutcherProxyBuilder skutcherProxyConfig;
    private SkuBDApi.OfferInfo offerInfo;

    @Before
    public void initSkutcherProxyBuilder() throws IOException {
        String categoryFileName = getClass().getResource("/proto_json/parameters_91491.json").getFile();
        String modelFileName = getClass().getResource("/proto_json/sku_91491.json").getFile();

        CategorySkutcher realCategorySkutcher = CategoryEntityUtils.buildCategorySkutcher(
            categoryFileName, modelFileName, NOP
        );

        skutcherProxyConfig = new ConfigurableSkutcherProxyBuilder(realCategorySkutcher);

        offerInfo = SkuBDApi.OfferInfo.newBuilder()
            .setCategoryId(91491)
            .setModelId(14206711)
            .setTitle("iPhone 7 plus 128GB красный")
            .build();
    }

    @Test
    public void whenClearIfAbsentInSkuParametersNotSetShouldAddFormalizedParameterToResult() {
        skutcher = skutcherProxyConfig
            .noneParameterClearsIfAbsentInSku()
            .buildDefaultSkutcher();

        offerInfo = offerInfo.toBuilder()
            .addFormalizedParam(formalizedParamFor(PARAMETER_ID))
            .build();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).contains(PARAMETER_ID);
    }

    @Test
    public void whenClearIfAbsentInSkuParametersSetShouldNotAddFormalizedParameterToResult() {
        skutcher = skutcherProxyConfig
            .addClearIfAbsentInSkuParametersToConfiguration(PARAMETER_ID)
            .buildDefaultSkutcher();

        offerInfo = offerInfo.toBuilder()
            .addFormalizedParam(formalizedParamFor(PARAMETER_ID))
            .build();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).doesNotContain(PARAMETER_ID);
    }

    @Test
    public void whenClearIfAbsentInSkuParametersSetShouldAddOtherParametersToResult() {
        skutcher = skutcherProxyConfig
            .addClearIfAbsentInSkuParametersToConfiguration(PARAMETER_ID)
            .buildDefaultSkutcher();

        offerInfo = offerInfo.toBuilder()
            .addFormalizedParam(formalizedParamFor(PARAMETER_ID))
            .addFormalizedParam(formalizedParamFor(ANOTHER_PARAM_ID))
            .build();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).doesNotContain(PARAMETER_ID);
        assertThatFormalizedParameters(offer).contains(ANOTHER_PARAM_ID);
    }

    @Test
    public void whenClearIfAbsentInSkuParametersSetAndValueIsSetInSkuShouldNotAddParameterToResult() {
        skutcher = skutcherProxyConfig
            .addParameterValuesToSkutchedSku(numericValueFor(PARAMETER_ID))
            .addClearIfAbsentInSkuParametersToConfiguration(PARAMETER_ID)
            .buildDefaultSkutcher();

        offerInfo = offerInfo.toBuilder()
            .addFormalizedParam(formalizedParamFor(PARAMETER_ID))
            .build();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).contains(PARAMETER_ID);
    }

    @Test
    public void whenClearIfAbsentInSkuParametersSetAndParameterExtractedFromOfferShouldNotAddParameterToResult() {
        skutcher = skutcherProxyConfig
            .addExtractedFromOfferParameterValuesToSkutchingResult(numericValueFor(PARAMETER_ID))
            .addClearIfAbsentInSkuParametersToConfiguration(PARAMETER_ID)
            .buildDefaultSkutcher();

        offerInfo = offerInfo.toBuilder()
            .addFormalizedParam(formalizedParamFor(PARAMETER_ID))
            .build();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).doesNotContain(PARAMETER_ID);
    }

    @Test
    public void whenOfferNotSkutchedClearIfAbsentInSkuParametersShouldNotAffectResult() {
        skutcher = skutcherProxyConfig
            .addClearIfAbsentInSkuParametersToConfiguration(PARAMETER_ID)
            .makeCategorySkutcherNoSkutch()
            .buildDefaultSkutcher();

        offerInfo = offerInfo.toBuilder()
            .addFormalizedParam(formalizedParamFor(PARAMETER_ID))
            .build();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).contains(PARAMETER_ID);
    }

    @Test
    public void whenClearIfAbsentInSkuParametersNotSetAndParameterExtractedFromOfferShouldAddParameterToResult() {
        skutcher = skutcherProxyConfig
            .addExtractedFromOfferParameterValuesToSkutchingResult(numericValueFor(PARAMETER_ID))
            .noneParameterClearsIfAbsentInSku()
            .buildDefaultSkutcher();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).contains(PARAMETER_ID);
    }

    @Test
    public void whenParameterInAllSkuParametersShouldNotAddParameterToResult() {
        skutcher = skutcherProxyConfig
            .addSkuParametersToConfiguration(PARAMETER_ID)
            .noneParameterClearsIfAbsentInSku()
            .buildDefaultSkutcher();

        offerInfo = offerInfo.toBuilder()
            .addFormalizedParam(formalizedParamFor(PARAMETER_ID))
            .build();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).doesNotContain(PARAMETER_ID);
    }

    @Test
    public void whenParameterNotInAllSkuParametersShouldAddParameterToResult() {
        skutcher = skutcherProxyConfig
            .addSkuParametersToConfiguration(PARAMETER_ID)
            .noneParameterClearsIfAbsentInSku()
            .buildDefaultSkutcher();

        offerInfo = offerInfo.toBuilder()
            .addFormalizedParam(formalizedParamFor(ANOTHER_PARAM_ID))
            .build();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).contains(ANOTHER_PARAM_ID);
    }

    // Убрать тест после https://st.yandex-team.ru/MBO-16468
    @Test
    public void whenWeightDimensionParameterShouldAddParameterToResult() {
        skutcher = skutcherProxyConfig
            .addSkuParametersToConfiguration(DELIVERY_WEIGHT_PARAM_ID)
            .noneParameterClearsIfAbsentInSku()
            .buildDefaultSkutcher();

        offerInfo = offerInfo.toBuilder()
            .addFormalizedParam(formalizedParamFor(DELIVERY_WEIGHT_PARAM_ID))
            .build();

        SkuBDApi.SkuOffer offer = skutcher.skutch(offerInfo);

        assertThatFormalizedParameters(offer).contains(DELIVERY_WEIGHT_PARAM_ID);
    }


    private ListAssert<Long> assertThatFormalizedParameters(SkuBDApi.SkuOffer offer) {
        return assertThat(offer.getFormalizedParamList().stream()
            .map(FormalizerParam.FormalizedParamPosition::getParamId)
            .map(Integer::longValue)
            .collect(Collectors.toList())
        );
    }

    private FormalizerParam.FormalizedParamPosition formalizedParamFor(long paramId) {
        return FormalizerParam.FormalizedParamPosition.newBuilder().setParamId((int) paramId).build();
    }

    private ParameterValue numericValueFor(Long paramId) {
        return ParameterValue.newNumericValue(paramId, 5555.0);
    }
}
