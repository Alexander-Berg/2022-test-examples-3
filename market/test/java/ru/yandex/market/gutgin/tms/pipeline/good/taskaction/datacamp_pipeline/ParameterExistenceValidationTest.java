package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMeta;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.Categories;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataForSizeParametersMock;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;
import static ru.yandex.market.ir.excel.generator.param.MainParamCreator.DATACAMP_GROUP_ID_NAME;
import static ru.yandex.market.robot.db.ParameterValueComposer.VENDOR_ID;

public class ParameterExistenceValidationTest extends DBDcpStateGenerator {

    private ParameterExistenceValidation parameterExistenceValidation;
    private ModelStorageHelper modelStorageHelper;

    private static final Long EXISTING_SKU_ID = 1L;
    private static final Long OWNER_ID = 1L;
    private static final Long ANOTHER_OWNER_ID = 2L;

    @Before
    public void setup() {
        modelStorageHelper = mock(ModelStorageHelper.class);
    }


    @Test
    public void whenAllMandatoryParamsFilledShouldBeOk() {
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        categoryDataKnowledge.addCategoryData(CATEGORY_ID, CategoryData.build(CATEGORY));

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                DataCampOfferContent.ProcessedSpecification.Builder actualBuilder =
                    builder.getContentBuilder().getPartnerBuilder().getActualBuilder();
                actualBuilder
                    .setTitle(DataCampOfferMeta.StringValue.newBuilder()
                        .setValue("Title")
                        .build())
                    .setDescription(DataCampOfferMeta.StringValue.newBuilder()
                        .setValue("Description")
                        .build())
                    .setVendor(DataCampOfferMeta.StringValue.newBuilder()
                        .setValue("Vendor")
                        .build());
            });
        });
        GcSkuTicket ticket = gcSkuTickets.get(0);

        parameterExistenceValidation = new ParameterExistenceValidation(gcSkuValidationDao,
                gcSkuTicketDao, categoryDataKnowledge, null, null, true);
        ProcessTaskResult<List<TicketValidationResult>> result =
                parameterExistenceValidation.validate(List.of(ticket), Map.of());

        assertThat(result.getResult().get(0).getValidationMessages()).isEmpty();
    }

    @Test
    public void whenMissingParameterShouldAddMessage() {
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        categoryDataKnowledge.addCategoryData(CATEGORY_ID, CategoryData.build(CATEGORY));

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                DataCampOfferContent.ProcessedSpecification.Builder actualBuilder =
                    builder.getContentBuilder().getPartnerBuilder().getActualBuilder();
                actualBuilder
                    .setTitle(DataCampOfferMeta.StringValue.newBuilder()
                        .setValue("Title")
                        .build());
            });
        });
        GcSkuTicket ticket = gcSkuTickets.get(0);

        parameterExistenceValidation = new ParameterExistenceValidation(gcSkuValidationDao, gcSkuTicketDao,
                categoryDataKnowledge, null, null, true);
        ProcessTaskResult<List<TicketValidationResult>> result =
                parameterExistenceValidation.validate(List.of(ticket), Map.of());
        List<MessageInfo> messages = result.getResult().get(0).getValidationMessages();
        assertThat(messages).extracting(MessageInfo::getCode)
            .containsOnly("ir.partner_content.dcp.validation.absent_mandatory_parameter")
            .hasSize(2);

        assertThat(messages).extracting(messageInfo -> messageInfo.getParams().get("paramId"))
            .containsExactlyInAnyOrder(MainParamCreator.DESCRIPTION_ID, VENDOR_ID);
    }

    @Test
    public void messageOnTicketWithoutGroupIdInFashionCategory() {
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        categoryDataKnowledge.addCategoryData(CATEGORY_ID, new CategoryDataForSizeParametersMock(
                new HashMap<Long, Pair<Long, Long>>() {
                    {
                        put(100L, Pair.of(101L, 102L));
                    }
                },
                MboParameters.Parameter.newBuilder()
                        .setId(100L)
                        .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                        .buildPartial()
                )
        );

        List<GcSkuTicket> ticketWithoutGroupId = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                DataCampOfferContent.ProcessedSpecification.Builder actualBuilder =
                        builder.getContentBuilder().getPartnerBuilder().getActualBuilder();
                actualBuilder
                        .setTitle(DataCampOfferMeta.StringValue.newBuilder()
                                .setValue("Title")
                                .build())
                        .setDescription(DataCampOfferMeta.StringValue.newBuilder()
                                .setValue("Description")
                                .build())
                        .setVendor(DataCampOfferMeta.StringValue.newBuilder()
                                .setValue("Vendor")
                                .build());
                builder.getContentBuilder().getPartnerBuilder().getOriginalBuilder()
                        .setGroupId(DataCampOfferMeta.Ui32Value.newBuilder().build())//group_id without value
                        .build();
            });
        });


        GcSkuTicket ticket = ticketWithoutGroupId.get(0);

        parameterExistenceValidation = new ParameterExistenceValidation(gcSkuValidationDao, gcSkuTicketDao,
                categoryDataKnowledge, null, null, true);
        ProcessTaskResult<List<TicketValidationResult>> result =
                parameterExistenceValidation.validate(List.of(ticket), Map.of());
        List<MessageInfo> messages = result.getResult().get(0).getValidationMessages();

        assertThat(messages).hasSize(1);
        assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.validation.absent_mandatory_parameter");
        assertThat(messages).extracting(messageInfo -> messageInfo.getParams().get("paramId")).containsOnly(-1L);
        assertThat(messages).extracting(messageInfo -> messageInfo.getParams().get("paramName"))
                .containsOnly(DATACAMP_GROUP_ID_NAME);
    }

    @Test
    public void messageOnTicketWithoutGroupIdInJewelryCategory() {
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        Long categoryId = Categories.JEWELRY_CATEGORIES.stream().findAny().get();
        MboParameters.Category category = MboParameters.Category.newBuilder()
                .setHid(categoryId)
                .addName(MboParameters.Word.newBuilder().setName("Category " + categoryId).setLangId(225))
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(VENDOR_ID).setXslName(ParameterValueComposer.VENDOR)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .addName(MboParameters.Word.newBuilder().setLangId(225).setName("производитель"))
                        .addOption(MboParameters.Option.newBuilder().addName(
                                        MboParameters.Word.newBuilder().setLangId(225).setName("производитель-1"))
                                .setId(1000)
                                .build())
                        .setMandatoryForPartner(true)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.NAME_ID).setXslName(ParameterValueComposer.NAME)
                        .setValueType(MboParameters.ValueType.STRING)
                        .setMandatoryForPartner(true)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.BARCODE_ID).setXslName(ParameterValueComposer.BARCODE)
                        .setValueType(MboParameters.ValueType.STRING)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.VENDOR_CODE_ID).setXslName(ParameterValueComposer.VENDOR_CODE)
                        .setValueType(MboParameters.ValueType.STRING)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.ALIASES_ID).setXslName(ParameterValueComposer.ALIASES)
                        .setValueType(MboParameters.ValueType.STRING))
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(DESCRIPTION_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .setXslName(DESCRIPTION_NAME)
                        .setMandatoryForPartner(true)
                )
                .build();
        categoryDataKnowledge.addCategoryData(categoryId, CategoryData.build(category));

        List<GcSkuTicket> ticketWithoutGroupId = generateDBDcpInitialState(1, offers -> {
            initOffer(categoryId, offers.get(0), builder -> {
                DataCampOfferContent.ProcessedSpecification.Builder actualBuilder =
                        builder.getContentBuilder().getPartnerBuilder().getActualBuilder();
                actualBuilder
                        .setTitle(DataCampOfferMeta.StringValue.newBuilder()
                                .setValue("Title")
                                .build())
                        .setDescription(DataCampOfferMeta.StringValue.newBuilder()
                                .setValue("Description")
                                .build())
                        .setVendor(DataCampOfferMeta.StringValue.newBuilder()
                                .setValue("Vendor")
                                .build());
                builder.getContentBuilder().getPartnerBuilder().getOriginalBuilder()
                        .setGroupId(DataCampOfferMeta.Ui32Value.newBuilder().build())//group_id without value
                        .build();
            });
        });


        GcSkuTicket ticket = ticketWithoutGroupId.get(0);
        ticket.setCategoryId(categoryId);


        parameterExistenceValidation = new ParameterExistenceValidation(gcSkuValidationDao, gcSkuTicketDao,
                categoryDataKnowledge, null, null, true);
        ProcessTaskResult<List<TicketValidationResult>> result =
                parameterExistenceValidation.validate(List.of(ticket), Map.of());
        List<MessageInfo> messages = result.getResult().get(0).getValidationMessages();

        assertThat(messages).hasSize(1);
        assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.validation.absent_mandatory_parameter");
        assertThat(messages).extracting(messageInfo -> messageInfo.getParams().get("paramId")).containsOnly(-1L);
        assertThat(messages).extracting(messageInfo -> messageInfo.getParams().get("paramName"))
                .containsOnly(DATACAMP_GROUP_ID_NAME);
    }

    @Test
    public void whenMissingParameterIsBlockedForModificationThenShouldNotAddMessage() {
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        categoryDataKnowledge.addCategoryData(CATEGORY_ID, CategoryData.build(CATEGORY));

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                DataCampOfferContent.ProcessedSpecification.Builder actualBuilder =
                        builder.getContentBuilder().getPartnerBuilder().getActualBuilder();
                actualBuilder
                        .setTitle(DataCampOfferMeta.StringValue.newBuilder()
                                .setValue("Title")
                                .build());
            });
        });
        GcSkuTicket ticket = gcSkuTickets.get(0);
        ticket.setType(GcSkuTicketType.CSKU);
        ticket.setExistingMboPskuId(EXISTING_SKU_ID);
        ticket.setPartnerShopId(OWNER_ID.intValue());
        gcSkuTicketDao.update(ticket);

        when(modelStorageHelper.findModelHierarchy(Set.of(EXISTING_SKU_ID))).thenReturn(Map.of(
                EXISTING_SKU_ID, ModelStorage.Model.newBuilder().setId(EXISTING_SKU_ID)
                        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                                .setParamId(DESCRIPTION_ID)
                                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("Description").build())
                                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                                .build()
                        )
                        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                                .setParamId(VENDOR_ID)
                                .setOwnerId(ANOTHER_OWNER_ID)
                                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("Vendor").build())
                                .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                                .build()
                        )
                        .build()
        ));

        parameterExistenceValidation = new ParameterExistenceValidation(gcSkuValidationDao, gcSkuTicketDao,
                categoryDataKnowledge, new Judge(), modelStorageHelper, false);
        ProcessTaskResult<List<TicketValidationResult>> result =
                parameterExistenceValidation.validate(List.of(ticket), Map.of());
        List<MessageInfo> messages = result.getResult().get(0).getValidationMessages();
        assertThat(messages).isEmpty();
    }
}
