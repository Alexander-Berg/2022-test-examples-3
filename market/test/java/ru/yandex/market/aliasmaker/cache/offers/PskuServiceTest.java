package ru.yandex.market.aliasmaker.cache.offers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.cache.CategoryCache;
import ru.yandex.market.aliasmaker.cache.models.ModelService;
import ru.yandex.market.aliasmaker.cache.params.CategoryParamsCache;
import ru.yandex.market.aliasmaker.models.CategoryKnowledge;
import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.LocalizedString;
import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;
import ru.yandex.market.mbo.http.ModelStorage.Relation;
import ru.yandex.matcher.be.OfferCopy;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static ru.yandex.market.robot.db.ParameterValueComposer.BARCODE_ID;
import static ru.yandex.market.robot.db.ParameterValueComposer.VENDOR_CODE_ID;

public class PskuServiceTest {

    private static final Long DESCRIPTION_ID = 15341921L;
    private static final Long CATEGORY_ID = 0L;
    private static final Long PARENT_ID = 0L;
    private static final Long CHILD_ID = 1L;
    private static final String VENDOR_CHILD_1 = "Vendor-00001";
    private static final String VENDOR_CHILD_2 = "Vendor-00002";
    private static final String VENDOR_PARENT = "Vendor-00003";
    private static final String DESCRIPTION_PARENT = "Description";
    private static final String BARCODE_CHILD = "Barcode-00001";
    private static final List<Model> models = Arrays.asList(
            Model.newBuilder()
                    .setId(PARENT_ID)
                    .addTitles(toLocalizedString("Parent Model"))
                    .addAllParameterValues(Arrays.asList(
                            toStrParameterValue(VENDOR_CODE_ID, VENDOR_PARENT),
                            toStrParameterValue(DESCRIPTION_ID, DESCRIPTION_PARENT)
                    ))
                    .addRelations(
                            Relation.newBuilder()
                                    .setCategoryId(CATEGORY_ID)
                                    .setType(ModelStorage.RelationType.SKU_MODEL)
                                    .setId(CHILD_ID)
                                    .build()
                    )
                    .setDeleted(true)
                    .build(),
            Model.newBuilder()
                    .setId(CHILD_ID)
                    .addTitles(toLocalizedString("Child Model"))
                    .addAllParameterValues(Arrays.asList(
                            toStrParameterValue(VENDOR_CODE_ID, VENDOR_CHILD_1),
                            toStrParameterValue(VENDOR_CODE_ID, VENDOR_CHILD_2),
                            toStrParameterValue(BARCODE_ID, BARCODE_CHILD)
                    ))
                    .addRelations(
                            Relation.newBuilder()
                                    .setCategoryId(CATEGORY_ID)
                                    .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                    .setId(PARENT_ID)
                                    .build()
                    )
                    .setDeleted(false)
                    .build()
    );
    CategoryKnowledge emptyCategoryKnowledgeMock = new CategoryKnowledge() {
        @Override
        public CategoryParamsCache getParamsCache() {
            return new CategoryParamsCache() {
                @Override
                public List<MboParameters.Option> getOptions(long paramId) {
                    return Collections.emptyList();
                }
            };
        }
    };
    private PskuService pskuService;

    private static LocalizedString toLocalizedString(String string) {
        return LocalizedString.newBuilder()
                .setValue(string)
                .build();
    }

    private static ParameterValue toStrParameterValue(Long parameterId, String value) {
        return ParameterValue
                .newBuilder()
                .setParamId(parameterId)
                .addStrValue(toLocalizedString(value))
                .build();
    }

    @Before
    public void setUp() throws Exception {
        ModelService modelService = new ModelServiceMock(models);
        CategoryCache categoryCache = Mockito.mock(CategoryCache.class);

        Mockito.when(categoryCache.getGlobalVendorName(anyInt()))
                .thenReturn("");

        Mockito.when(categoryCache.getCategory(anyInt()))
                .thenReturn(emptyCategoryKnowledgeMock);

        this.pskuService = new PskuService(modelService, categoryCache);
    }

    @Test
    public void testGetOffersForPSKU20() {
        Offer offer = pskuService.getOffers(Collections.singletonList("1")).get(0);
        OfferCopy offerCopy = offer.getOfferCopy();

        assertEquals(offerCopy.getVendorCode(), VENDOR_CHILD_1);
        assertEquals(offerCopy.getBarcode(), BARCODE_CHILD);
        assertEquals(offer.getDescription(), DESCRIPTION_PARENT);
    }

    private static class ModelServiceMock extends ModelService {

        private final Map<Long, Model> modelMap;

        public ModelServiceMock(List<Model> models) {
            this.modelMap = models.stream().collect(Collectors.toMap(
                    Model::getId,
                    Function.identity()
            ));
        }

        @Override
        public AliasMaker.GetModelsResponse findModels(ModelStorage.FindModelsRequest request) {
            AliasMaker.GetModelsResponse.Builder getModelsResponseBuilder = AliasMaker.GetModelsResponse.newBuilder();

            boolean acceptDeleted = request.getDeleted() == ModelStorage.FindModelsRequest.DeletedState.DELETED ||
                    request.getDeleted() == ModelStorage.FindModelsRequest.DeletedState.ALL;

            boolean acceptAlive = request.getDeleted() == ModelStorage.FindModelsRequest.DeletedState.ALIVE ||
                    request.getDeleted() == ModelStorage.FindModelsRequest.DeletedState.ALL;

            for (Long id : request.getModelIdsList()) {
                Model model = modelMap.get(id);
                boolean shouldAddModel = model != null && (
                        (model.getDeleted() && acceptDeleted) || (!model.getDeleted() && acceptAlive)
                );

                if (shouldAddModel) {
                    getModelsResponseBuilder.addModel(model);
                }
            }

            return getModelsResponseBuilder.build();
        }


    }


}
