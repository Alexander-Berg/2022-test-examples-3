package ru.yandex.market.ir.autogeneration_api.http.service.mvc.controller;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import ru.yandex.market.extractor.ExtractorConfig;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryModelsHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean.ContentDataRules;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean.ContentReceivingOption;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean.DcpCategoryPojo;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean.SpecialParams;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.ir.excel.generator.XslInfo;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.csku.judge.Action;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.mocks.CategoryParametersFormParserMock;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DcpApiControllerTest {

    private static final Long PARENT_ID = 1L;
    private static final Long SKU_ID = 123L;
    private static final Long SKU_ID_1 = 124L;
    private static final Long CATEGORY_ID = 417357L;
    private static final Long BUSINESS_ID = 152734L;
    private static final Long IS_GREEN_HOW_PARAM_ID = 4749254L;
    private static final Long BASIC_CATEGORY_ID = 136162677L;

    private static final Function<String, MboParameters.Word> wordMaker = s -> MboParameters.Word.newBuilder()
            .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE)
            .setName(s)
            .build();

    private static final Function<String, MboParameters.Word> englishWordMaker = s -> MboParameters.Word.newBuilder()
            .setLangId(ExtractorConfig.Language.ENGLISH_VALUE)
            .setName(s)
            .build();

    private static BeanFactory createSpringContext() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(Configuration.class);
        applicationContext.refresh();
        return applicationContext;
    }

    @Test
    public void testBasic() {
        BeanFactory springContext = getContextForCategory(true);

        DcpApiController dcpApiController = springContext.getBean(DcpApiController.class);
        DcpCategoryPojo dcpCategoryPojo = dcpApiController.getCategory(BASIC_CATEGORY_ID, null, "");
        assertThat(dcpCategoryPojo.getContentReceivingOption()).isEqualTo(ContentReceivingOption.AVAILABLE);
        assertThat(dcpCategoryPojo.getSpecialParams().size()).isEqualTo(6);
        assertThat(dcpCategoryPojo).isNotNull();
        assertThat(dcpCategoryPojo.getParameterList())
                .extracting(DcpCategoryPojo.Parameter::getParameterId)
                .containsExactlyInAnyOrder(
                        ParameterValueComposer.NAME_ID, ParameterValueComposer.VENDOR_ID,
                        IS_GREEN_HOW_PARAM_ID, ParameterValueComposer.CLUSTERIZER_USER_ID
                );
        assertThat(dcpCategoryPojo.getParameterList())
                .filteredOn(el -> el.getParameterId() == ParameterValueComposer.CLUSTERIZER_USER_ID)
                .flatExtracting(el -> Arrays.asList(el.getMinValue(), el.getMaxValue()))
                .anyMatch(d -> Math.abs(d) < 0.0001)
                .anyMatch(d -> Math.abs(d - Integer.MAX_VALUE) < 0.0001);
    }

    @Test
    public void testGetCategoryNotLeaf() {
        BeanFactory springContext = getContextForCategory(false);
        DcpApiController dcpApiController = springContext.getBean(DcpApiController.class);
        DcpCategoryPojo dcpCategoryPojo = dcpApiController.getCategory(BASIC_CATEGORY_ID, null, "");
        assertThat(dcpCategoryPojo.getCategoryId()).isEqualTo(String.valueOf(BASIC_CATEGORY_ID));
        assertThat(dcpCategoryPojo.getContentReceivingOption()).isEqualTo(ContentReceivingOption.CATEGORY_IS_NOT_LEAF);
    }

    @Test
    public void testGetCategoryWithVendorName() {
        long sizeParamId = 25749770L;
        long vendor1 = 1L;
        List<Long> vendor1SizeOptions = List.of(1L, 2L);
        Map<Pair<Long, String>, List<Long>> vendorOptions = Map.of(
                Pair.of(vendor1, "vendor-" + vendor1), vendor1SizeOptions
        );
        BeanFactory springContext = getContextForCategoryWithSizes(sizeParamId, vendorOptions);
        DcpApiController dcpApiController = springContext.getBean(DcpApiController.class);

        Function<DcpCategoryPojo, List<DcpCategoryPojo.Option>> getOptionsFunc =
                dcpCategoryPojo -> dcpCategoryPojo.getParameterList().stream()
                        .filter(p -> p.getParameterId() == sizeParamId)
                        .findFirst()
                        .get()
                        .getOptions();

        DcpCategoryPojo defaultCategory = dcpApiController.getCategory(BASIC_CATEGORY_ID, null, "");
        assertThat(getOptionsFunc.apply(defaultCategory).size()).isEqualTo(0);

        DcpCategoryPojo category1 = dcpApiController.getCategory(BASIC_CATEGORY_ID, null, "vendor-" + vendor1);
        List<DcpCategoryPojo.Option> vendor1Options = getOptionsFunc.apply(category1);
        assertThat(vendor1Options.size()).isEqualTo(2);
        assertThat(
                vendor1Options.stream()
                        .map(DcpCategoryPojo.Option::getId)
                        .sorted()
                        .collect(Collectors.toList())
        ).isEqualTo(vendor1SizeOptions);
    }

    @Test
    public void testGetCategoryWithVendorId() {
        long sizeParamId = 25749770L;
        long vendor1 = 1L;
        long vendor2 = 2L;
        List<Long> vendor1SizeOptions = List.of(1L, 2L);
        List<Long> vendor2SizeOptions = List.of(3L, 4L);
        Map<Pair<Long, String>, List<Long>> vendorOptions = Map.of(
                Pair.of(vendor1, "vendor-" + vendor1), vendor1SizeOptions,
                Pair.of(vendor2, "vendor-" + vendor2), vendor2SizeOptions
        );
        BeanFactory springContext = getContextForCategoryWithSizes(sizeParamId, vendorOptions);
        DcpApiController dcpApiController = springContext.getBean(DcpApiController.class);

        Function<DcpCategoryPojo, List<DcpCategoryPojo.Option>> getOptionsFunc =
                dcpCategoryPojo -> dcpCategoryPojo.getParameterList().stream()
                        .filter(p -> p.getParameterId() == sizeParamId)
                        .findFirst()
                        .get()
                        .getOptions();

        DcpCategoryPojo defaultCategory = dcpApiController.getCategory(BASIC_CATEGORY_ID, "", "");
        assertThat(getOptionsFunc.apply(defaultCategory).size()).isEqualTo(0);

        DcpCategoryPojo category1 = dcpApiController.getCategory(BASIC_CATEGORY_ID, "" + vendor1, "");
        List<DcpCategoryPojo.Option> vendor1Options = getOptionsFunc.apply(category1);
        assertThat(vendor1Options.size()).isEqualTo(2);
        assertThat(
                vendor1Options.stream()
                        .map(DcpCategoryPojo.Option::getId)
                        .sorted()
                        .collect(Collectors.toList())
        ).isEqualTo(vendor1SizeOptions);


        DcpCategoryPojo category2 = dcpApiController.getCategory(BASIC_CATEGORY_ID, "" + vendor2, "");
        List<DcpCategoryPojo.Option> vendor2Options = getOptionsFunc.apply(category2);
        assertThat(vendor2Options.size()).isEqualTo(2);
        assertThat(
                vendor2Options.stream()
                        .map(DcpCategoryPojo.Option::getId)
                        .sorted()
                        .collect(Collectors.toList())
        ).isEqualTo(vendor2SizeOptions);
    }

    @Test
    public void testGetContentDataRules() {
        BeanFactory springContext = createSpringContext();
        DcpApiController dcpApiController = springContext.getBean(DcpApiController.class);

        ContentDataRules contentDataRules = dcpApiController.getContentDataRules(BUSINESS_ID, SKU_ID);

        assertThat(contentDataRules.getIsCsku()).isTrue();
        List<ContentDataRules.ParamRule> rules = contentDataRules.getParamsRules();
        assertThat(rules.size()).isEqualTo(11); //+6 специальных параметров

        assertThat(rules.get(0).getParamId()).isEqualTo(11);
        assertThat(rules.get(0).getAllowedAction()).isEqualTo(Action.MODIFY); //модельный параметр, один и тот же
        // владелец оффера и модели
        assertThat(rules.get(3).getParamId()).isEqualTo(3);
        assertThat(rules.get(3).getAllowedAction()).isEqualTo(Action.MODIFY); //такой же owner_id
        assertThat(rules.get(4).getParamId()).isEqualTo(1);
        assertThat(rules.get(4).getAllowedAction()).isEqualTo(Action.MODIFY); //чужой owner_id, запретов нет
        assertThat(rules.get(2).getParamId()).isEqualTo(2);
        assertThat(rules.get(2).getAllowedAction()).isEqualTo(Action.MODIFY); //нет owner_id, один и тот же владелец
        // оффера и карточки
        assertThat(rules.get(1).getParamId()).isEqualTo(4);
        assertThat(rules.get(1).getAllowedAction()).isEqualTo(Action.NONE); //заполнил оператор

        contentDataRules = dcpApiController.getContentDataRules(BUSINESS_ID + 1, SKU_ID_1);
        assertThat(contentDataRules.getIsCsku()).isFalse();
        rules = contentDataRules.getParamsRules();
        assertThat(rules.size()).isEqualTo(11);
        assertThat(rules.get(0).getParamId()).isEqualTo(11);
        assertThat(rules.get(0).getAllowedAction()).isEqualTo(Action.MODIFY); //модельный параметр, не совпадает
        // владелец оффера и модели
        assertThat(rules.get(3).getParamId()).isEqualTo(3);
        assertThat(rules.get(3).getAllowedAction()).isEqualTo(Action.MODIFY); //чужой owner_id
        assertThat(rules.get(2).getParamId()).isEqualTo(2);
        assertThat(rules.get(2).getAllowedAction()).isEqualTo(Action.MODIFY); //нет owner_id, не совпадает владелец
        // оффера и карточки
        assertThat(rules.get(4).getParamId()).isEqualTo(1);
        assertThat(rules.get(4).getAllowedAction()).isEqualTo(Action.MODIFY); //чужой owner_id
        assertThat(rules.get(1).getParamId()).isEqualTo(4);
        assertThat(rules.get(1).getAllowedAction()).isEqualTo(Action.MODIFY);

        assertThat(rules.get(5).getParamId()).isEqualTo(KnownParameters.NAME.getId());
        assertThat(rules.get(6).getParamId()).isEqualTo(KnownParameters.BARCODE.getId());
        assertThat(rules.get(7).getParamId()).isEqualTo(KnownParameters.VENDOR.getId());
        assertThat(rules.get(8).getParamId()).isEqualTo(KnownParameters.VENDOR_CODE.getId());
        assertThat(rules.get(9).getParamId()).isEqualTo(KnownParameters.DESCRIPTION.getId());
        assertThat(rules.get(10).getParamId()).isEqualTo(SpecialParams.PICTURES_ID);
    }

    private static class Configuration {
        @Bean
        CategoryParametersFormParserMock categoryParametersFormParser() {
            CategoryParametersFormParserMock categoryParametersFormParserMock = new CategoryParametersFormParserMock();
            return categoryParametersFormParserMock;
        }

        @Bean(name = "categoryDataKnowledgeCached")
        CategoryDataKnowledge categoryDataKnowledgeCached() {
            CategoryDataKnowledgeMock categoryDataKnowledgeMock = new CategoryDataKnowledgeMock();
            categoryDataKnowledgeMock.addCategoryData(CATEGORY_ID,
                    CategoryData.build(MboParameters.Category.newBuilder()
                            .addAllParameter(Arrays.asList(
                                    MboParameters.Parameter.newBuilder().setXslName("vendor").setId(11111).build(),
                                    MboParameters.Parameter.newBuilder().setXslName("param1").setId(1).setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL).build(),
                                    MboParameters.Parameter.newBuilder().setXslName("param2").setId(2).setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL).build(),
                                    MboParameters.Parameter.newBuilder().setXslName("param3").setId(3).setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL).build(),
                                    MboParameters.Parameter.newBuilder().setXslName("param11").setId(11).setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL).build(),
                                    MboParameters.Parameter.newBuilder().setXslName("param4").setId(4).setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL).build()
                            ))
                            .setHid(CATEGORY_ID)
                            .build()));
            return categoryDataKnowledgeMock;
        }

        @Bean(name = "categoryDataKnowledge")
        CategoryDataKnowledge categoryDataKnowledge() {
            return new CategoryDataKnowledgeMock();
        }


        @Bean
        DcpApiController dcpApiController(
                @Qualifier("categoryDataKnowledgeCached") CategoryDataKnowledge categoryDataKnowledge,
                CategoryParametersFormParser categoryParametersFormParser) {

            // Currently not used.
            ModelStorageServiceMock modelStorageService = new ModelStorageServiceMock();
            modelStorageService.putModel(ModelStorage.Model.newBuilder()
                    .setId(SKU_ID)
                    .setSupplierId(BUSINESS_ID)
                    .setCategoryId(CATEGORY_ID)
                    .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                    .addRelations(ModelStorage.Relation.newBuilder()
                            .setId(PARENT_ID)
                            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                            .setCategoryId(CATEGORY_ID).build())
                    .addAllParameterValues(Arrays.asList(
                            ModelStorage.ParameterValue.newBuilder()
                                    .setParamId(1)
                                    .setOwnerId(1)
                                    .build(),
                            ModelStorage.ParameterValue.newBuilder()
                                    .setParamId(2)
                                    .setOwnerId(BUSINESS_ID)
                                    .build(),
                            ModelStorage.ParameterValue.newBuilder()
                                    .setParamId(3)
                                    .build(),
                            ModelStorage.ParameterValue.newBuilder()
                                    .setParamId(4)
                                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                                    .build()
                    )));

            modelStorageService.putModel(ModelStorage.Model.newBuilder()
                    .setId(SKU_ID_1)
                    .setSupplierId(BUSINESS_ID)
                    .setCategoryId(CATEGORY_ID)
                    .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                    .addRelations(ModelStorage.Relation.newBuilder()
                            .setId(PARENT_ID)
                            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                            .setCategoryId(CATEGORY_ID).build())
                    .addAllParameterValues(Arrays.asList(
                            ModelStorage.ParameterValue.newBuilder()
                                    .setParamId(1)
                                    .setOwnerId(1)
                                    .build(),
                            ModelStorage.ParameterValue.newBuilder()
                                    .setParamId(2)
                                    .setOwnerId(BUSINESS_ID)
                                    .build(),
                            ModelStorage.ParameterValue.newBuilder()
                                    .setParamId(3)
                                    .build()
                    )));
            modelStorageService.putModel(
                    ModelStorage.Model.newBuilder()
                            .setId(PARENT_ID)
                            .setCategoryId(CATEGORY_ID)
                            .setSupplierId(BUSINESS_ID)
                            .setSourceType(ModelStorage.ModelType.PARTNER.name())
                            .setCurrentType(ModelStorage.ModelType.GURU.name())
                            .addAllParameterValues(Collections.singletonList(
                                    ModelStorage.ParameterValue.newBuilder()
                                            .setParamId(11)
                                            .build()
                            )));
            ModelStorageHelper modelStorageHelper = new ModelStorageHelper(modelStorageService, modelStorageService);
            CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(categoryDataKnowledge,
                    categoryParametersFormParser);

            CategoryModelsService categoryModelsService = Mockito.mock(CategoryModelsService.class);
            CategoryModelsHelper categoryModelsHelper = new CategoryModelsHelper(categoryModelsService);

            return new DcpApiController(categoryDataKnowledge, categoryInfoProducer, modelStorageHelper, categoryModelsHelper,
                    new Judge(),
                    "testing");
        }
    }

    private BeanFactory getContextForCategory(boolean isLeaf) {
        BeanFactory springContext = createSpringContext();

        MboParameters.Category category = MboParameters.Category
                .newBuilder()
                .addName(wordMaker.apply("Шланг"))
                .addName(englishWordMaker.apply("Hose"))
                .addFullName(wordMaker.apply("Садовые шланги и шлангообразное оборудование"))
                .setGuruId(42)
                .setLeaf(isLeaf)
                .addParameter(validNameParam().build())
                .addParameter(validVendorIdParam().build())
                .addParameter(
                        MboParameters.Parameter
                                .newBuilder()
                                .setId(ParameterValueComposer.CLUSTERIZER_USER_ID)
                                .setValueType(MboParameters.ValueType.NUMERIC)
                                .setMinValue(0)
                                .setMaxValue(Integer.MAX_VALUE)
                                .setMandatory(true)
                                .setParamType(MboParameters.ParameterLevel.OFFER_LEVEL)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                                .build()
                ).addParameter(
                        MboParameters.Parameter
                                .newBuilder()
                                .setId(IS_GREEN_HOW_PARAM_ID)
                                .addName(wordMaker.apply("Как именно зеленый"))
                                .setXslName("is_green_how")
                                .setDescription(
                                        "Для случая, если шланг зеленого цвета, " +
                                                "позволяет покупателю понять, он зеленый как"
                                )
                                .setService(false)
                                .setValueType(MboParameters.ValueType.ENUM)
                                .setMandatory(true)
                                .addOption(
                                        MboParameters.Option
                                                .newBuilder()
                                                .setId(1616L)
                                                .addName(wordMaker.apply("никак"))
                                                .build()
                                )
                                .addOption(
                                        MboParameters.Option
                                                .newBuilder()
                                                .setId(1617L)
                                                .addName(wordMaker.apply("только в длину"))
                                                .build()
                                )
                                .addOption(
                                        MboParameters.Option
                                                .newBuilder()
                                                .setId(1618L)
                                                .addName(wordMaker.apply("только в ширину"))
                                                .build()
                                )
                                .addOption(
                                        MboParameters.Option
                                                .newBuilder()
                                                .setId(1619L)
                                                .addName(wordMaker.apply("в длину и в ширину"))
                                                .build()
                                )
                                .build()
                ).build();

        springContext.getBean("categoryDataKnowledgeCached", CategoryDataKnowledgeMock.class).addCategoryData(
                BASIC_CATEGORY_ID, CategoryData.build(category)
        );
        springContext.getBean(CategoryParametersFormParserMock.class)
                .addCategoryAttrs(BASIC_CATEGORY_ID, new HashMap<String, XslInfo>() {{
                    put("name", new XslInfo(0, "Основные"));
                    put("is_green_how", new XslInfo(0, "Тоже важные"));
                }});

        return springContext;
    }

    /**
     * Initializes category data with size parameter <b>sizeParamId</b> and options from <b>vendorOptions</b>.
     * <br />
     * This size param additionally has 2 parameters in range [sizeParamId + 1, sizeParamId + 2]
     *
     * @param vendorOptions -
     */
    private BeanFactory getContextForCategoryWithSizes(long sizeParamId, Map<Pair<Long, String>, List<Long>> vendorOptions) {
        BeanFactory springContext = createSpringContext();
        long minSizeParamId = sizeParamId + 1;
        long maxSizeParamId = sizeParamId + 2;

        MboParameters.Parameter.Builder sizeParamBuilder = MboParameters.Parameter.newBuilder()
                .setId(sizeParamId)
                .setValueType(MboParameters.ValueType.ENUM)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .setXslName("size")
                .addName(wordMaker.apply("Размер"));

        MboSizeMeasures.SizeMeasureInfo.Builder sizeMeasuresBuilder = MboSizeMeasures.SizeMeasureInfo.newBuilder()
                .setSizeMeasure(
                        MboSizeMeasures.SizeMeasure.newBuilder()
                                .setId(1)
                                .setNumericParamId(2)
                                .setName("Размер")
                                .setUnitParamId(3)
                                .setValueParamId(sizeParamId)
                                .setMinNumericParamId(minSizeParamId)
                                .setMaxNumericParamId(maxSizeParamId)
                                .build()
                );

        MboParameters.Parameter.Builder vendorIdParamBuilder = validVendorIdParam();

        for (Map.Entry<Pair<Long, String>, List<Long>> entry : vendorOptions.entrySet()) {
            long vendorId = entry.getKey().getKey();
            String vendorName = entry.getKey().getValue();
            List<Long> options = entry.getValue();
            MboSizeMeasures.ScaleInfo.Builder scale = MboSizeMeasures.ScaleInfo.newBuilder()
                    .setVendorId(vendorId);

            vendorIdParamBuilder.addOption(
                    MboParameters.Option.newBuilder()
                            .setId(vendorId)
                            .addName(wordMaker.apply(vendorName))
                            .build()
            );

            for (Long option : options) {
                sizeParamBuilder.addOption(
                        MboParameters.Option.newBuilder()
                                .setId(option)
                                .addName(wordMaker.apply(vendorName + "-option-" + option))
                                .build()
                );
                scale.addSizeInfos(
                        MboSizeMeasures.SizeInfo.newBuilder()
                                .setSizeId(option)
                                .build()
                );
            }
            sizeMeasuresBuilder.addScales(scale.build());
        }

        MboParameters.Category category = MboParameters.Category
                .newBuilder()
                .addName(wordMaker.apply("Шланг"))
                .setGuruId(42)
                .setLeaf(true)
                .addParameter(validNameParam().build())
                .addParameter(vendorIdParamBuilder.build())
                .addParameter(sizeParamBuilder.build())
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(minSizeParamId)
                                .setValueType(MboParameters.ValueType.NUMERIC)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
                                .setXslName("size_min")
                                .addName(wordMaker.apply("Размер (мин)"))
                                .build()
                )
                .addParameter(
                        MboParameters.Parameter.newBuilder()
                                .setId(maxSizeParamId)
                                .setValueType(MboParameters.ValueType.NUMERIC)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
                                .setXslName("size_max")
                                .addName(wordMaker.apply("Размер (макс)"))
                                .build()
                )
                .build();

        List<MboSizeMeasures.GetSizeMeasuresInfoResponse.CategoryResponse> sizes = new ArrayList<>();
        sizes.add(
                MboSizeMeasures.GetSizeMeasuresInfoResponse.CategoryResponse.newBuilder()
                        .setCategoryId(category.getHid())
                        .addSizeMeasureInfos(sizeMeasuresBuilder.build())
                        .build()
        );

        springContext.getBean("categoryDataKnowledgeCached", CategoryDataKnowledgeMock.class).addCategoryData(
                BASIC_CATEGORY_ID, CategoryData.build(category, sizes)
        );
        return springContext;
    }

    /* Helper methods for creating domain objects */

    private static MboParameters.Parameter.Builder validVendorIdParam() {
        return MboParameters.Parameter
                .newBuilder()
                .setId(ParameterValueComposer.VENDOR_ID)
                .addName(wordMaker.apply("Производитель"))
                .setXslName(CategoryData.VENDOR)
                .setDescription("Название вендора")
                .setValueType(MboParameters.ValueType.ENUM)
                .setMandatory(true)
                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING);
    }

    private static MboParameters.Parameter.Builder validNameParam() {
        return MboParameters.Parameter
                .newBuilder()
                .setId(ParameterValueComposer.NAME_ID)
                .addName(wordMaker.apply("Имя"))
                .setXslName(CategoryData.NAME)
                .setDescription("Имя буквами")
                .setValueType(MboParameters.ValueType.STRING)
                .setMandatory(true)
                .setParamType(MboParameters.ParameterLevel.MODEL_LEVEL)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING);
    }
}
