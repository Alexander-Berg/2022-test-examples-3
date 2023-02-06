package ru.yandex.market.mbo.export.modelstorage.pipe;

import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.ModelStopWordsModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModelStopWordsFilterPipePartTest {
    private static final Long WHITE_LIST_CATEGORY = 123L;
    private static final Long BLACK_LIST_CATEGORY = 321L;

    private static final List<String> STOP_WORDS = Stream.of(
        "ABC",
        "cat",
        "new"
    ).collect(Collectors.toList());

    private static final Collection<ModelStopWordsModel> MODEL_STOP_WORDS_MODELS = Stream.of(
        new ModelStopWordsModel(
            STOP_WORDS.get(0),
            Collections.emptySet(),
            Collections.emptySet()
        ),
        new ModelStopWordsModel(
            STOP_WORDS.get(1),
            Collections.emptySet(),
            Collections.emptySet()

        ),
        new ModelStopWordsModel(
            STOP_WORDS.get(2),
            Collections.singleton(BLACK_LIST_CATEGORY),
            Collections.singleton(WHITE_LIST_CATEGORY)
        )
    ).collect(Collectors.toList());

    @Test
    public void pipePartIsDisabledTest() throws IOException {
        ModelStopWordsFilterPipePart disabledPipePart = new ModelStopWordsFilterPipePart(
            WHITE_LIST_CATEGORY, MODEL_STOP_WORDS_MODELS, false
        );

        final String stopWord = "abc cba"; // contains stop word "abc"

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(stopWord).build())
                .build()
            ).build();

        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.emptyList()
        );

        disabledPipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modelAfterAccepting = context.getModel();

        assertTrue(modelAfterAccepting.getPublished());
        assertTrue(modelAfterAccepting.getPublishedOnBlueMarket());
        assertTrue(modelAfterAccepting.getPublishedOnMarket());
    }

    @Test
    public void modelWithoutStopWords() throws IOException {
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("ok param").build())
                .build())
            .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
                .addStrValue(MboParameters.Word.newBuilder().setName("ok hypothesis").build()).build())
            .addTitles(ModelStorage.LocalizedString.newBuilder().setValue("ok title").build())
            .addAliases(ModelStorage.LocalizedString.newBuilder().setValue("ok alias").build())
            .build();

        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.emptyList()
        );

        ModelStopWordsFilterPipePart pipePart = new ModelStopWordsFilterPipePart(
            WHITE_LIST_CATEGORY, MODEL_STOP_WORDS_MODELS, true
        );
        pipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modelAfterAccepting = context.getModel();

        assertTrue(modelAfterAccepting.getPublished());
        assertTrue(modelAfterAccepting.getPublishedOnBlueMarket());
        assertTrue(modelAfterAccepting.getPublishedOnMarket());
    }

    @Test
    public void testIfModelParameterContainsStopWord() throws IOException {
        final String stopWord = "abc cba";

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(stopWord).build())
                .build()
            ).build();

        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.emptyList()
        );

        ModelStopWordsFilterPipePart pipePart = new ModelStopWordsFilterPipePart(
            WHITE_LIST_CATEGORY, MODEL_STOP_WORDS_MODELS, true
        );
        pipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modelAfterAccepting = context.getModel();

        assertFalse(modelAfterAccepting.getPublished());
        assertFalse(modelAfterAccepting.getPublishedOnBlueMarket());
        assertFalse(modelAfterAccepting.getPublishedOnMarket());
    }

    @Test
    public void testIfSkuHypothesisContainStopWord() throws IOException {
        final String stopWord = "abc\tcba";

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("OK").build())
                .build()
            ).build();

        ModelStorage.Model sku = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.SKU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addParameterValueHypothesis(ModelStorage.ParameterValueHypothesis.newBuilder()
                .addStrValue(MboParameters.Word.newBuilder().setName(stopWord).build()).build())
            .build();

        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.singletonList(sku)
        );

        ModelStopWordsFilterPipePart pipePart = new ModelStopWordsFilterPipePart(
            WHITE_LIST_CATEGORY, MODEL_STOP_WORDS_MODELS, true
        );
        pipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modelAfterAccepting = context.getModel();
        ModelStorage.Model.Builder skuAfterAccepting = context.getSkus().get(0);

        assertTrue(modelAfterAccepting.getPublished());
        assertTrue(modelAfterAccepting.getPublishedOnBlueMarket());
        assertTrue(modelAfterAccepting.getPublishedOnMarket());

        assertFalse(skuAfterAccepting.getPublished());
        assertFalse(skuAfterAccepting.getPublishedOnBlueMarket());
        assertFalse(skuAfterAccepting.getPublishedOnMarket());
    }

    @Test
    public void testIfTitleContainsStopWord() throws IOException {
        final String stopWord = "abc\rcba";

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .build();

        ModelStorage.Model sku = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.SKU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addTitles(ModelStorage.LocalizedString.newBuilder().setValue(stopWord).build())
            .build();

        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.singletonList(sku)
        );

        ModelStopWordsFilterPipePart pipePart = new ModelStopWordsFilterPipePart(
            WHITE_LIST_CATEGORY, MODEL_STOP_WORDS_MODELS, true
        );
        pipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder skuAfterAccepting = context.getSkus().get(0);

        assertFalse(skuAfterAccepting.getPublished());
        assertFalse(skuAfterAccepting.getPublishedOnBlueMarket());
        assertFalse(skuAfterAccepting.getPublishedOnMarket());
    }

    @Test
    public void testIfAliasContainsStopWord() throws IOException {
        final String stopWord = "cat\tghj";

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .build();

        ModelStorage.Model sku = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.SKU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addAliases(ModelStorage.LocalizedString.newBuilder().setValue(stopWord).build())
            .build();

        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.singletonList(sku)
        );

        ModelStopWordsFilterPipePart pipePart = new ModelStopWordsFilterPipePart(
            WHITE_LIST_CATEGORY, MODEL_STOP_WORDS_MODELS, true
        );
        pipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder skuAfterAccepting = context.getSkus().get(0);

        assertFalse(skuAfterAccepting.getPublished());
        assertFalse(skuAfterAccepting.getPublishedOnBlueMarket());
        assertFalse(skuAfterAccepting.getPublishedOnMarket());
    }

    @Test
    public void testIfModelContainsHiddenEngLetterStopWord() throws IOException {
        final String stopWord = "сat"; //с - is russian

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addAliases(ModelStorage.LocalizedString.newBuilder().setValue(stopWord).build())
            .build();


        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.emptyList()
        );

        ModelStopWordsFilterPipePart pipePart = new ModelStopWordsFilterPipePart(
            WHITE_LIST_CATEGORY, MODEL_STOP_WORDS_MODELS, true
        );
        pipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modelAfterAccepting = context.getModel();

        assertFalse(modelAfterAccepting.getPublished());
        assertFalse(modelAfterAccepting.getPublishedOnBlueMarket());
        assertFalse(modelAfterAccepting.getPublishedOnMarket());
    }

    @Test
    public void modelWithStopWordInBlackListTest() throws IOException {
        final String stopWord = "new";

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addAliases(ModelStorage.LocalizedString.newBuilder().setValue(stopWord).build())
            .build();


        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.emptyList()
        );

        ModelStopWordsFilterPipePart pipePart = new ModelStopWordsFilterPipePart(
            BLACK_LIST_CATEGORY, MODEL_STOP_WORDS_MODELS, true
        );
        pipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modelAfterAccepting = context.getModel();

        assertFalse(modelAfterAccepting.getPublished());
        assertFalse(modelAfterAccepting.getPublishedOnBlueMarket());
        assertFalse(modelAfterAccepting.getPublishedOnMarket());
    }


    @Test
    public void modelWithStopWordInWhiteListTest() throws IOException {
        final String stopWord = "new";

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addAliases(ModelStorage.LocalizedString.newBuilder().setValue(stopWord).build())
            .build();


        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.emptyList()
        );

        ModelStopWordsFilterPipePart pipePart = new ModelStopWordsFilterPipePart(
            WHITE_LIST_CATEGORY, MODEL_STOP_WORDS_MODELS, true
        );
        pipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modelAfterAccepting = context.getModel();

        assertTrue(modelAfterAccepting.getPublished());
        assertTrue(modelAfterAccepting.getPublishedOnBlueMarket());
        assertTrue(modelAfterAccepting.getPublishedOnMarket());
    }


    @SuppressWarnings("checkstyle:magicNumber")
    @Test
    public void modelWithStopWordWhenBlackListIsEmpty() throws IOException {
        final String stopWord = "abc";

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setPublished(true)
            .setPublishedOnBlueMarket(true)
            .setPublishedOnMarket(true)
            .addTitles(ModelStorage.LocalizedString.newBuilder().setValue(stopWord).build())
            .build();


        ModelPipeContext context = new ModelPipeContext(
            model, Collections.emptyList(), Collections.emptyList()
        );

        ModelStopWordsFilterPipePart pipePart = new ModelStopWordsFilterPipePart(
            999L, MODEL_STOP_WORDS_MODELS, true
        );
        pipePart.acceptModelsGroup(context);

        ModelStorage.Model.Builder modelAfterAccepting = context.getModel();

        assertFalse(modelAfterAccepting.getPublished());
        assertFalse(modelAfterAccepting.getPublishedOnBlueMarket());
        assertFalse(modelAfterAccepting.getPublishedOnMarket());
    }

}
