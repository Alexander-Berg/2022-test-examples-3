package ru.yandex.market.mbo.db.modelstorage.validation.dump;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.mbo.export.client.measure.CategorySizeMeasuresConfig;
import ru.yandex.market.mbo.export.client.measure.CategorySizeMeasuresServiceClient;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersConfig;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.WordProtoUtils;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ExporterDumpValidationServiceTest extends AbstractDumpValidationServiceTest {

    @Mock
    CategoryParametersServiceClient categoryParametersServiceClient;

    @Mock
    CategorySizeMeasuresServiceClient categorySizeMeasuresServiceClient;

    @Before
    public void setUp() {
        List<MboParameters.Parameter> params = Arrays.asList(
            MboParameters.Parameter.newBuilder()
                .setId(1)
                .setXslName("numeric")
                .addName(WordProtoUtils.defaultWord("Numeric"))
                .setValueType(MboParameters.ValueType.NUMERIC)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .setUnit(MboParameters.Unit.newBuilder()
                    .setId(1)
                    .addName(WordProtoUtils.defaultWord("kg")))
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(2)
                .setXslName("sizeValue")
                .addName(WordProtoUtils.defaultWord("SizeValue"))
                .setValueType(MboParameters.ValueType.ENUM)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .addOption(buildOption(10L, "size1"))
                .addOption(buildOption(11L, "size2"))
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(3)
                .setXslName("sizeUnit")
                .addName(WordProtoUtils.defaultWord("SizeUnit"))
                .setValueType(MboParameters.ValueType.ENUM)
                .addOption(buildOption(20L, "unit1"))
                .addOption(buildOption(21L, "unit2"))
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(6)
                .setXslName("sizeNumericMin")
                .addName(WordProtoUtils.defaultWord("sizeNumericMin"))
                .setValueType(MboParameters.ValueType.NUMERIC)
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(7)
                .setXslName("sizeNumericMax")
                .addName(WordProtoUtils.defaultWord("sizeNumericMax"))
                .setValueType(MboParameters.ValueType.NUMERIC)
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(4)
                .setXslName("string")
                .addName(WordProtoUtils.defaultWord("String"))
                .setValueType(MboParameters.ValueType.STRING)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(KnownIds.NAME_PARAM_ID)
                .setXslName(XslNames.NAME)
                .addName(WordProtoUtils.defaultWord("Name"))
                .setValueType(MboParameters.ValueType.STRING)
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(KnownIds.VENDOR_PARAM_ID)
                .setXslName(XslNames.VENDOR)
                .addName(WordProtoUtils.defaultWord("Vendor"))
                .setValueType(MboParameters.ValueType.ENUM)
                .addOption(buildOption(1L, "vendor1"))
                .addOption(buildOption(2L, "vendor2"))
                .build(),
            MboParameters.Parameter.newBuilder()
                .setId(KnownIds.IS_PARTNER_PARAM_ID)
                .setXslName(XslNames.IS_PARTNER)
                .addName(WordProtoUtils.defaultWord("Партнерская"))
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .addOption(buildOption(3L, "TRUE"))
                .addOption(buildOption(4L, "FALSE"))
                .build()
        );
        MboParameters.ParameterValueLinks valueLinks = MboParameters.ParameterValueLinks.newBuilder()
            .setType(MboParameters.ValueLinkRestrictionType.BIDIRECTIONAL)
            .setParamId(3)
            .setLinkedParamId(2)
            .addLinkedValue(
                MboParameters.ValueLink.newBuilder()
                .setOptionId(20L)
                .addLinkedOptionId(10L)
                .addLinkedOptionId(11L)
                .build()
            ).build();

        MboParameters.Category category = MboParameters.Category.newBuilder()
            .setHid(1L)
            .addAllParameter(params)
            .addParameterValueLinks(valueLinks)
            .setGuruTitleTemplate(String.format(
                "{\"delimiter\":\" \",\"values\":[[(1 ),(v%d ),null,(true)],[(1 ),(t0 ),null,(true)]]}",
                KnownIds.VENDOR_PARAM_ID))
            .setGuruTitleWithoutVendorTemplate("{\"delimiter\":\" \",\"values\":[[(1 ),(t0 ),null,(true)]]}")
            .build();

        when(categoryParametersServiceClient.getCategoryParameters(anyLong()))
            .thenReturn(new CategoryParametersConfig(category));

        MboSizeMeasures.CategorySizeMeasure sizeMeasure = MboSizeMeasures.CategorySizeMeasure.newBuilder()
            .setCategoryId(1L)
            .addSizeMeasure(MboSizeMeasures.SizeMeasure.newBuilder()
                .setId(3L)
                .setName("Measure")
                .setValueParamId(2)
                .setUnitParamId(3)
                .setNumericParamId(5)
                .setMinNumericParamId(6)
                .setMaxNumericParamId(7)
                .build())
            .build();

        when(categorySizeMeasuresServiceClient.getCategorySizeMeasures(anyLong()))
            .thenReturn(new CategorySizeMeasuresConfig(sizeMeasure));

        dumpValidationService = new ExporterDumpValidationService(
            categoryParametersServiceClient, categorySizeMeasuresServiceClient, autoUser);
    }

    @Test
    public void testTitleGeneration() {
        testTitleGeneratedInternal();
        testTitleGeneratedIsSkuInternal();
    }

    private MboParameters.Option buildOption(long id, String name) {
        return MboParameters.Option.newBuilder()
            .setId(id)
            .addName(WordProtoUtils.defaultWord(name))
            .build();
    }
}
