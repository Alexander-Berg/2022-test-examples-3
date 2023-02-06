package ru.yandex.market.gutgin.tms.service.fast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.assertions.GutginAssertions;
import ru.yandex.market.gutgin.tms.service.datacamp.BaseOfferToModelParameterConverter;
import ru.yandex.market.partner.content.common.utils.DcpOfferBuilder;
import ru.yandex.market.gutgin.tms.utils.ParameterCreator;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.util.LocalizedStringUtils;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class FastCardRequestCreatorTest extends DBDcpStateGenerator {

    public static final String OFFER_TITLE = "имя без лишних пробелов и символов табуляции";
    public static final String BAD_OFFER_TITLE = "имя \t\t\t\t\t\t без лишних    пробелов и \t \tсимволов табуляции \t";
    public static final long SERVICE_PARAM_ID = 1000L;

    private FastCardRequestCreator fastCardRequestCreator;
    private ModelStorageHelper modelStorageHelper;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        categoryDataKnowledge.addCategoryData(CATEGORY_ID,
                CategoryData.build(CATEGORY.toBuilder()
                        .addParameter(MboParameters.Parameter.newBuilder()
                                .setId(SERVICE_PARAM_ID)
                                .setService(true)
                        )
                        .addParameter(MboParameters.Parameter.newBuilder()
                                .setId(MainParamCreator.SHOP_SKU_PARAM_ID)
                                .setService(true)
                        ))
        );
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(
                categoryDataKnowledge,
                null
        );
        this.modelStorageHelper = mock(ModelStorageHelper.class);
        this.gcSkuValidationDao = mock(GcSkuValidationDao.class);
        ParameterCreator parameterCreator = new ParameterCreator();
        this.fastCardRequestCreator = new FastCardRequestCreator(
                parameterCreator,
                categoryDataHelper,
                new BaseOfferToModelParameterConverter()
        );
    }

    @Test
    public void requestWithMinimumParameters() {
        List<GcSkuTicket> tickets = generateTicket(1, OFFER_TITLE);

        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.emptyMap());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(1);
        ModelStorage.Model model = modelsList.get(0);
        GutginAssertions.assertThat(model)
                .hasTitle(OFFER_TITLE)
                .hasCurrentType(ModelStorage.ModelType.FAST_SKU)
                .hasSourceType(ModelStorage.ModelType.FAST_SKU)
                .hasCategoryId(CATEGORY_ID)
                .hasSupplierId(PARTNER_SHOP_ID)
                .containOnlyThisParameterValues(
                        MainParamCreator.SHOP_SKU_PARAM_ID,
                        ParameterValueComposer.VENDOR_ID,
                        ParameterValueComposer.NAME_ID
                );
    }

    @Test
    public void flagsResetTest() {
        List<GcSkuTicket> tickets = generateTicket(1, OFFER_TITLE);

        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.emptyMap());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(1);
        ModelStorage.Model model = modelsList.get(0);
        assertFalse(model.getStrictChecksRequired());
        assertFalse(model.getBroken());
    }

    @Test
    public void requestWithMinimumParametersAndBadTitle() {
        List<GcSkuTicket> tickets = generateTicket(1, BAD_OFFER_TITLE);

        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.emptyMap());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(1);
        ModelStorage.Model model = modelsList.get(0);
        GutginAssertions.assertThat(model)
                .hasTitle(OFFER_TITLE)
                .hasCurrentType(ModelStorage.ModelType.FAST_SKU)
                .hasSourceType(ModelStorage.ModelType.FAST_SKU)
                .hasCategoryId(CATEGORY_ID)
                .hasSupplierId(PARTNER_SHOP_ID)
                .containOnlyThisParameterValues(
                        MainParamCreator.SHOP_SKU_PARAM_ID,
                        ParameterValueComposer.VENDOR_ID,
                        ParameterValueComposer.NAME_ID
                );
    }

    @Test
    public void whenEmptyOptionalParamsThenTheyAreNotInRequest() {
        List<GcSkuTicket> tickets = generateTicketWithHackedParams("","");
        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.emptyMap());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(1);
        ModelStorage.Model model = modelsList.get(0);
        GutginAssertions.assertThat(model)
                .hasTitle(OFFER_TITLE)
                .hasCurrentType(ModelStorage.ModelType.FAST_SKU)
                .hasSourceType(ModelStorage.ModelType.FAST_SKU)
                .hasCategoryId(CATEGORY_ID)
                .hasSupplierId(PARTNER_SHOP_ID)
                .doesNotContainParameterValues(MainParamCreator.DESCRIPTION_ID, MainParamCreator.RAW_VENDOR_ID);
    }

    @Test
    public void skipAlmostEmptyDescription() {
        String emptyDescription = "''";
        List<GcSkuTicket> tickets = generateBigTicket(emptyDescription);

        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.emptyMap());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(1);
        ModelStorage.Model model = modelsList.get(0);
        GutginAssertions.assertThat(model)
                .hasTitle(OFFER_TITLE)
                .hasCurrentType(ModelStorage.ModelType.FAST_SKU)
                .hasSourceType(ModelStorage.ModelType.FAST_SKU)
                .hasCategoryId(CATEGORY_ID)
                .hasSupplierId(PARTNER_SHOP_ID)
                .containOnlyThisParameterValues(
                        MainParamCreator.SHOP_SKU_PARAM_ID,
                        ParameterValueComposer.VENDOR_ID,
                        ParameterValueComposer.NAME_ID
                )
                .doesNotContainParameterValues(MainParamCreator.DESCRIPTION_ID);
    }

    @Test
    public void requestWithAllPossibleParameters() {
        List<GcSkuTicket> tickets = generateBigTicket("some description");

        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.emptyMap());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(1);
        ModelStorage.Model model = modelsList.get(0);
        GutginAssertions.assertThat(model)
                .hasTitle(OFFER_TITLE)
                .hasCurrentType(ModelStorage.ModelType.FAST_SKU)
                .hasSourceType(ModelStorage.ModelType.FAST_SKU)
                .hasCategoryId(CATEGORY_ID)
                .hasSupplierId(PARTNER_SHOP_ID)
                .containOnlyThisParameterValues(
                        MainParamCreator.SHOP_SKU_PARAM_ID,
                        ParameterValueComposer.VENDOR_ID,
                        ParameterValueComposer.NAME_ID,
                        MainParamCreator.DESCRIPTION_ID
                );
    }

    @Test
    public void requestWithMultipleModels() {
        List<GcSkuTicket> tickets = generateTicket(25, OFFER_TITLE);

        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.emptyMap());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(25);
    }

    @Test
    public void requestForExistingModel() {
        long existingModelId = 5000L;

        ModelStorage.Model model = generateExistingModel(existingModelId);
        List<GcSkuTicket> tickets = generateBigTicket(existingModelId);
        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.singletonMap(existingModelId, model));

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(1);
        ModelStorage.Model actualRequestModel = modelsList.get(0);
        GutginAssertions.assertThat(actualRequestModel)
                .hasTitle(OFFER_TITLE)
                .hasCurrentType(ModelStorage.ModelType.FAST_SKU)
                .hasSourceType(ModelStorage.ModelType.FAST_SKU)
                .hasCategoryId(CATEGORY_ID)
                .hasSupplierId(PARTNER_SHOP_ID)
                .hasModifiedTs(100) // параметр берется из существующей модели, а не из офера
                .hasPublished(true) // параметр берется из существующей модели, а не из офера
                .containOnlyThisParameterValues(
                        MainParamCreator.SHOP_SKU_PARAM_ID,
                        ParameterValueComposer.VENDOR_ID,
                        ParameterValueComposer.NAME_ID,
                        MainParamCreator.DESCRIPTION_ID
                );
    }

    @Test
    public void updateExistingModelAndLeaveServiceParameters() {
        ModelStorage.ParameterValue serviceParam = ModelStorage.ParameterValue.newBuilder()
                .setParamId(SERVICE_PARAM_ID)
                .build();
        ModelStorage.ParameterValue notServiceParam = ModelStorage.ParameterValue.newBuilder()
                .setParamId(1001L)
                .build();
        ModelStorage.ParameterValue shopSkuParam = ModelStorage.ParameterValue.newBuilder()
                .setParamId(MainParamCreator.SHOP_SKU_PARAM_ID)
                .addStrValue(LocalizedStringUtils.defaultString("old_ssku"))
                .build();
        ModelStorage.Model build = generateExistingModel(100).toBuilder()
                .addAllParameterValues(Arrays.asList(serviceParam, shopSkuParam, notServiceParam))
                .build();

        List<GcSkuTicket> tickets = generateBigTicket(100L);
        String newShopSku = tickets.get(0).getShopSku();

        doReturn(Collections.singletonList(build)).when(modelStorageHelper).findModels(any());

        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.singletonMap(build.getId(), build));

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(1);
        ModelStorage.Model actualRequestModel = modelsList.get(0);
        GutginAssertions.assertThat(actualRequestModel)
                .hasTitle(OFFER_TITLE)
                .hasCurrentType(ModelStorage.ModelType.FAST_SKU)
                .hasSourceType(ModelStorage.ModelType.FAST_SKU)
                .hasCategoryId(CATEGORY_ID)
                .hasSupplierId(PARTNER_SHOP_ID)
                .hasModifiedTs(100) // параметр берется из существующей модели, а не из офера
                .hasPublished(true) // параметр берется из существующей модели, а не из офера
                .hasParamWithValue(MainParamCreator.SHOP_SKU_PARAM_ID, newShopSku)
                .containOnlyThisParameterValues(
                        MainParamCreator.SHOP_SKU_PARAM_ID,
                        ParameterValueComposer.VENDOR_ID,
                        ParameterValueComposer.NAME_ID,
                        MainParamCreator.DESCRIPTION_ID,
                        SERVICE_PARAM_ID
                );
    }

    @Test
    public void whenVendorNameIsBlankDoNotAddRawVendor() {
        List<GcSkuTicket> tickets = generateTicketWithHackedParams(" ", OFFER_TITLE);

        ModelCardApi.SaveModelsGroupRequest request = fastCardRequestCreator.createRequest(tickets,
                Collections.emptyMap());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        assertThat(modelsRequestList).hasSize(1);
        List<ModelStorage.Model> modelsList = modelsRequestList.get(0).getModelsList();
        assertThat(modelsList).hasSize(1);
        ModelStorage.Model model = modelsList.get(0);
        GutginAssertions.assertThat(model)
                .hasTitle(OFFER_TITLE)
                .hasCurrentType(ModelStorage.ModelType.FAST_SKU)
                .hasSourceType(ModelStorage.ModelType.FAST_SKU)
                .hasCategoryId(CATEGORY_ID)
                .hasSupplierId(PARTNER_SHOP_ID)
                .containOnlyThisParameterValues(
                        MainParamCreator.SHOP_SKU_PARAM_ID,
                        ParameterValueComposer.VENDOR_ID,
                        ParameterValueComposer.NAME_ID,
                        MainParamCreator.DESCRIPTION_ID
                );
    }

    private ModelStorage.Model generateExistingModel(long modelId) {
        return ModelStorage.Model.newBuilder()
                .setId(modelId)
                .addTitles(LocalizedStringUtils.defaultString("старое имя"))
                .setCurrentType(ModelStorage.ModelType.FAST_SKU.name())
                .setSourceType(ModelStorage.ModelType.FAST_SKU.name())
                .setCategoryId(0)
                .setSupplierId(10)
                .setPublished(true)
                .setModifiedTs(100)
                .build();
    }

    private ModelStorage.Model generateExistingModelWithWrongType(int modelId) {
        return ModelStorage.Model.newBuilder()
                .setId(modelId)
                .addTitles(LocalizedStringUtils.defaultString("старое имя"))
                .setCategoryId(0)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.SKU.name())
                .setSupplierId(10)
                .setPublished(true)
                .setModifiedTs(100)
                .build();
    }

    private List<GcSkuTicket> generateTicket(int amount, String title) {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(amount, datacampOffers -> {

            DatacampOffer datacampOffer = datacampOffers.get(0);
            new DcpOfferBuilder(
                    datacampOffer.getBusinessId(),
                    datacampOffer.getOfferId())
                    .build();
        });
        gcSkuTickets.forEach(gcSkuTicket -> {
            gcSkuTicket.setName(title);
            gcSkuTicket.setShopSku(gcSkuTicket.getDatacampOffer().getIdentifiers().getOfferId());
        });
        gcSkuTicketDao.update(gcSkuTickets);
        return gcSkuTickets;
    }

    private List<GcSkuTicket> generateTicketWithHackedParams(String vendor, String description) {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, datacampOffers -> {
            DatacampOffer datacampOffer = datacampOffers.get(0);
            datacampOffer.setData(new DcpOfferBuilder(datacampOffer.getBusinessId(), datacampOffer.getOfferId())
                    .withVendor(vendor)
                    .withDescription(description)
                    .build());

        });
        gcSkuTickets.forEach(gcSkuTicket -> {
            gcSkuTicket.setName(OFFER_TITLE);
            gcSkuTicket.setShopSku(gcSkuTicket.getDatacampOffer().getIdentifiers().getOfferId());
        });
        gcSkuTicketDao.update(gcSkuTickets);
        return gcSkuTickets;
    }

    private List<GcSkuTicket> generateBigTicket(String description) {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, datacampOffers -> {
            DatacampOffer datacampOffer = datacampOffers.get(0);
            datacampOffer.setData(new DcpOfferBuilder(datacampOffer.getBusinessId(), datacampOffer.getOfferId())
                    .withVendor("производитель-1")
                    .withDescription(description)
                    .build());

        });
        gcSkuTickets.forEach(gcSkuTicket -> {
            gcSkuTicket.setName(OFFER_TITLE);
            gcSkuTicket.setShopSku(gcSkuTicket.getDatacampOffer().getIdentifiers().getOfferId());
        });
        gcSkuTicketDao.update(gcSkuTickets);
        return gcSkuTickets;
    }

    private List<GcSkuTicket> generateBigTicket(long existingModelId) {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, datacampOffers -> {
            DatacampOffer datacampOffer = datacampOffers.get(0);
            datacampOffer.setData(new DcpOfferBuilder(datacampOffer.getBusinessId(), datacampOffer.getOfferId())
                    .withVendor("производитель-1")
                    .withDescription("some description")
                    .build());

        });
        gcSkuTickets.forEach(gcSkuTicket -> {
            gcSkuTicket.setName(OFFER_TITLE);
            gcSkuTicket.setShopSku(gcSkuTicket.getDatacampOffer().getIdentifiers().getOfferId());
            gcSkuTicket.setExistingMboPskuId(existingModelId);
        });
        gcSkuTicketDao.update(gcSkuTickets);
        return gcSkuTickets;
    }
}
