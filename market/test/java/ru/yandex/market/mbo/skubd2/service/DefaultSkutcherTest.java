package ru.yandex.market.mbo.skubd2.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.FormalizerParam.FormalizedParamPosition;
import ru.yandex.market.ir.http.OfferProblem;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.http.SkuBDApi.Status;
import ru.yandex.market.mbo.skubd2.CategoryEntityUtils;
import ru.yandex.market.mbo.skubd2.knowledge.SkuKnowledge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.skubd2.service.DefaultSkutcher.DELIVERY_DEPTH_PARAM_ID;
import static ru.yandex.market.mbo.skubd2.service.DefaultSkutcher.DELIVERY_HEIGHT_PARAM_ID;
import static ru.yandex.market.mbo.skubd2.service.DefaultSkutcher.DELIVERY_WEIGHT_PARAM_ID;
import static ru.yandex.market.mbo.skubd2.service.DefaultSkutcher.DELIVERY_WIDTH_PARAM_ID;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DefaultSkutcherTest {
    private static final Consumer<MboParameters.Category.Builder> NOP = builder -> {
    };

    @Mock
    private SkuKnowledge skuKnowledge;
    private Skutcher skutcher;

    @Before
    public void init() throws IOException {
        String categoryFileName = getClass().getResource("/proto_json/parameters_91491.json").getFile();
        String modelFileName = getClass().getResource("/proto_json/sku_91491.json").getFile();

        CategorySkutcher categorySkutcher
            = CategoryEntityUtils.buildCategorySkutcher(categoryFileName, modelFileName, NOP);
        when(skuKnowledge.getSkutcher(anyLong())).thenReturn(categorySkutcher);
        skutcher = new DefaultSkutcher(skuKnowledge);
    }

    @Test
    public void simpleSkutch() {
        SkuBDApi.OfferInfo offerInfo = SkuBDApi.OfferInfo.newBuilder()
            .setCategoryId(91491)
            .setModelId(14206711)
            .setTitle("iPhone 7 plus 128GB красный")
            .build();
        SkuBDApi.SkuOffer skuOffer = skutcher.skutch(offerInfo);

        assertEquals(SkuBDApi.Status.OK, skuOffer.getStatus());
        assertEquals(1748279693L, skuOffer.getMarketSkuId());
    }

    @Test
    public void noModelSkutch() {
        SkuBDApi.OfferInfo offerInfo = SkuBDApi.OfferInfo.newBuilder()
            .setCategoryId(91491)
            .setModelId(111111)
            .setTitle("iPhone 7 plus 128GB странного цвета")
            .build();
        SkuBDApi.SkuOffer skuOffer = skutcher.skutch(offerInfo);

        assertEquals(Status.NO_MODEL, skuOffer.getStatus());
    }

    @Test
    public void cleanParamIfOk() {
        List<FormalizerParam.FormalizedParamPosition> formalizedParams = new ArrayList<>();
        formalizedParams.add(FormalizerParam.FormalizedParamPosition.newBuilder()
            .setParamId(15164148)
            .setOptionId(15164155)
            .setValueId(15164155)
            .setType(FormalizerParam.FormalizedParamType.ENUM)
            .build());
        SkuBDApi.OfferInfo offerInfo = SkuBDApi.OfferInfo.newBuilder()
            .setCategoryId(91491)
            .setModelId(14206711)
            .setTitle("iPhone 7 plus 128GB красный")
            .addAllFormalizedParam(formalizedParams)
            .build();
        SkuBDApi.SkuOffer skuOffer = skutcher.skutch(offerInfo);

        assertEquals(SkuBDApi.Status.OK, skuOffer.getStatus());
        assertEquals(1748279693L, skuOffer.getMarketSkuId());
        assertEqualsParamIds(new long[] {14871214}, skuOffer.getFormalizedParamList());
    }

    @Test
    public void notCleanParamIfOkForNotCompleteModelAndAddDeliveryParams() {
        List<FormalizerParam.FormalizedParamPosition> formalizedParams = new ArrayList<>();
        formalizedParams.add(FormalizerParam.FormalizedParamPosition.newBuilder()
            .setParamId(15164148)
            .setOptionId(15164155)
            .setValueId(15164155)
            .setType(FormalizerParam.FormalizedParamType.ENUM)
            .build());
        SkuBDApi.OfferInfo offerInfo = SkuBDApi.OfferInfo.newBuilder()
            .setCategoryId(91491)
            .setModelId(92352002)
            .setTitle("Телефон Apple И еще один айфон золотой")
            .addAllFormalizedParam(formalizedParams)
            .build();
        SkuBDApi.SkuOffer skuOffer = skutcher.skutch(offerInfo);

        assertEquals(SkuBDApi.Status.OK, skuOffer.getStatus());
        assertEquals(100283474558L, skuOffer.getMarketSkuId());

        assertEqualsParamIds(
            new long[]{
                15164148, DELIVERY_DEPTH_PARAM_ID, DELIVERY_HEIGHT_PARAM_ID, DELIVERY_WEIGHT_PARAM_ID,
                DELIVERY_WIDTH_PARAM_ID
            }, skuOffer.getFormalizedParamList()
        );
    }

    @Test
    public void notCleanParamIfOkForNotCompleteModelAndNotAddDeliveryParams() {
        List<FormalizerParam.FormalizedParamPosition> formalizedParams = new ArrayList<>();
        formalizedParams.add(FormalizerParam.FormalizedParamPosition.newBuilder()
            .setParamId(15164148)
            .setOptionId(15164155)
            .setValueId(15164155)
            .setType(FormalizerParam.FormalizedParamType.ENUM)
            .build());
        SkuBDApi.OfferInfo offerInfo = SkuBDApi.OfferInfo.newBuilder()
            .setCategoryId(91491)
            .setModelId(92352002)
            .setTitle("Телефон Apple И еще один айфон красный")
            .addAllFormalizedParam(formalizedParams)
            .build();
        SkuBDApi.SkuOffer skuOffer = skutcher.skutch(offerInfo);

        assertEquals(SkuBDApi.Status.OK, skuOffer.getStatus());
        assertEquals(100283866240L, skuOffer.getMarketSkuId());
        assertEqualsParamIds(new long[]{15164148}, skuOffer.getFormalizedParamList());
    }

    @Test
    public void notCleanParamIfNotSkutchAndNotCompleteModel() {
        List<FormalizerParam.FormalizedParamPosition> formalizedParams = new ArrayList<>();
        formalizedParams.add(FormalizerParam.FormalizedParamPosition.newBuilder()
            .setParamId(15164148)
            .setOptionId(15164155)
            .setValueId(15164155)
            .setType(FormalizerParam.FormalizedParamType.ENUM)
            .build());
        SkuBDApi.OfferInfo offerInfo = SkuBDApi.OfferInfo.newBuilder()
            .setCategoryId(91491)
            .setModelId(92352002)
            .setTitle("Телефон Apple И еще один айфон серобурокозявчатый")
            .addAllFormalizedParam(formalizedParams)
            .build();
        SkuBDApi.SkuOffer skuOffer = skutcher.skutch(offerInfo);

        assertEquals(Status.NO_SKU, skuOffer.getStatus());
        assertEqualsParamIds(new long[]{15164148}, skuOffer.getFormalizedParamList());
    }

    @Test
    public void testRequiredParamOfferProblem() {
        SkuBDApi.SkuOffer problemOffer = skutcher.skutch(SkuBDApi.OfferInfo.newBuilder()
            .setMarketSkuId(1748279693L)
            .build()
        );

        assertEquals(SkuBDApi.Status.OK, problemOffer.getStatus());
        assertEquals(1748279693L, problemOffer.getMarketSkuId());
        assertEquals(1, problemOffer.getOfferProblemCount());
        assertRequiredOfferProblems(problemOffer.getOfferProblem(0),
            OfferProblem.ProblemType.REQUIRED_CATEGORY_PARAM_NOT_FOUND, 13887626, "Цвет");

        SkuBDApi.SkuOffer notProblemOffer = skutcher.skutch(SkuBDApi.OfferInfo.newBuilder()
            .setMarketSkuId(1752194204L)
            .build()
        );

        assertEquals(SkuBDApi.Status.OK, notProblemOffer.getStatus());
        assertEquals(1752194204L, notProblemOffer.getMarketSkuId());
        assertEquals(0, notProblemOffer.getOfferProblemCount());
    }

    private void assertRequiredOfferProblems(OfferProblem.Problem offerProblem,
                                             OfferProblem.ProblemType type, int paramId, String paramName) {
        assertEquals(type, offerProblem.getProblemType());
        assertEquals(paramId, offerProblem.getParamId());
        assertEquals(paramName, offerProblem.getParamName());
    }

    private void assertEqualsParamIds(long[] paramIds,
                                      List<FormalizerParam.FormalizedParamPosition> formalizedParamList) {
        Arrays.sort(paramIds);
        Integer[] sortedFormalizedParamIds = formalizedParamList.stream()
            .map(FormalizedParamPosition::getParamId)
            .sorted()
            .toArray(Integer[]::new);
        for (int i = 0; i < sortedFormalizedParamIds.length; i++) {
            assertEquals(paramIds[i], (long) sortedFormalizedParamIds[i]);
        }
    }
}
