package ru.yandex.market.ir.excel.generator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Issue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataForSizeParametersMock;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMockBuilder;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.excel.generator.param.ParameterInfo;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.partner.content.common.mocks.CategoryParametersFormParserMock;
import ru.yandex.market.robot.db.ParameterValueComposer;

public class CategoryInfoProducerTest {

    private static final long CATEGORY_ID = 123L;
    private static final String DEFAULT_BLOCK_NAME = "Общие характеристики";

    @Test
    public void doesNotThrowForDcpUiWhenCategoryParametersFormIsMissing() {
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = new CategoryParametersFormParserMock();
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(categoryDataKnowledge, categoryParametersFormParserMock);

        CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, ImportContentType.DCP_UI);
        assertThat(categoryInfo.getId()).isEqualTo(CATEGORY_ID);
        assertThat(categoryInfo.isParameterFormParserDataMissing()).isTrue();
    }

    @Test
    public void nameParameterIsMandatoryForGoodAndDcpExcel() {
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .nameParameterBuilder().build()
            .vendorParameterBuilder().build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addAllParamsToCategoryParametersFormParser(
            CATEGORY_ID, categoryDataKnowledge);
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImmutableList.of(ImportContentType.DCP_EXCEL)) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            switch (importContentType) {
                case DCP_EXCEL:
                    assertThat(categoryInfo.getParameterInfo(ParameterValueComposer.NAME_ID).isMandatory())
                        .describedAs(importContentType.toString())
                        .isTrue();
                    break;
                case DCP_UI:
                    assertThat(categoryInfo.getParameterInfo(ParameterValueComposer.NAME_ID))
                        .describedAs(importContentType.toString())
                        .isNull();
                    break;
                default:
                    throw new IllegalStateException("Unknown xls type " + importContentType);
            }
        }
    }

    @Test
    public void descriptionParameterIsMandatoryForGoodAndDcpExcel() {
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .stringParameterBuilder(MainParamCreator.DESCRIPTION_ID, MainParamCreator.DESCRIPTION_XSL_NAME).build()
            .vendorParameterBuilder().build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addAllParamsToCategoryParametersFormParser(
            CATEGORY_ID, categoryDataKnowledge);
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            switch (importContentType) {
                case DCP_EXCEL:
                    assertThat(categoryInfo.getParameterInfo(MainParamCreator.DESCRIPTION_ID).isMandatory()).isTrue();
                    break;
                case DCP_UI:
                    assertThat(categoryInfo.getParameterInfo(MainParamCreator.DESCRIPTION_ID)).isNull();
                    break;
                default:
                    throw new IllegalStateException("Unknown xls type " + importContentType);
            }
        }
    }

    @Test
    public void vendorParameterIsMandatoryForAllExcelFormats() {
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addAllParamsToCategoryParametersFormParser(
            CATEGORY_ID, categoryDataKnowledge);
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            switch (importContentType) {
                case DCP_EXCEL:
                    assertThat(categoryInfo.getParameterInfo(ParameterValueComposer.VENDOR_ID).isMandatory())
                        .describedAs(importContentType.toString())
                        .isTrue();
                    break;
                case DCP_UI:
                    assertThat(categoryInfo.getParameterInfo(ParameterValueComposer.VENDOR_ID))
                        .describedAs(importContentType.toString())
                        .isNull();
                    break;
                default:
                    throw new IllegalStateException("Unknown xls type " + importContentType);
            }
        }
    }

    @Test
    public void mandatoryIsRetainedForDcpContent() {
        final long paramId = 1;
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .stringParameterBuilder(paramId, "mandatory param").setMandatory(true).build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addAllParamsToCategoryParametersFormParser(
            CATEGORY_ID, categoryDataKnowledge);
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            assertThat(categoryInfo.getParameterInfo(paramId).isMandatory())
                .describedAs(importContentType.toString())
                .isTrue();
        }
    }

    @Test
    public void commonParamsArePrependedToListForDcpExcel() {
        CategoryDataKnowledge categoryDataKnowledge = createCategory().build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addAllParamsToCategoryParametersFormParser(
            CATEGORY_ID, categoryDataKnowledge);
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, ImportContentType.DCP_EXCEL);

        assertThat(categoryInfo.getParameters().stream())
            .extracting(ParameterInfo::getId)
            .containsExactly(
                (long)MainParamCreator.SKU_ID,
                (long)MainParamCreator.RATING,
                (long)MainParamCreator.GROUP_ID_ORDER,
                (long)MainParamCreator.GROUP_NAME_PARAM_ID,
                ParameterValueComposer.NAME_ID,
                ParameterValueComposer.VENDOR_ID,
                (long)MainParamCreator.MAIN_PICTURE_ORDER,
                MainParamCreator.DESCRIPTION_ID,
                ParameterValueComposer.VENDOR_CODE_ID,
                ParameterValueComposer.BARCODE_ID,
                (long)MainParamCreator.PICTURE_URL_ORDER);
    }

    @Test
    public void commonParametersAreSkippedForDcpUi() {
        CategoryDataKnowledge categoryDataKnowledge = createCategory().build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addAllParamsToCategoryParametersFormParser(
            CATEGORY_ID, categoryDataKnowledge);
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, ImportContentType.DCP_UI);

        assertThat(categoryInfo.getParameters()).isEmpty();
    }

    @Test
    public void hiddenParametersAreSkipped() {
        final int paramId = 1;
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .stringParameterBuilder(paramId, "hidden param").setHidden(true).build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addParamsToCategoryParametersFormParser(
            CATEGORY_ID,
            categoryDataKnowledge.getCategoryData(CATEGORY_ID).getParameterList());
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            assertThat(categoryInfo.getParameterInfo(paramId))
                .describedAs(importContentType.toString())
                .isNull();
        }
    }

    @Test
    public void serviceParametersAreSkipped() {
        final int paramId = 1;
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .stringParameterBuilder(paramId, "service param").setService(true).build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addParamsToCategoryParametersFormParser(
            CATEGORY_ID,
            categoryDataKnowledge.getCategoryData(CATEGORY_ID).getParameterList());
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            assertThat(categoryInfo.getParameterInfo(paramId))
                .describedAs(importContentType.toString())
                .isNull();
        }
    }

    @Test
    public void nonMandatoryMdmParametersAreSkipped() {
        final int paramId = 1;
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .stringParameterBuilder(paramId, "mdm param")
            .setMdmParameter(true)
            .setMandatory(false)
            .build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addAllParamsToCategoryParametersFormParser(
            CATEGORY_ID, categoryDataKnowledge);
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            assertThat(categoryInfo.getParameterInfo(paramId))
                .describedAs(importContentType.toString())
                .isNull();
        }
    }

    @Test
    public void nonSkuParametersWithoutCategoryFormXlsInfoAreSkipped() {
        final int paramId = 1;
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .stringParameterBuilder()
                .setParamId(paramId).setXlsName("mdm param").setSkuMode(MboParameters.SKUParameterMode.SKU_NONE).build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = new CategoryParametersFormParserMock();
        categoryParametersFormParserMock.addCategoryAttrs(CATEGORY_ID, Collections.emptyMap());
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            assertThat(categoryInfo.getParameterInfo(paramId))
                .describedAs(importContentType.toString())
                .isNull();
        }
    }

    @Test
    public void sizeParamsAreSkippedForDcp() {
        final long sizeParamId = 1;
        final long unitParamId = 2;
        final long numericParamId = 3;

        MboSizeMeasures.GetSizeMeasuresInfoResponse.CategoryResponse sizeMeasure =
            MboSizeMeasures.GetSizeMeasuresInfoResponse.CategoryResponse.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSizeMeasureInfos(MboSizeMeasures.SizeMeasureInfo.newBuilder()
                    .setSizeMeasure(MboSizeMeasures.SizeMeasure.newBuilder()
                        .setId(sizeParamId)
                        .setName("size")
                        .setValueParamId(sizeParamId)
                        .setUnitParamId(unitParamId)
                        .setNumericParamId(numericParamId)))
                .build();

        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .setSizeMeasureList(Collections.singletonList(sizeMeasure))
            .vendorParameterBuilder().build()
            .numericParameterBuilder()
                .setParamId(sizeParamId)
                .setXlsName("size")
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .setSubType(MboParameters.SubType.SIZE).build()
            .enumParameterBuilder()
                .setParamId(unitParamId)
                .setXlsName("unit")
                .setSubType(MboParameters.SubType.SIZE).build()
            .build()
            .build();

        CategoryParametersFormParserMock categoryParametersFormParserMock = addAllParamsToCategoryParametersFormParser(
            CATEGORY_ID, categoryDataKnowledge);
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            switch (importContentType) {
                case DCP_EXCEL:
                case DCP_UI:
                    assertThat(categoryInfo.getParameters())
                        .describedAs(importContentType.toString())
                        .extracting(ParameterInfo::getId)
                        .doesNotContain(unitParamId, numericParamId)
                        .contains(sizeParamId);
                    break;
                default:
                    assertThat(categoryInfo.getParameters())
                        .describedAs(importContentType.toString())
                        .extracting(ParameterInfo::getId)
                        .contains(sizeParamId, unitParamId);
                    break;
            }
        }
    }

    @Test
    public void paramsManuallyMarkedAsSkippedAreSkippedForDcp() {
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
                .startCategory(CATEGORY_ID)
                .vendorParameterBuilder().build()
                .stringParameterBuilder(ParamXslNames.GIRL_KIDS_SIZE_ID, "additional param").build()
                .build()
                .build();

        CategoryParametersFormParserMock categoryParametersFormParserMock = addParamsToCategoryParametersFormParser(
                CATEGORY_ID,
                categoryDataKnowledge.getCategoryData(CATEGORY_ID).getParameterList());
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
                categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImmutableList.of(ImportContentType.DCP_EXCEL, ImportContentType.DCP_UI)) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            assertThat(categoryInfo.getParameterInfo(ParamXslNames.GIRL_KIDS_SIZE_ID))
                    .describedAs(importContentType.toString())
                    .isNull();
        }
    }

    @Test
    public void parametersWithNonZeroContainingTabOrderAreSkippedForDcp() {
        final int paramId = 1;
        final String paramName = "param";
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .stringParameterBuilder(paramId, paramName).build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = new CategoryParametersFormParserMock();
        categoryParametersFormParserMock.addCategoryAttrs(CATEGORY_ID,
            ImmutableMap.of(paramName,
                new XslInfo(0, DEFAULT_BLOCK_NAME,
                    new XslInfo.Tab("Form tab", 1))));
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImmutableList.of(ImportContentType.DCP_EXCEL, ImportContentType.DCP_UI)) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            assertThat(categoryInfo.getParameterInfo(paramId))
                .describedAs(importContentType.toString())
                .isNull();
        }
    }

    @Test
    public void mandatoryServiceSkuParamIsNotSkipped() {
        final int paramId = 1;
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .stringParameterBuilder(paramId, "service param")
            .setService(true).setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL).setMandatory(true)
            .build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addParamsToCategoryParametersFormParser(
            CATEGORY_ID,
            categoryDataKnowledge.getCategoryData(CATEGORY_ID).getParameterList());
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            assertThat(categoryInfo.getParameterInfo(paramId))
                .describedAs(importContentType.toString())
                .isNotNull();
        }
    }

    @Test
    public void serviceSkuParamWithShowOnSkuTabIsNotSkipped() {
        final int paramId = 1;
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .stringParameterBuilder(paramId, "service param")
            .setService(true).setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL).setShowOnSkuTab(true)
            .build()
            .build()
            .build();
        CategoryParametersFormParserMock categoryParametersFormParserMock = addParamsToCategoryParametersFormParser(
            CATEGORY_ID,
            categoryDataKnowledge.getCategoryData(CATEGORY_ID).getParameterList());
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParserMock);

        for (ImportContentType importContentType : ImportContentType.values()) {
            CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, importContentType);
            assertThat(categoryInfo.getParameterInfo(paramId))
                .describedAs(importContentType.toString())
                .isNotNull();
        }
    }

    @Test
    @Issue("MARKETIR-18544")
    public void rangeWithSkippedRangeParamsIsSkipped() {
        final long paramId = 1;
        final long absentRangeParamId = 1_0;
        final long presentRangeParamId = 1_1;
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        categoryDataKnowledge.addCategoryData(CATEGORY_ID,
                new CategoryDataForSizeParametersMock(
                        new HashMap<Long, Pair<Long, Long>>() {

                            {
                                put(paramId, Pair.of(absentRangeParamId, presentRangeParamId));
                            }
                        },
                        true,
                        MboParameters.Parameter.newBuilder()
                                .setId(paramId)
                                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                                .buildPartial()
                ));
        CategoryParametersFormParserMock categoryParametersFormParserMock = addParamsToCategoryParametersFormParser(
                CATEGORY_ID,
                Collections.emptySet());
        CategoryInfoProducer categoryInfoProducer = new CategoryInfoProducer(
                categoryDataKnowledge, categoryParametersFormParserMock);
        CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(CATEGORY_ID, ImportContentType.DCP_UI);
        assertThat(categoryInfo.getSizeToNumericRangeParams())
                .describedAs("Range for absent params should be skipped")
                .doesNotContainKey(paramId);

    }

    private CategoryDataKnowledgeMockBuilder createCategory() {
        return CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .nameParameterBuilder().build()
            .vendorParameterBuilder().build()
            .vendorCodeParameterBuilder().build()
            .barCodeParameterBuilder().build()
            .stringParameterBuilder(MainParamCreator.DESCRIPTION_ID, MainParamCreator.DESCRIPTION_XSL_NAME).build()
            .build();
    }

    private CategoryParametersFormParserMock addAllParamsToCategoryParametersFormParser(
        long hid, CategoryDataKnowledge categoryDataKnowledge) {
        CategoryParametersFormParserMock categoryParametersFormParserMock = new CategoryParametersFormParserMock();
        CategoryData category = categoryDataKnowledge.getCategoryData(hid);
        Map<String, XslInfo> xslInfoMap = new HashMap<>();
        Collection<MboParameters.Parameter> parameterList = category.getParameterList()
            .stream().filter(p -> !p.getService() && !p.getHidden())
            .collect(Collectors.toList());
        int offset = 0;
        for (MboParameters.Parameter parameter : parameterList) {
            xslInfoMap.put(parameter.getXslName(), new XslInfo(offset++, DEFAULT_BLOCK_NAME));
        }
        categoryParametersFormParserMock.addCategoryAttrs(hid, xslInfoMap);
        return categoryParametersFormParserMock;
    }

    private CategoryParametersFormParserMock addParamsToCategoryParametersFormParser(long hid, Iterable<MboParameters.Parameter> parameters) {
        CategoryParametersFormParserMock categoryParametersFormParserMock = new CategoryParametersFormParserMock();
        Map<String, XslInfo> xslInfoMap = new HashMap<>();
        int offset = 0;
        for (MboParameters.Parameter parameter : parameters) {
            xslInfoMap.put(parameter.getXslName(), new XslInfo(offset++, DEFAULT_BLOCK_NAME));
        }
        categoryParametersFormParserMock.addCategoryAttrs(hid, xslInfoMap);
        return categoryParametersFormParserMock;

    }

}
