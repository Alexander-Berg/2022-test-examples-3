package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MetadataProviderMock;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

public class BmdmEntityTypeToParamsConverterTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmParamCache mdmParamCache;

    private BmdmEntityTypeToParamsConverter converter;

    @Before
    public void setUp() throws Exception {
        MetadataProviderMock metadataProviderMock = new MetadataProviderMock();
        metadataProviderMock.addEntityType(TestBmdmUtils.VGH_ENTITY_TYPE);
        metadataProviderMock.addEntityType(TestBmdmUtils.GOLDEN_MSKU_ENTITY_TYPE);
        metadataProviderMock.addEntityType(TestBmdmUtils.TIME_ENTITY_TYPE);
        metadataProviderMock.addExternalReferences(TestBmdmUtils.GOLD_MSKU_EXTERNAL_REFERENCES);
        converter = new BmdmEntityTypeToParamsConverterImpl(
            metadataProviderMock,
            new BmdmAttributeToMdmParamConverterImpl(metadataProviderMock)
        );
    }

    @Test
    public void testCommonMskuConversion() {
        Assertions.assertThat(converter.extractParams(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID))
            .containsExactlyInAnyOrderElementsOf(mdmParamCache.find(List.of(
                KnownMdmParams.MSKU_ID_REFERENCE,
                KnownMdmParams.LENGTH,
                KnownMdmParams.WIDTH,
                KnownMdmParams.HEIGHT,
                KnownMdmParams.WEIGHT_GROSS,
                KnownMdmParams.WEIGHT_NET,
                KnownMdmParams.WEIGHT_TARE,
                KnownMdmParams.SHELF_LIFE,
                KnownMdmParams.SHELF_LIFE_UNIT,
                KnownMdmParams.SHELF_LIFE_COMMENT,
                KnownMdmParams.SHELF_LIFE_UNLIMITED,
                KnownMdmParams.HIDE_SHELF_LIFE,
                KnownMdmParams.LIFE_TIME,
                KnownMdmParams.LIFE_TIME_UNIT,
                KnownMdmParams.LIFE_TIME_COMMENT,
                KnownMdmParams.LIFE_TIME_UNLIMITED,
                KnownMdmParams.HIDE_LIFE_TIME,
                KnownMdmParams.GUARANTEE_PERIOD,
                KnownMdmParams.GUARANTEE_PERIOD_UNIT,
                KnownMdmParams.GUARANTEE_PERIOD_COMMENT,
                KnownMdmParams.GUARANTEE_PERIOD_UNLIMITED,
                KnownMdmParams.HIDE_GUARANTEE_PERIOD
            )));
    }

    @Test
    public void whenParsingUnknownEntityShouldReturnNoMdmParams() {
        Assertions.assertThat(converter.extractParams(0x7fffffffffffffffL)).isEmpty();
    }
}
