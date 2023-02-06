package ru.yandex.market.mbo.mdm.common.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuGoldenParamUtilTest extends MdmBaseDbTestClass {
    private static final ShopSkuKey KEY = new ShopSkuKey(1235, "fds");
    @Autowired
    private ServiceSskuConverter converter;
    @Autowired
    private MdmParamCache mdmParamCache;

    private SskuGoldenParamUtil util;

    @Before
    public void setup() {
        util = new SskuGoldenParamUtil(mdmParamCache);
    }

    @Test
    public void testMeasurementStateConversion() {
        ReferenceItemWrapper wrapper = new ReferenceItemWrapper(MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(1)
                .setShopSku("xxx")
                .build())
            .setMeasurementState(MdmIrisPayload.MeasurementState.newBuilder()
                .setIsMeasured(true)
                .setLastMeasurementTs(100500L)
                .build())
            .build());
        var params = util.createSskuGoldenParamValuesFromReferenceItem(
            wrapper, SskuGoldenParamUtil.ParamsGroup.COMMON);
        Assertions.assertThat(params.stream()
            .filter(p -> p.getMdmParamId() == KnownMdmParams.HAS_MEASUREMENT_AFTER_INHERIT)
            .map(MdmParamValue::getBool)
            .flatMap(Optional::stream)
            .findFirst()
        ).hasValue(true);
        Assertions.assertThat(params.stream()
            .filter(p -> p.getMdmParamId() == KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_AFTER_INHERIT)
            .map(MdmParamValue::getNumeric)
            .flatMap(Optional::stream)
            .findFirst()
        ).hasValue(new BigDecimal(100500L));
    }

    @Test
    public void testUpdatedTsPreserved() {
        var sourceUpdatedTs = Instant.now().minusSeconds(100500L).truncatedTo(ChronoUnit.MILLIS);
        var updatedTs = sourceUpdatedTs.plusSeconds(200500L);
        var ssku = new CommonSskuBuilder(mdmParamCache, KEY)
            .withVghAfterInheritance(13, 14, 15, 16)
            .customized(v -> v.setUpdatedTs(updatedTs).setSourceUpdatedTs(sourceUpdatedTs))
            .build();

        ReferenceItemWrapper ri = converter.toReferenceItem(ssku.getBaseSsku());

        // Предварительная проверка, что в процессе конвертаций ts-ы не теряются
        var conversionValues = converter.fromReferenceItem(ri);
        Assertions.assertThat(conversionValues).hasSize(4);
        for (var value : conversionValues) {
            Assertions.assertThat(value.getSourceUpdatedTs()).isEqualTo(sourceUpdatedTs);
            Assertions.assertThat(value.getUpdatedTs()).isEqualTo(updatedTs);
        }

        // Непосредственно тест - проверяем, что перекладывание туды-сюды в голден-парамы не теряет штампы
        var values = util.createSskuGoldenParamValuesFromReferenceItem(ri, SskuGoldenParamUtil.ParamsGroup.SSKU);

        Assertions.assertThat(values).hasSize(4);
        for (SskuGoldenParamValue value : values) {
            Assertions.assertThat(value.getSourceUpdatedTs()).isEqualTo(sourceUpdatedTs);
            Assertions.assertThat(value.getUpdatedTs()).isEqualTo(updatedTs);
        }
    }
}
