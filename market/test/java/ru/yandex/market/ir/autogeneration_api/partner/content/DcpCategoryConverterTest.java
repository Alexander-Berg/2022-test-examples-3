package ru.yandex.market.ir.autogeneration_api.partner.content;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.db.NamedParamRestriction;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean.DcpCategoryPojo;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.controller.manager.DcpCategoryConverter;
import ru.yandex.market.ir.excel.generator.CategoryInfo;
import ru.yandex.market.ir.excel.generator.ImportContentType;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.excel.generator.param.MboParameterWrapper;
import ru.yandex.market.ir.excel.generator.param.ParameterInfoBuilder;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;

public class DcpCategoryConverterTest {
    private static final long SKU_DEFINING_PARAM_ID = 1;
    private static final long SIZE_SKU_DEFINING_PARAM_ID = 2;
    private static final long NOT_SKU_PARAM_ID = 3;
    private static final long SIZE_RANGE_LEFT_PARAM_ID = 4;
    private static final long SIZE_RANGE_RIGHT_PARAM_ID = 5;
    private static final long KNOWN_VENDOR_ID = 1;
    private static final long UNKNOWN_VENDOR_ID = 2;

    private CategoryInfo categoryInfo;

    @Before
    public void initCategory() {
        Map<Long, Pair<Long, Long>> sizeToNumericRange = new HashMap<>();
        sizeToNumericRange
                .put(SIZE_SKU_DEFINING_PARAM_ID, Pair.of(SIZE_RANGE_LEFT_PARAM_ID, SIZE_RANGE_RIGHT_PARAM_ID));
        Long2ObjectMap<LongSet> restrictedValues = new Long2ObjectOpenHashMap<>();
        restrictedValues.put(KNOWN_VENDOR_ID, new LongOpenHashSet(Arrays.asList(11L, 12L, 13L)));
        categoryInfo = CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .addParameter(
                        ParameterInfoBuilder.asMboParameter(
                                new MboParameterWrapper(
                                        MboParameters.Parameter.newBuilder()
                                                .setId(SKU_DEFINING_PARAM_ID)
                                                .setXslName("1")
                                                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                                                .build()
                                )
                        )
                                .setImportContentType(ImportContentType.DCP_EXCEL)
                                .build()
                )
                .addParameter(
                        ParameterInfoBuilder.asMboParameter(
                                new MboParameterWrapper(
                                        MboParameters.Parameter.newBuilder()
                                                .setId(SIZE_SKU_DEFINING_PARAM_ID)
                                                .setXslName("2")
                                                .setValueType(MboParameters.ValueType.ENUM)
                                                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                                                .addOption(MboParameters.Option.newBuilder().setId(11L).build())
                                                .addOption(MboParameters.Option.newBuilder().setId(12L).build())
                                                .addOption(MboParameters.Option.newBuilder().setId(13L).build())
                                                .addOption(MboParameters.Option.newBuilder().setId(14L).build())
                                                .build(),
                                         false, true, false, false
                                )
                        )
                                .setImportContentType(ImportContentType.DCP_EXCEL)
                                .build()
                )
                .addParameter(
                        ParameterInfoBuilder.asMboParameter(
                                new MboParameterWrapper(
                                        MboParameters.Parameter.newBuilder()
                                                .setId(NOT_SKU_PARAM_ID)
                                                .setXslName("3")
                                                .build()
                                )
                        )
                                .setBlockName("ttt")
                                .setImportContentType(ImportContentType.DCP_EXCEL)
                                .build()
                )
                .addParameter(
                        ParameterInfoBuilder.asMboParameter(
                                new MboParameterWrapper(
                                        MboParameters.Parameter.newBuilder()
                                                .setId(SIZE_RANGE_LEFT_PARAM_ID)
                                                .setXslName("4")
                                                .build(),
                                        false, false, false, true
                                )
                        )
                                .setImportContentType(ImportContentType.DCP_EXCEL)
                                .build()
                )
                .addParameter(
                        ParameterInfoBuilder.asMboParameter(
                                new MboParameterWrapper(
                                        MboParameters.Parameter.newBuilder()
                                                .setId(SIZE_RANGE_RIGHT_PARAM_ID)
                                                .setXslName("5")
                                                .build(),
                                        false, false, false, true
                                )
                        )
                                .setImportContentType(ImportContentType.DCP_EXCEL)
                                .build()
                )
                .setNamedParamRestrictions(Collections.singletonList(
                        new NamedParamRestriction("t", ParameterValueComposer.VENDOR_ID,
                                SIZE_SKU_DEFINING_PARAM_ID, restrictedValues)))
                .setSizeToNumericRange(sizeToNumericRange)
                .setSizeToNumericRange(sizeToNumericRange)
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL);
    }

    @Test
    public void testNoSizeOptionsWithNullVendor() {
        DcpCategoryPojo result = new DcpCategoryPojo();
        DcpCategoryConverter.buildSkuParams(categoryInfo, result, null);

        result.getParameterList().stream()
                .filter(p -> p.getParameterId() == SIZE_SKU_DEFINING_PARAM_ID)
                .forEach(p -> Assert.assertTrue(p.getOptions().isEmpty()));
    }

    @Test
    public void testNoSizeOptionsWithUnknownVendor() {
        DcpCategoryPojo result = new DcpCategoryPojo();
        DcpCategoryConverter.buildSkuParams(categoryInfo, result, UNKNOWN_VENDOR_ID);

        result.getParameterList().stream()
                .filter(p -> p.getParameterId() == SIZE_SKU_DEFINING_PARAM_ID)
                .forEach(p -> Assert.assertTrue(p.getOptions().isEmpty()));
    }

    @Test
    public void testSizeOptionsWithKnownVendor() {
        DcpCategoryPojo result = new DcpCategoryPojo();
        DcpCategoryConverter.buildSkuParams(categoryInfo, result, KNOWN_VENDOR_ID);

        result.getParameterList().stream()
                .filter(p -> p.getParameterId() == SIZE_SKU_DEFINING_PARAM_ID)
                .forEach(p -> Assert.assertEquals(3, p.getOptions().size()));
    }

    @Test
    public void testSkuParamsWithSizes() {
        DcpCategoryPojo result = new DcpCategoryPojo();
        DcpCategoryConverter.buildSkuParams(categoryInfo, result, null);

        assertThat(result.getParameterList())
                .extracting(DcpCategoryPojo.Parameter::getParameterId)
                .containsExactlyInAnyOrder(
                        SIZE_SKU_DEFINING_PARAM_ID, SIZE_RANGE_LEFT_PARAM_ID, SIZE_RANGE_RIGHT_PARAM_ID,
                        SKU_DEFINING_PARAM_ID
                );

        Optional<DcpCategoryPojo.Section> section = result.getLayout().getSections().stream()
                .filter(s -> s.getId().equals("sku"))
                .findAny();

        Assert.assertTrue(section.isPresent());
        Assert.assertEquals(1, section.get().getGroups().size());

        DcpCategoryPojo.Group group = section.get().getGroups().get(0);
        DcpCategoryPojo.Parameter p = new DcpCategoryPojo.Parameter();
        p.setParameterId(SKU_DEFINING_PARAM_ID);

        assertThat(group.getParameterReferences())
                .containsExactly(
                        new DcpCategoryPojo.ParamRef(
                                SIZE_SKU_DEFINING_PARAM_ID, SIZE_RANGE_LEFT_PARAM_ID, SIZE_RANGE_RIGHT_PARAM_ID),
                        new DcpCategoryPojo.ParamRef(p)
                );
    }

    @Test
    public void testModelParams() {
        DcpCategoryPojo result = new DcpCategoryPojo();
        DcpCategoryConverter.buildModelParams(categoryInfo, result);

        assertThat(result.getParameterList())
                .extracting(DcpCategoryPojo.Parameter::getParameterId)
                .contains(NOT_SKU_PARAM_ID)
                .doesNotContain(SIZE_SKU_DEFINING_PARAM_ID, SIZE_RANGE_LEFT_PARAM_ID, SIZE_RANGE_RIGHT_PARAM_ID,
                        SKU_DEFINING_PARAM_ID);

        Optional<DcpCategoryPojo.Section> section = result.getLayout().getSections().stream()
                .filter(s -> s.getId().equals("model"))
                .findAny();
        Assert.assertTrue(section.isPresent());

        DcpCategoryPojo.Parameter p = new DcpCategoryPojo.Parameter();
        p.setParameterId(NOT_SKU_PARAM_ID);
        List<DcpCategoryPojo.ParamRef> paramRefList = section.get().getGroups().stream()
                .flatMap(g -> g.getParameterReferences().stream()).collect(Collectors.toList());
        assertThat(paramRefList).contains(new DcpCategoryPojo.ParamRef(p));
    }

}
