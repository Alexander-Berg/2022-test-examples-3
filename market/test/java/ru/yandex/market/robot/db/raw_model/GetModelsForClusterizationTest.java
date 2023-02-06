package ru.yandex.market.robot.db.raw_model;

import com.ninja_squad.dbsetup.operation.CompositeOperation;
import com.ninja_squad.dbsetup.operation.Operation;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.qameta.allure.Step;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.test.util.random.RandomBean;
import ru.yandex.market.robot.db.RawModelStorage;
import ru.yandex.market.robot.db.raw_model.config.MarketRawModelAccessorsTestConfig;
import ru.yandex.market.robot.db.raw_model.tables.*;
import ru.yandex.market.robot.shared.raw_model.*;
import ru.yandex.market.test.db.DatabaseTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newHashSet;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;
import static ru.yandex.market.test.db.DatabaseTester.Utils.insert;

/**
 * @author jkt on 07.12.17.
 */

@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class})
@ContextConfiguration(classes = MarketRawModelAccessorsTestConfig.class)
@Transactional
public class GetModelsForClusterizationTest {

    private static final int CATEGORY_ID = 123;
    private static final int VENDOR_ID = 555;
    private static final int ANOTHER_VENDOR_ID = 666;

    private static final String INVALID_UNKNOWN_PARAM = "invalid_unknown_param";

    private static final String PROMO_URL = "promoUrl";
    private static final String INSTRUCTION_URL = "instructionUrl";
    private static final String DRIVERS_URL = "driversUrl";
    private static final String BARCODE = "barcode";
    private static final String RECOMMENDED_PRICE = "recommendedPrice";

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();


    @Autowired
    DatabaseTester dataBase;

    @Autowired
    RawModelStorage rawModelStorage;


    private RawModel modelData;


    @Before
    public void initializeModelData() {
        modelData = randomRawModel(CATEGORY_ID);

        dataBase.modify(insertModelDataToRequiredTables(modelData));
    }


    @Test
    public void whenGettingModelsShouldReturnCorrectModelData() {
        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        String[] fieldsToIgnore = join(
            Fields.NOT_USED_IN_THIS_METHOD,
            Fields.FROM_MARKET_RELATION_TABLE,
            Fields.FROM_MODEL_PARAM_TABLE,
            Fields.FROM_MODEL_PICTURE_TABLE,
            Fields.FROM_MODEL_RECOMMENDATION_TABLE
        );

        assertThat(rawModel)
            .as("Поля возвращенной модели, не соответствуют значениям в базе")
            .isEqualToIgnoringGivenFields(modelData, fieldsToIgnore);
    }

    @Test
    public void whenGettingModelsShouldReturnCorrectMarketRelationData() {
        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertThat(rawModel.getMarketRelations())
            .as("Данные в marketRelations, не соответствуют значениям в базе")
            .isEqualToComparingOnlyGivenFields(modelData.getMarketRelations(), Fields.VENDOR_ID, Fields.VENDOR_NAME); // остальные в этом методе не инициализируются
    }

    @Test
    public void whenGettingModelsShouldReturnCorrectModelParamData() {
        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertThat(rawModel.getParams())
            .as("Данные в парамметрах модели, не соответствуют значениям в базе")
            .containsExactlyInAnyOrder(modelData.getParams().stream().toArray(RawModel.Param[]::new));
    }

    @Test
    public void whenParameterInternalShouldSpecifyAdditionalParametersForModel() {
        setInternalParams(randomParam(PROMO_URL), randomParam(INSTRUCTION_URL), randomParam(DRIVERS_URL));

        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertSoftly(soft ->
            modelData.getParams().forEach(param -> {
                    String fieldName = param.getName() + "s";
                    soft.assertThat(rawModel)
                        .as("%s содержит неверные значения.", fieldName)
                        .hasFieldOrPropertyWithValue(fieldName, newHashSet("http://" + param.getValue()));
                }
            )
        );
    }

    @Test
    public void whenAddingUrlParameterWithProtocolShouldNotAddProtocolAgain() {
        RawModel.Param promoUrlWithHttp = randomParam(PROMO_URL);
        promoUrlWithHttp.setValue("http://" + EnhancedRandom.random(String.class));

        setInternalParams(promoUrlWithHttp);

        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertThat(rawModel.getPromoUrls())
            .containsExactlyInAnyOrder(promoUrlWithHttp.getValue());

    }

    @Test
    public void whenInternalBarcodeParameterPresentShouldAddBarcodeToModel() {
        RawModel.Param barcode = randomParam(BARCODE);
        setInternalParams(barcode);

        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertThat(rawModel.getBarcodes())
            .as("Список баркодов не соответствует значениям в базе в базе")
            .containsExactlyInAnyOrder(barcode.getValue());
    }


    @Test
    public void whenInternalRecommendedPriceParameterPresentShouldAddPriceInfoToModel() {
        RawModel.Param recommendedPrice = randomParam(RECOMMENDED_PRICE);
        recommendedPrice.setValue(EnhancedRandom.random(Double.class).toString());

        setInternalParams(recommendedPrice);

        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertSoftly(softly -> {
            softly.assertThat(rawModel.getRecommendedPrice()).isEqualTo(Double.parseDouble(recommendedPrice.getValue()));
            softly.assertThat(rawModel.getRecommendedPriceCurrency()).isEqualTo(recommendedPrice.getUnit());
        });
    }

    @Test
    public void whenUnknownInternalParameterPresentShouldThrowException() {
        setInternalParams(randomParam(INVALID_UNKNOWN_PARAM));

        assertThatThrownBy(() -> getModelsForClusterization(CATEGORY_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(INVALID_UNKNOWN_PARAM);

    }


    @Test
    public void whenPictureIsNotDeletedShouldReturnPictureUrls() {
        Set<Picture> pictureUrls = modelData.getPictureUrls();
        pictureUrls.forEach(picture -> picture.setDeleted(false));

        dataBase.modify(
            deleteAllFrom(ModelPictureTable.NAME),
            insert(ModelPictureTable::entryFor, modelData)
        );

        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertThat(rawModel.getPictureUrls())
            .as("Данные в урлах картинок, не соответствуют значениям в базе")
            .containsExactlyInAnyOrder(pictureUrls.stream().toArray(Picture[]::new));
    }

    @Test
    public void whenPictureIsDeletedShouldNotReturnPictureUrls() {
        Set<Picture> pictureUrls = modelData.getPictureUrls();
        pictureUrls.forEach(picture -> picture.setDeleted(true));

        dataBase.modify(
            deleteAllFrom(ModelPictureTable.NAME),
            insert(ModelPictureTable::entryFor, modelData)
        );

        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertThat(rawModel.getPictureUrls())
            .as("Возвращает урлы для удаленных картинок")
            .isEmpty();
    }


    @Test
    public void whenRecommendationsPresentShouldReturnCorrectData() {
        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertThat(rawModel.getRecommendations())
            .as("Данные в  рекоммендациях, не соответствуют значениям в базе")
            .containsExactlyInAnyOrder(modelData.getRecommendations().stream().toArray(Recommendation[]::new));
    }

    @Test
    public void whenMarketModelsPresentShouldReturnCorrectData() {
        Set<MarketModel> marketModels = modelData.getMarketRelations().getMarketModels();

        RawModel rawModel = getSingleModelForClusterization(CATEGORY_ID);

        assertThat(rawModel.getMarketRelations().getMarketModels())
            .as("Данные в  рекоммендациях, не соответствуют значениям в базе")
            .containsExactlyInAnyOrder(marketModels.stream().toArray(MarketModel[]::new));
    }


    @Test
    public void whenModelStatusIsWrongShouldIgnoreThatModel() {
        modelData.getMarketRelations().setStatus(Status.WRONG);

        dataBase.modify(
            deleteAllFrom(MarketRelationTable.NAME),
            insert(MarketRelationTable::entryFor, modelData)
        );

        List<RawModel> modelsForClusterization = getModelsForClusterization(CATEGORY_ID);

        assertThat(modelsForClusterization)
            .as("Вернул модели со статусом %s", Status.WRONG)
            .hasSize(0);
    }

    @Test
    public void whenAliasesPresentShouldParseCorrectly() {
        Set<String> aliases = EnhancedRandom.randomSetOf(3, String.class);

        modelData.setAliases(aliases);
        dataBase.modify(
            deleteAllFrom(ModelTable.NAME),
            insert(ModelTable::entryFor, modelData)
        );

        RawModel model = getSingleModelForClusterization(CATEGORY_ID);

        assertThat(model.getAliases())
            .as("Алиасы спарсились неверно", Status.WRONG)
            .containsExactlyInAnyOrder(aliases.stream().toArray(String[]::new));
    }

    @Test
    public void whenGettingModelsShouldCallCallbackMethodsInOrder() {
        RawModelStorage.Callback callback = mock(RawModelStorage.Callback.class);

        rawModelStorage.getModelsForClusterization(CATEGORY_ID, callback);

        InOrder inOrder = Mockito.inOrder(callback);
        inOrder.verify(callback, times(1)).startVendor(modelData.getMarketRelations().getVendorId());
        inOrder.verify(callback, times(1)).process(anyCollection());
        inOrder.verify(callback, times(1)).finishVendor();

    }

    @Test
    public void whenAllModelsFromSingleVendorShouldStartFinishVendorOnce() {
        dataBase.modify(
            insertModelDataToRequiredTables(
                randomRawModel(CATEGORY_ID, VENDOR_ID),
                randomRawModel(CATEGORY_ID, VENDOR_ID)
            )
        );

        RawModelStorage.Callback callback = mock(RawModelStorage.Callback.class);

        rawModelStorage.getModelsForClusterization(CATEGORY_ID, callback);

        InOrder inOrder = Mockito.inOrder(callback);
        inOrder.verify(callback, times(1)).startVendor(anyInt());
        inOrder.verify(callback, times(1)).finishVendor();

    }

    @Test
    public void whenModelsFromDifferentVendorsShouldStartFinishEachVendor() {
        dataBase.modify(
            insertModelDataToRequiredTables(
                randomRawModel(CATEGORY_ID, VENDOR_ID),
                randomRawModel(CATEGORY_ID, ANOTHER_VENDOR_ID),
                randomRawModel(CATEGORY_ID, VENDOR_ID),
                randomRawModel(CATEGORY_ID, ANOTHER_VENDOR_ID)
            )
        );

        RawModelStorage.Callback callback = mock(RawModelStorage.Callback.class);

        rawModelStorage.getModelsForClusterization(CATEGORY_ID, callback);

        InOrder inOrder = Mockito.inOrder(callback);
        inOrder.verify(callback, times(1)).startVendor(VENDOR_ID);
        inOrder.verify(callback, times(1)).finishVendor();
        inOrder.verify(callback, times(1)).startVendor(ANOTHER_VENDOR_ID);
        inOrder.verify(callback, times(1)).finishVendor();

    }


    private String[] join(String[]... arrays) {
        return Stream.of(arrays).flatMap(Stream::of).toArray(String[]::new);
    }

    private RawModel getSingleModelForClusterization(int categoryId) {
        List<RawModel> models = getModelsForClusterization(categoryId);

        if (models.size() != 1) {
            throw new IllegalStateException("Должна была вернуться ровно одна модель. Но вернулось: " + models);
        }

        return models.get(0);
    }

    @Step("Получаем модели для кластеризации категории: {categoryId}")
    private List<RawModel> getModelsForClusterization(int categoryId) {
        RawModelStorage.Callback callback = mock(RawModelStorage.Callback.class);

        List<RawModel> rawModels = new ArrayList<>();

        //ArgumentCaptor не сработает. Коллекция параметров чистится сразу после вызова. Ловит только пустую коллекцию.
        doAnswer(invocation -> {
            ObjectCollection<RawModel> argumentModels = invocation.<ObjectCollection<RawModel>>getArgument(0);
            rawModels.addAll(argumentModels);

            return null;
        }).when(callback).process(anyCollection());

        rawModelStorage.getModelsForClusterization(categoryId, callback);

        return rawModels;
    }

    private void setInternalParams(RawModel.Param... params) {
        modelData.setParams(Stream.of(params).collect(Collectors.toSet()));

        dataBase.modify(
            deleteAllFrom(ModelParamTable.NAME),
            insert(ModelParamTable::entryAsInternalParamFor, modelData)
        );
    }

    private RawModel randomRawModel() {
        EnhancedRandom random = RandomBean.defaultRandomBuilder()
            .randomize(Recommendation.class,
                (Supplier<Recommendation>) () -> RandomBean.generateComplete(Recommendation.class))
            .randomize(Picture.class,
                (Supplier<Picture>) () -> {
                    Picture picture = RandomBean.generateComplete(Picture.class);
                    picture.setDownloadStatus(Picture.DownloadStatus.DOWNLOADED);
                    return picture;
                })
            .randomize(RawModel.Param.class,
                (Supplier<RawModel.Param>) () -> RandomBean.generateComplete(RawModel.Param.class))
            .randomize(MarketModel.class,
                (Supplier<MarketModel>) () -> RandomBean.generateComplete(MarketModel.class))
            .randomize(MarketRelations.class,
                (Supplier<MarketRelations>) () -> RandomBean.generateComplete(MarketRelations.class))
            .build();

        RawModel model = random.nextObject(RawModel.class);

        model.setMarketRelations(RandomBean.generateComplete(MarketRelations.class));

        RandomBean.assumeHasNoneNullFields(model);
        RandomBean.assumeHasNoneNullFields(model.getMarketRelations());
        RandomBean.assumeHasCompleteEntries(model.getParams());
        RandomBean.assumeHasCompleteEntries(model.getPictureUrls());
        RandomBean.assumeHasCompleteEntries(model.getRecommendations());
        RandomBean.assumeHasCompleteEntries(model.getMarketRelations().getMarketModels());

        return model;
    }

    private RawModel randomRawModel(int categoryId) {
        RawModel model = randomRawModel();
        model.getMarketRelations().setMarketCategoryId(categoryId);

        return model;
    }

    private RawModel randomRawModel(int categoryId, int vendorId) {
        RawModel model = randomRawModel(categoryId);
        model.getMarketRelations().setVendorId(vendorId);

        return model;
    }

    private RawModel.Param randomParam(String name) {
        RawModel.Param param = RandomBean.generateComplete(RawModel.Param.class);
        param.setName(name);

        return param;
    }


    private Operation insertModelDataToRequiredTables(RawModel... modelData) {
        return CompositeOperation.sequenceOf(
            insert(ModelTable::entryFor, modelData),
            insert(MarketRelationTable::entryFor, modelData),
            insert(MarketRelationModelTable::entryFor, modelData),
            insert(ModelParamTable::entryAsExternalParamFor, modelData),
            insert(ModelPictureTable::entryFor, modelData),
            insert(ModelRecommendationTable::entryFor, modelData)
        );
    }

    private static class Fields {
        private static final String VENDOR_ID = "vendorId";
        private static final String VENDOR_NAME = "vendorName";

        private static final String[] NOT_USED_IN_THIS_METHOD = new String[]{
            // не извлекаются из базы при вызове этого метода
            "sourceName",
            "rawId",
            "categoryId", //используется market_relation.market_category_id
            "announceDate",
            "inStockDate",
            "createVersionDate",
            "createVersionNumber",
            "firstVersionDate",
            "firstVersionNumber",
            "lastVersionDate",
            "lastVersionNumber",
            "actual",
            "deleted",
            "vendor",
            "changed"
        };

        private static final String[] FROM_MARKET_RELATION_TABLE = new String[]{
            "marketRelations"
        };

        private static final String[] FROM_MODEL_PARAM_TABLE = new String[]{
            "promoUrls",
            "instructionUrls",
            "driversUrls",
            "barcodes",
            "recommendedPrice",
            "recommendedPriceCurrency"
        };

        private static final String[] FROM_MODEL_PICTURE_TABLE = new String[]{
            "pictureUrls",
            "hasQueuedDownloads"
        };

        private static final String[] FROM_MODEL_RECOMMENDATION_TABLE = new String[]{
            "recommendations"
        };
    }

}
