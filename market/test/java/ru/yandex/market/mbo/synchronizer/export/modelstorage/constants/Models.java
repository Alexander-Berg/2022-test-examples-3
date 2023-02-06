package ru.yandex.market.mbo.synchronizer.export.modelstorage.constants;

import java.util.Date;
import java.util.List;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.modelstorage.pipe.IsPartnerParamPipePart;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class Models {
    public static final int UID = 20;
    public static final long EXPORT_TIME = System.currentTimeMillis();

    public static final ModelStorage.Model M1 = ModelStorage.Model.newBuilder()
        .setId(1).setCategoryId(1).setVendorId(1).addTitles(title("M1"))
        .setCurrentType(CommonModel.Source.GURU.name()).setPublished(true)
        .build();

    public static final ModelStorage.Model M_WITH_LINE_BREAK_IN_TITLES = ModelStorage.Model.newBuilder()
        .setId(1).setCategoryId(1).setVendorId(1).addTitles(title("some \n title"))
        .setCurrentType(CommonModel.Source.GURU.name()).setPublished(true)
        .addAliases(ModelStorage.LocalizedString.newBuilder().setValue("some \n alias").build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setXslName(XslNames.NAME)
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("name parameter \n value").build())
            .build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setXslName(XslNames.ALIASES)
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("aliases parameter \n value").build())
            .build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setXslName(XslNames.SEARCH_ALIASES)
            .addStrValue(ModelStorage.LocalizedString.newBuilder()
                .setValue("search aliases parameter \n value").build())
            .build())
        .build();

    public static final ModelStorage.Model M1_ENRICHED = M1.toBuilder()
        .clearTitles().addTitles(title(""))
        .setTitleWithoutVendor(title(""))
        .setGroupSize(2)
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(1)
        .build();

    public static final ModelStorage.Model SKU1_1 = ModelStorage.Model.newBuilder()
        .setId(2).setCategoryId(M1.getCategoryId()).setVendorId(M1.getVendorId()).addTitles(title("SKU1_1"))
        .setCurrentType(CommonModel.Source.SKU.name()).setPublished(true)
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(M1.getCategoryId())
            .setId(M1.getId())
            .build())
        .build();
    public static final ModelStorage.Model SKU1_1_ENRICHED = SKU1_1.toBuilder()
        .clearTitles().addTitles(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(1)
        .build();

    public static final ModelStorage.Model SKU1_2 = ModelStorage.Model.newBuilder(SKU1_1)
        .setId(3).setCategoryId(M1.getCategoryId()).setVendorId(M1.getVendorId()).addTitles(title("SKU1_2"))
        .setCurrentType(CommonModel.Source.SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(M1.getCategoryId())
            .setId(M1.getId())
            .build())
        .build();
    public static final ModelStorage.Model SKU1_2_ENRICHED = SKU1_2.toBuilder()
        .clearTitles().addTitles(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(1)
        .build();

    public static final ModelStorage.Model MODIF1 = ModelStorage.Model.newBuilder()
        .setId(5).setCategoryId(M1.getCategoryId()).setVendorId(M1.getVendorId()).addTitles(title("MODIF1"))
        .setCurrentType(CommonModel.Source.GURU.name()).setPublished(true)
        .setParentId(M1.getId())
        .build();
    public static final ModelStorage.Model MODIF1_ENRICHED = MODIF1.toBuilder()
        .clearTitles().addTitles(title(""))
        .setTitleWithoutVendor(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(1)
        .build();

    public static final ModelStorage.Model SKU_MODIF_11 = ModelStorage.Model.newBuilder()
        .addTitles(title("SKU_MODIF_11"))
        .setId(6).setCategoryId(MODIF1.getCategoryId()).setVendorId(MODIF1.getVendorId())
        .setCurrentType(CommonModel.Source.SKU.name()).setPublished(true)
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(MODIF1.getCategoryId())
            .setId(MODIF1.getId())
            .build())
        .build();
    public static final ModelStorage.Model SKU_MODIF_11_ENRICHED = SKU_MODIF_11.toBuilder()
        .clearTitles().addTitles(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(1)
        .build();

    public static final ModelStorage.Model SKU_MODIF_12 = ModelStorage.Model.newBuilder()
        .addTitles(title("SKU_MODIF_12"))
        .setId(7).setCategoryId(MODIF1.getCategoryId()).setVendorId(MODIF1.getVendorId())
        .setCurrentType(CommonModel.Source.SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(MODIF1.getCategoryId())
            .setId(MODIF1.getId())
            .build())
        .build();
    public static final ModelStorage.Model SKU_MODIF_12_ENRICHED = SKU_MODIF_12.toBuilder()
        .clearTitles().addTitles(title(""))
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(false)
        .setGroupModelId(1)
        .build();

    public static final ModelStorage.Model MODIF2 = ModelStorage.Model.newBuilder()
        .setId(8).setCategoryId(M1.getCategoryId()).setVendorId(MODIF1.getVendorId()).addTitles(title("MODIF2"))
        .setCurrentType(CommonModel.Source.GURU.name()).setPublished(false)
        .setParentId(M1.getId())
        .build();
    public static final ModelStorage.Model MODIF2_ENRICHED = MODIF2.toBuilder()
        .clearTitles().addTitles(title(""))
        .setTitleWithoutVendor(title(""))
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(false)
        .setGroupModelId(1)
        .build();

    public static final ModelStorage.Model SKU_MODIF_2 = ModelStorage.Model.newBuilder()
        .addTitles(title("SKU_MODIF_2"))
        .setId(9).setCategoryId(MODIF2.getCategoryId()).setVendorId(MODIF2.getVendorId())
        .setCurrentType(CommonModel.Source.SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(MODIF2.getCategoryId())
            .setId(MODIF2.getId())
            .build())
        .build();
    public static final ModelStorage.Model SKU_MODIF_2_ENRICHED = SKU_MODIF_2.toBuilder()
        .clearTitles().addTitles(title(""))
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(false)
        .setGroupModelId(1)
        .build();

    public static final ModelStorage.Model M2 = ModelStorage.Model.newBuilder()
        .setId(10).setCategoryId(2).setVendorId(2).addTitles(title("M2"))
        .setCurrentType(CommonModel.Source.GURU.name())
        .build();
    public static final ModelStorage.Model M2_ENRICHED = M2.toBuilder()
        .clearTitles().addTitles(title(""))
        .setTitleWithoutVendor(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(false)
        .setGroupModelId(10)
        .build();

    public static final ModelStorage.Model M3 = ModelStorage.Model.newBuilder()
        .setId(11).setCategoryId(1).setVendorId(1).addTitles(title("M3"))
        .setCurrentType(CommonModel.Source.GURU.name()).setPublished(true)
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setXslName(XslNames.IS_SKU)
            .setParamId(100)
            .setBoolValue(true)
            .setOptionId(1))
        .build();
    public static final ModelStorage.Model M3_ENRICHED = M3.toBuilder()
        .clearTitles().addTitles(title(""))
        .setTitleWithoutVendor(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .addRelations(ModelStorage.Relation.newBuilder()
            .setId(M3.getId()).setCategoryId(M3.getCategoryId()).setType(ModelStorage.RelationType.SKU_MODEL))
        .setGroupModelId(11)
        .build();
    public static final ModelStorage.Model M3_SKU_ENRICHED = M3.toBuilder()
        .clearTitles().addTitles(title(""))
        .setCurrentType(CommonModel.Source.SKU.name())
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .addRelations(ModelStorage.Relation.newBuilder()
            .setId(M3.getId()).setCategoryId(M3.getCategoryId()).setType(ModelStorage.RelationType.SKU_PARENT_MODEL))
        .setGroupModelId(11)
        .build();

    public static final ModelStorage.Model C1 = ModelStorage.Model.newBuilder()
        .setId(12).setCategoryId(1).setVendorId(1).addTitles(title("C1"))
        .setCurrentType(CommonModel.Source.CLUSTER.name()).setPublished(true)
        .setDeleted(true).setDeletedDate(new Date().getTime() - 60L * 60L * 1000L) // one hour before
        .build();
    public static final ModelStorage.Model C1_ENRICHED = C1.toBuilder()
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(false)
        .setGroupModelId(12)
        .build();

    public static final ModelStorage.Model PARTNER1 = ModelStorage.Model.newBuilder()
        .setId(13).setCategoryId(1).setVendorId(1).addTitles(title("PARTNER1"))
        .setCurrentType(CommonModel.Source.PARTNER.name()).setPublished(true)
        .build();
    public static final ModelStorage.Model PARTNER1_ENRICHED = enrichPartnerModel(PARTNER1.toBuilder()
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(13)
        .build());

    public static final ModelStorage.Model PARTNER_SKU1_1 = ModelStorage.Model.newBuilder()
        .setId(14).setCategoryId(PARTNER1.getCategoryId()).setVendorId(PARTNER1.getVendorId())
        .addTitles(title("PARTNER_SKU1_1"))
        .setCurrentType(CommonModel.Source.PARTNER_SKU.name()).setPublished(true)
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(PARTNER1.getCategoryId())
            .setId(PARTNER1.getId())
            .build())
        .build();
    public static final ModelStorage.Model PARTNER_SKU1_1_ENRICHED = enrichPartnerModel(PARTNER_SKU1_1.toBuilder()
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(13)
        .build());

    public static final ModelStorage.Model PARTNER_SKU1_2 = ModelStorage.Model.newBuilder()
        .setId(15).setCategoryId(PARTNER1.getCategoryId()).setVendorId(PARTNER1.getVendorId())
        .addTitles(title("PARTNER_SKU1_2"))
        .setCurrentType(CommonModel.Source.PARTNER_SKU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(PARTNER1.getCategoryId())
            .setId(PARTNER1.getId())
            .build())
        .build();
    public static final ModelStorage.Model PARTNER_SKU1_2_ENRICHED = enrichPartnerModel(PARTNER_SKU1_2.toBuilder()
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(false)
        .setGroupModelId(13)
        .build());

    public static final ModelStorage.Model PARTNER2 = ModelStorage.Model.newBuilder()
        .setId(16).setCategoryId(1).setVendorId(1).addTitles(title("PARTNER2"))
        .setCurrentType(CommonModel.Source.PARTNER.name()).setPublished(true)
        .setDeleted(true).setDeletedDate(new Date().getTime() - 60L * 60L * 1000L) // one hour before
        .build();
    public static final ModelStorage.Model PARTNER2_ENRICHED = enrichPartnerModel(PARTNER2.toBuilder()
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(false)
        .build());

    public static final ModelStorage.Model PARTNER3 = ModelStorage.Model.newBuilder()
        .setId(17).setCategoryId(1).setVendorId(KnownIds.NOT_DEFINED_GLOBAL_VENDOR).addTitles(title("PARTNER3"))
        .setCurrentType(CommonModel.Source.PARTNER.name()).setPublished(true).setPublishedOnBlueMarket(true)
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(1).setXslName(XslNames.RAW_VENDOR).setValueType(MboParameters.ValueType.STRING)
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setIsoCode("ru").setValue("draft vendor"))
        )
        .build();
    public static final ModelStorage.Model PARTNER3_ENRICHED = enrichPartnerModel(PARTNER3.toBuilder()
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(17)
        .build());

    public static final ModelStorage.Model PARTNER_SKU3_1 = ModelStorage.Model.newBuilder()
        .setId(18).setCategoryId(PARTNER1.getCategoryId()).setVendorId(KnownIds.NOT_DEFINED_GLOBAL_VENDOR)
        .addTitles(title("PARTNER_SKU3_1"))
        .setCurrentType(CommonModel.Source.PARTNER_SKU.name()).setPublished(true).setPublishedOnBlueMarket(true)
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(1).setXslName(XslNames.RAW_VENDOR).setValueType(MboParameters.ValueType.STRING)
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setIsoCode("ru").setValue("draft vendor"))
        )
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(PARTNER3.getCategoryId())
            .setId(PARTNER3.getId())
            .build())
        .build();
    public static final ModelStorage.Model PARTNER_SKU3_1_ENRICHED = enrichPartnerModel(PARTNER_SKU3_1.toBuilder()
        .setPublishedOnMarket(false)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(17)
        .build());


    public static final ModelStorage.Model EXP_M1 = ModelStorage.Model.newBuilder()
        .setExperimentFlag("testExp")
        .setId(19).setCategoryId(1).setVendorId(1).addTitles(title("Exp M1"))
        .setCurrentType(CommonModel.Source.EXPERIMENTAL.name()).setPublished(true)
        .setSourceType(CommonModel.Source.GURU.name())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.EXPERIMENTAL_MODEL)
            .setCategoryId(M1.getCategoryId())
            .setId(M1.getId())
            .build())
        .build();

    public static final ModelStorage.Model EXP_M1_ENRICHED = EXP_M1.toBuilder()
        .clearTitles().addTitles(title(""))
        .setTitleWithoutVendor(title(""))
        .setGroupSize(1)
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(19)
        .build();

    public static final ModelStorage.Model EXP_SKU1_1 = ModelStorage.Model.newBuilder()
        .setExperimentFlag("testExp")
        .setId(20).setCategoryId(M1.getCategoryId()).setVendorId(M1.getVendorId()).addTitles(title("Exp SKU1_1"))
        .setSourceType(CommonModel.Source.SKU.name())
        .setCurrentType(CommonModel.Source.EXPERIMENTAL_SKU.name()).setPublished(true)
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.EXPERIMENTAL_MODEL)
            .setCategoryId(M1.getCategoryId())
            .setId(M1.getId())
            .build())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(EXP_M1.getCategoryId())
            .setId(EXP_M1.getId())
            .build())
        .build();

    public static final ModelStorage.Model EXP_M2 = ModelStorage.Model.newBuilder()
        .setExperimentFlag("testExp")
        .setId(21).setCategoryId(1).setVendorId(1).addTitles(title("Exp M2"))
        .setCurrentType(CommonModel.Source.EXPERIMENTAL.name()).setPublished(true)
        .setSourceType(CommonModel.Source.GURU.name())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setXslName(XslNames.IS_SKU)
            .setParamId(100)
            .setBoolValue(true)
            .setOptionId(1))
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.EXPERIMENTAL_MODEL)
            .setCategoryId(M2.getCategoryId())
            .setId(M2.getId())
            .build())
        .build();

    public static final ModelStorage.Model EXP_M2_ENRICHED = EXP_M2.toBuilder()
        .clearTitles().addTitles(title(""))
        .setTitleWithoutVendor(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .addRelations(ModelStorage.Relation.newBuilder()
            .setId(EXP_M2.getId()).setCategoryId(EXP_M2.getCategoryId()).setType(ModelStorage.RelationType.SKU_MODEL))
        .setGroupModelId(EXP_M2.getId())
        .build();

    public static final ModelStorage.Model EXP_M2_SKU_ENRICHED = EXP_M2.toBuilder()
        .clearTitles().addTitles(title(""))
        .setCurrentType(CommonModel.Source.EXPERIMENTAL_SKU.name())
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .addRelations(ModelStorage.Relation.newBuilder()
            .setId(EXP_M2.getId()).setCategoryId(EXP_M2.getCategoryId())
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL))
        .setGroupModelId(EXP_M2.getId())
        .build();

    public static final ModelStorage.Model EXP_SKU1_1_ENRICHED = EXP_SKU1_1.toBuilder()
        .clearTitles().addTitles(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(19)
        .build();

    public static final ModelStorage.Model EXP_MODIF1 = ModelStorage.Model.newBuilder()
        .setId(21).setCategoryId(M1.getCategoryId()).setVendorId(M1.getVendorId()).addTitles(title("Exp MODIF1"))
        .setCurrentType(CommonModel.Source.GURU.name()).setPublished(true)
        .setParentId(EXP_M1.getId())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.EXPERIMENTAL_MODEL)
            .setCategoryId(MODIF1.getCategoryId())
            .setId(MODIF1.getId())
            .build())
        .build();
    public static final ModelStorage.Model EXP_MODIF1_ENRICHED = EXP_MODIF1.toBuilder()
        .clearTitles().addTitles(title(""))
        .setTitleWithoutVendor(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(19)
        .build();

    public static final ModelStorage.Model EXP_SKU_MODIF_11 = ModelStorage.Model.newBuilder()
        .addTitles(title("Exp SKU_MODIF_11"))
        .setId(22).setCategoryId(MODIF1.getCategoryId()).setVendorId(MODIF1.getVendorId())
        .setCurrentType(CommonModel.Source.SKU.name()).setPublished(true)
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setCategoryId(EXP_MODIF1.getCategoryId())
            .setId(EXP_MODIF1.getId())
            .build())
        .addRelations(ModelStorage.Relation.newBuilder()
            .setType(ModelStorage.RelationType.EXPERIMENTAL_MODEL)
            .setCategoryId(SKU_MODIF_11.getCategoryId())
            .setId(SKU_MODIF_11.getId())
            .build())
        .build();
    public static final ModelStorage.Model EXP_SKU_MODIF_11_ENRICHED = EXP_SKU_MODIF_11.toBuilder()
        .clearTitles().addTitles(title(""))
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(true)
        .setGroupModelId(19)
        .build();

    public static final ModelStorage.Model DUMMY_GURU = ModelStorage.Model.newBuilder()
        .setId(23).setCategoryId(1).setVendorId(1).addTitles(title("Dummy Guru"))
        .setCurrentType(CommonModel.Source.GURU_DUMMY.name())
        .setSourceType(CommonModel.Source.GURU_DUMMY.name())
        .setPublished(true)
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(1).setXslName(XslNames.USE_IN_FILTERS).setValueType(MboParameters.ValueType.STRING)
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setIsoCode("ru").setValue("draft vendor"))
        )
        .build();

    public static final ModelStorage.Model DUMMY_GURU_ENRICHED = DUMMY_GURU.toBuilder()
        .clearTitles().addTitles(title(""))
        .setTitleWithoutVendor(title(""))
        .setCurrentType(CommonModel.Source.GURU.name())
        .setPublishedOnMarket(true)
        .setPublishedOnBlueMarket(false)
        .setGroupModelId(23)
        .build();

    private Models() {
    }

    public static ModelStorage.Model removeIsPartnerModificationDate(ModelStorage.Model model) {
        ModelStorage.Model.Builder builder = model.toBuilder();
        List<ModelStorage.ParameterValue> values = builder.getParameterValuesList();
        builder.clearParameterValues();
        values.stream()
            .map(pv -> {
                ModelStorage.ParameterValue.Builder pvb = pv.toBuilder();
                if (pv.getParamId() == Categories.IS_PARTNER_PARAM.getId()
                    && pv.getOptionId() == Categories.IS_PARTNER_TRUE_OPTION.getId()) {
                    pvb.setModificationDate(0L);
                }
                return pvb;
            })
            .forEach(builder::addParameterValues);
        return builder.build();
    }

    public static ModelStorage.LocalizedString.Builder title(String title) {
        return ModelStorage.LocalizedString.newBuilder().setIsoCode(Language.RUSSIAN.getIsoCode()).setValue(title);
    }

    private static ModelStorage.Model enrichPartnerModel(ModelStorage.Model model) {
        ModelStorage.Model.Builder builder = model.toBuilder();
        ModelStorage.ParameterValue.Builder isPartnerTrueValue = IsPartnerParamPipePart.createIsPartnerTrueValue(
            Categories.IS_PARTNER_PARAM, Categories.IS_PARTNER_TRUE_OPTION.getId(), UID);
        isPartnerTrueValue.setModificationDate(0L);
        builder.addParameterValues(isPartnerTrueValue);
        return builder.build();
    }
}
