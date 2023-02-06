package ru.yandex.market.mbo.export.modelstorage.pipe.sizemeasure;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.Range;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.export.SizeChartValuesData;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.export.modelstorage.pipe.Pipe;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getDefaultSkuBuilder;

@SuppressWarnings("checkstyle:magicNumber")
public class SizeChartConversionPipePartTest {
    private static final AtomicLong IDS = new AtomicLong(0);
    private static final long USER_ID = 100500L;

    @Test
    public void noSizeParam() throws IOException {
        CommonModel sku = getDefaultSkuBuilder()
            .id(IDS.incrementAndGet()).endModel();

        ModelPipeContext context = createContextWithSku(sku);
        createPipe().acceptModelsGroup(context);

        List<CommonModel> skus = getSkus(context);
        Assertions.assertThat(skus).contains(sku);
        Assertions.assertThat(skus.get(0).getParameterValues().size()).isEqualTo(1);
    }

    @Test
    public void oneSizeParamToConvert() throws IOException {
        CommonModel sku = getDefaultSkuBuilder()
            .id(IDS.incrementAndGet())
            .parameterValues(123L, "height", 456L)
            .endModel();

        ModelPipeContext context = createContextWithSku(sku);
        createPipe(ImmutableMap.of(456L, Range.is(2L)),
            ImmutableMap.of(123L, 1001L),
            ImmutableMap.of(1001L, ImmutableMap.of(2L, Range.between(0L, 5L)))
            ).acceptModelsGroup(context);

        List<CommonModel> skus = getSkus(context);
        Assertions.assertThat(skus).contains(sku);
        Assertions.assertThat(skus.get(0).getParameterValues(123L).getOptionIds().size()).isEqualTo(2);
    }

    @Test
    public void manySizeParamsToConvert() throws IOException {
        CommonModel sku = getDefaultSkuBuilder()
            .id(IDS.incrementAndGet())
            .parameterValues(123L, "height", 456L)
            .parameterValues(12345L, "length", 5678L)
            .endModel();

        ModelPipeContext context = createContextWithSku(sku);
        createPipe(ImmutableMap.of(456L, Range.is(2L),
                                   5678L, Range.is(40L)),
            ImmutableMap.of(123L, 1001L, 12345L, 2003L),
            ImmutableMap.of(1001L, ImmutableMap.of(2L, Range.between(0L, 5L)),
                            2003L, ImmutableMap.of(5005L, Range.between(36L, 42L)))
        ).acceptModelsGroup(context);

        List<CommonModel> skus = getSkus(context);
        Assertions.assertThat(skus).contains(sku);
        Assertions.assertThat(skus.get(0).getParameterValues(123L).getOptionIds().size()).isEqualTo(2);
        Assertions.assertThat(skus.get(0).getParameterValues(12345L).getOptionIds().size()).isEqualTo(2);
    }

    @Test
    public void oneSizeParamNothingToConvert() throws IOException {
        CommonModel sku = getDefaultSkuBuilder()
            .id(IDS.incrementAndGet())
            .parameterValues(123L, "height", 456L)
            .endModel();

        ModelPipeContext context = createContextWithSku(sku);
        createPipe(ImmutableMap.of(456L, Range.is(2L)),
            ImmutableMap.of(123L, 1001L),
            ImmutableMap.of(1001L, ImmutableMap.of(2L, Range.between(50L, 100L)))
        ).acceptModelsGroup(context);

        List<CommonModel> skus = getSkus(context);
        Assertions.assertThat(skus).contains(sku);
        Assertions.assertThat(skus.get(0).getParameterValues().size()).isEqualTo(2);
        Assertions.assertThat(skus.get(0).getParameterValues(123L).getOptionIds().size()).isEqualTo(1);
    }

    private ModelPipeContext createContextWithSku(CommonModel... skus) {
        CommonModel guru = CommonModelBuilder.newBuilder()
            .id(IDS.incrementAndGet()).currentType(CommonModel.Source.GURU)
            .getModel();

        return new ModelPipeContext(
            ModelProtoConverter.convert(guru),
            Collections.emptyList(),
            Arrays.stream(skus).map(ModelProtoConverter::convert).collect(Collectors.toList()));
    }

    private Pipe createPipe() {
        return createPipe(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private Pipe createPipe(Map<Long, Range<Long>> sizeToRange,
                            Map<Long, Long> sizeParamToMeasure,
                            Map<Long, Map<Long, Range<Long>>> sizeChartMeasureToRange) {
        SizeChartValuesData scvd = new SizeChartValuesData(sizeToRange, sizeParamToMeasure, sizeChartMeasureToRange);
        return Pipe.start()
            .then(SizeChartConversionPipePart.createToTest(scvd))
            .build();
    }

    private List<CommonModel> getSkus(ModelPipeContext context) {
        return ModelProtoConverter.reverseConvertAll(context.getSkusForOutput());
    }
}
