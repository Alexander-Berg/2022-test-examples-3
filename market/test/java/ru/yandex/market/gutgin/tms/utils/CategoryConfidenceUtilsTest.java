package ru.yandex.market.gutgin.tms.utils;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CategoryConfidenceUtilsTest {

    private static final long SKU_ID = 1L;
    private ModelStorage.Model.Builder skuBuilder;
    private DataCampOffer.Offer.Builder offerBuilder;

    @Before
    public void setUp() {
        skuBuilder = ModelStorage.Model.newBuilder()
                .setId(SKU_ID);
        offerBuilder = DataCampOffer.Offer.newBuilder();
    }

    @Test
    public void whenNoMbocAndMboConfidenceThenDefault() {
        ModelStorage.CategoryConfidence result =
                CategoryConfidenceUtils.extractConfidence(skuBuilder.build(), offerBuilder.build());
        assertThat(result).isEqualTo(ModelStorage.CategoryConfidence.CATEGORY_CONFIDENCE_UNKNOWN);
    }

    @Test
    public void whenNoMbocAndExistsMboConfidenceThenMbo() {
        skuBuilder.setCategoryConfidence(ModelStorage.CategoryConfidence.CATEGORY_CONFIDENCE_CONTENT);
        ModelStorage.CategoryConfidence result =
                CategoryConfidenceUtils.extractConfidence(skuBuilder.build(), offerBuilder.build());
        assertThat(result).isEqualTo(ModelStorage.CategoryConfidence.CATEGORY_CONFIDENCE_CONTENT);
    }

    @Test
    public void whenMbocExistsAndNoMboConfidenceThenMboc() {
        offerBuilder.getContentBuilder().getStatusBuilder().getContentSystemStatusBuilder()
                .setCategoryConfidence(DataCampContentStatus.CategoryConfidence.CATEGORY_CONFIDENCE_AUTO_CONFIDENT);
        ModelStorage.CategoryConfidence result =
                CategoryConfidenceUtils.extractConfidence(skuBuilder.build(), offerBuilder.build());
        assertThat(result).isEqualTo(ModelStorage.CategoryConfidence.CATEGORY_CONFIDENCE_AUTO_CONFIDENT);
    }

    @Test
    public void whenMbocConfidenceIsOfHigherPriorityThenMboc() {
        offerBuilder.getContentBuilder().getStatusBuilder().getContentSystemStatusBuilder()
                .setCategoryConfidence(DataCampContentStatus.CategoryConfidence.CATEGORY_CONFIDENCE_AUTO_CONFIDENT);
        skuBuilder.setCategoryConfidence(ModelStorage.CategoryConfidence.CATEGORY_CONFIDENCE_AUTO);
        ModelStorage.CategoryConfidence result =
                CategoryConfidenceUtils.extractConfidence(skuBuilder.build(), offerBuilder.build());
        assertThat(result).isEqualTo(ModelStorage.CategoryConfidence.CATEGORY_CONFIDENCE_AUTO_CONFIDENT);
    }

    @Test
    public void whenMboConfidenceIsOfHigherPriorityThenMbo() {
        offerBuilder.getContentBuilder().getStatusBuilder().getContentSystemStatusBuilder()
                .setCategoryConfidence(DataCampContentStatus.CategoryConfidence.CATEGORY_CONFIDENCE_PARTNER);
        skuBuilder.setCategoryConfidence(ModelStorage.CategoryConfidence.CATEGORY_CONFIDENCE_CONTENT);
        ModelStorage.CategoryConfidence result =
                CategoryConfidenceUtils.extractConfidence(skuBuilder.build(), offerBuilder.build());
        assertThat(result).isEqualTo(ModelStorage.CategoryConfidence.CATEGORY_CONFIDENCE_CONTENT);
    }
}
