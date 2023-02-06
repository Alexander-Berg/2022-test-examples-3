package ru.yandex.market.mbo.mdm.common.masterdata.validator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.DimensionsBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.RslBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.TransportUnitBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MdmParamValueBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.VatBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.TransportUnit;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuBlocksServiceTest extends MdmBaseDbTestClass {
    private static final ShopSkuKey KEY = new ShopSkuKey(12345, "xxx");
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private SskuBlocksService service;

    @Test
    public void emptySskuYieldsNoBlocks() {
        var ssku = builder().build();

        SskuBlocks sskuBlocks = service.split(ssku);
        Assertions.assertThat(sskuBlocks.isEmpty()).isTrue();
    }

    @Test
    public void basicValuesOnServicesAreIgnored() {
        var ssku = builder()
            .startServiceValues(1)
                .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "США")
                .withVat(VatRate.VAT_10)
                .with(KnownMdmParams.DELIVERY_TIME, 7L)
            .endServiceValues()
            .startServiceValues(2)
                .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 4L)
                .withLifeTime(12, TimeInUnits.TimeUnit.WEEK, "Должен проигнорироваться")
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);

        var basicBlocks = sskuBlocks.getBasicBlocks();
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        var serviceBlocks2 = sskuBlocks.getServiceBlocks(2);

        Assertions.assertThat((Object) extractBlock(basicBlocks, ItemBlock.BlockType.LIFE_TIME)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks1, ItemBlock.BlockType.LIFE_TIME)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks2, ItemBlock.BlockType.LIFE_TIME)).isNull();
        Assertions.assertThat((Object) extractBlock(basicBlocks, KnownMdmParams.MANUFACTURER_COUNTRY)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks1, KnownMdmParams.MANUFACTURER_COUNTRY)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks2, KnownMdmParams.MANUFACTURER_COUNTRY)).isNull();
    }

    @Test
    public void serviceValuesOnBasicAreIgnored() {
        var ssku = builder()
            .withVat(VatRate.VAT_10)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 4L)
            .startServiceValues(1)
                .with(KnownMdmParams.DELIVERY_TIME, 7L)
            .endServiceValues()
            .startServiceValues(2)
                .with(KnownMdmParams.DELIVERY_TIME, 7L)
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);

        var basicBlocks = sskuBlocks.getBasicBlocks();
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        var serviceBlocks2 = sskuBlocks.getServiceBlocks(2);

        Assertions.assertThat((Object) extractBlock(basicBlocks, ItemBlock.BlockType.VAT)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks1, ItemBlock.BlockType.VAT)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks2, ItemBlock.BlockType.VAT)).isNull();
        Assertions.assertThat((Object) extractBlock(basicBlocks, KnownMdmParams.QUANTUM_OF_SUPPLY)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks1, KnownMdmParams.QUANTUM_OF_SUPPLY)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks2, KnownMdmParams.QUANTUM_OF_SUPPLY)).isNull();
    }

    @Test
    public void basicValuesAreNotCopiedOrMovedToServiceBlocksAfterSplit() {
        var ssku = builder()
            .with(KnownMdmParams.WIDTH, 14L)
            .with(KnownMdmParams.HEIGHT, 14L)
            .with(KnownMdmParams.LENGTH, 14L)
            .with(KnownMdmParams.WEIGHT_GROSS, 14L)
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
            .startServiceValues(1)
                .with(KnownMdmParams.DELIVERY_TIME, 7L)
            .endServiceValues()
            .startServiceValues(2)
                .with(KnownMdmParams.DELIVERY_TIME, 7L)
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);

        var basicBlocks = sskuBlocks.getBasicBlocks();
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        var serviceBlocks2 = sskuBlocks.getServiceBlocks(2);

        Assertions.assertThat((Object) extractBlock(basicBlocks, ItemBlock.BlockType.DIMENSIONS)).isNotNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks1, ItemBlock.BlockType.DIMENSIONS)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks2, ItemBlock.BlockType.DIMENSIONS)).isNull();
        Assertions.assertThat((Object) extractBlock(basicBlocks, KnownMdmParams.MANUFACTURER_COUNTRY)).isNotNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks1, KnownMdmParams.MANUFACTURER_COUNTRY)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks2, KnownMdmParams.MANUFACTURER_COUNTRY)).isNull();
    }

    @Test
    public void serviceValuesAreNotCopiedOrMovedToBasicBlocksAfterSplit() {
        var ssku = builder()
            .with(KnownMdmParams.WIDTH, 14L)
            .with(KnownMdmParams.HEIGHT, 14L)
            .with(KnownMdmParams.LENGTH, 14L)
            .with(KnownMdmParams.WEIGHT_GROSS, 14L)
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
            .startServiceValues(1)
                .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 6L)
                .with(KnownMdmParams.QUANTITY_IN_PACK, 5L)
            .endServiceValues()
            .startServiceValues(2)
                .with(KnownMdmParams.DELIVERY_TIME, 7L)
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);

        var basicBlocks = sskuBlocks.getBasicBlocks();
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        var serviceBlocks2 = sskuBlocks.getServiceBlocks(2);

        Assertions.assertThat((Object) extractBlock(basicBlocks, ItemBlock.BlockType.TRANSPORT_UNIT)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks1, ItemBlock.BlockType.TRANSPORT_UNIT)).isNotNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks2, ItemBlock.BlockType.TRANSPORT_UNIT)).isNull();
        Assertions.assertThat((Object) extractBlock(basicBlocks, KnownMdmParams.DELIVERY_TIME)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks1, KnownMdmParams.DELIVERY_TIME)).isNull();
        Assertions.assertThat((Object) extractBlock(serviceBlocks2, KnownMdmParams.DELIVERY_TIME)).isNotNull();
    }

    @Test
    public void basicValuesSplitOk() {
        var ssku = builder()
            .withShelfLife(5, TimeInUnits.TimeUnit.YEAR, "SL comment")
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
            .with(KnownMdmParams.WIDTH, 10L)
            .with(KnownMdmParams.HEIGHT, 11L)
            .with(KnownMdmParams.LENGTH, 12L)
            .with(KnownMdmParams.WEIGHT_GROSS, 13L)
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);

        var basicBlocks = sskuBlocks.getBasicBlocks();

        ValueCommentBlock shelfLifeBlock = extractBlock(basicBlocks, ItemBlock.BlockType.SHELF_LIFE);
        MdmParamValueBlock<String> countryBlock = extractBlock(basicBlocks, KnownMdmParams.MANUFACTURER_COUNTRY);
        DimensionsBlock vghBlock = extractBlock(basicBlocks, ItemBlock.BlockType.DIMENSIONS);

        Assertions.assertThat(shelfLifeBlock).isNotNull();
        Assertions.assertThat(countryBlock).isNotNull();
        Assertions.assertThat(vghBlock).isNotNull();

        Assertions.assertThat(shelfLifeBlock.getValueWithUnit())
            .isEqualTo(new TimeInUnits(5, TimeInUnits.TimeUnit.YEAR));
        Assertions.assertThat(shelfLifeBlock.getValueComment()).isEqualTo("SL comment");
        Assertions.assertThat(countryBlock.getMdmParamValue().get().getStrings()).containsExactly("Россия", "Китай");
        Assertions.assertThat(vghBlock.getWidth().longValue()).isEqualTo(10L);
        Assertions.assertThat(vghBlock.getHeight().longValue()).isEqualTo(11L);
        Assertions.assertThat(vghBlock.getLength().longValue()).isEqualTo(12L);
        Assertions.assertThat(vghBlock.getWeightGross().longValue()).isEqualTo(13L);
    }

    @Test
    public void serviceValuesSplitOk() {
        var ssku = builder()
            .startServiceValues(1)
                .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 6L)
                .with(KnownMdmParams.QUANTITY_IN_PACK, 5L)
                .with(KnownMdmParams.RSL_IN_DAYS, 60L)
                .with(KnownMdmParams.RSL_OUT_DAYS, 20L)
            .endServiceValues()
            .startServiceValues(2)
                .withVat(VatRate.VAT_18)
                .with(KnownMdmParams.DELIVERY_TIME, 7L)
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        var serviceBlocks2 = sskuBlocks.getServiceBlocks(2);

        TransportUnitBlock transportUnitBlock = extractBlock(serviceBlocks1, ItemBlock.BlockType.TRANSPORT_UNIT);
        RslBlock rslBlock = extractBlock(serviceBlocks1, ItemBlock.BlockType.RSL);
        VatBlock vatBlock = extractBlock(serviceBlocks2, ItemBlock.BlockType.VAT);
        MdmParamValueBlock<BigDecimal> deliveryTimeBlock = extractBlock(serviceBlocks2, KnownMdmParams.DELIVERY_TIME);

        Assertions.assertThat(transportUnitBlock).isNotNull();
        Assertions.assertThat(rslBlock).isNotNull();
        Assertions.assertThat(vatBlock).isNotNull();
        Assertions.assertThat(deliveryTimeBlock).isNotNull();

        Assertions.assertThat(transportUnitBlock.getTransportUnit()).isEqualTo(new TransportUnit(6, 5));
        Assertions.assertThat(rslBlock.getMinInboundLifetimeDay().get(0).getValue()).isEqualTo(60);
        Assertions.assertThat(rslBlock.getMinOutboundLifetimeDay().get(0).getValue()).isEqualTo(20);
        Assertions.assertThat(vatBlock.getRawValue()).isEqualTo(VatRate.VAT_18);
        Assertions.assertThat(deliveryTimeBlock.getRawValue().longValue()).isEqualTo(7L);
    }

    @Test
    public void sourceIsPreserved() {
        var measurementSource = new MasterDataSource(MasterDataSourceType.MEASUREMENT, "172");
        var operatorSource = new MasterDataSource(MasterDataSourceType.MDM_OPERATOR, "vasia");
        var supplierSource = new MasterDataSource(MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name());
        var defaultSource = MasterDataSource.DEFAULT_SOURCE;
        var mdmRslSource = new MasterDataSource(MasterDataSourceType.MDM_DEFAULT, MasterDataSourceType.RSL_SOURCE_ID);

        var ssku = builder()
            .with(KnownMdmParams.WIDTH, 14L).customized(v -> v.setMasterDataSource(measurementSource))
            .with(KnownMdmParams.HEIGHT, 14L).customized(v -> v.setMasterDataSource(measurementSource))
            .with(KnownMdmParams.LENGTH, 14L).customized(v -> v.setMasterDataSource(measurementSource))
            .with(KnownMdmParams.WEIGHT_GROSS, 14L).customized(v -> v.setMasterDataSource(measurementSource))
            .withShelfLife(2, TimeInUnits.TimeUnit.YEAR, null).customized(v -> v.setMasterDataSource(supplierSource))
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
            .customized(v -> v.setMasterDataSource(supplierSource))
            .startServiceValues(1)
                .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 6L).customized(v -> v.setMasterDataSource(operatorSource))
                .with(KnownMdmParams.QUANTITY_IN_PACK, 5L).customized(v -> v.setMasterDataSource(operatorSource))
                .with(KnownMdmParams.RSL_IN_DAYS, 60L).customized(v -> v.setMasterDataSource(mdmRslSource))
                .with(KnownMdmParams.RSL_OUT_DAYS, 20L).customized(v -> v.setMasterDataSource(mdmRslSource))
            .endServiceValues()
            .startServiceValues(2)
                .with(KnownMdmParams.DELIVERY_TIME, 7L).customized(v -> v.setMasterDataSource(defaultSource))
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);

        var basicBlocks = sskuBlocks.getBasicBlocks();
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        var serviceBlocks2 = sskuBlocks.getServiceBlocks(2);

        DimensionsBlock measurementBlock = extractBlock(basicBlocks, ItemBlock.BlockType.DIMENSIONS);
        ValueCommentBlock supplierBlock1 = extractBlock(basicBlocks, ItemBlock.BlockType.SHELF_LIFE);
        MdmParamValueBlock<String> supplierBlock2 = extractBlock(basicBlocks, KnownMdmParams.MANUFACTURER_COUNTRY);
        TransportUnitBlock operatorBlock = extractBlock(serviceBlocks1, ItemBlock.BlockType.TRANSPORT_UNIT);
        RslBlock mdmRslBlock = extractBlock(serviceBlocks1, ItemBlock.BlockType.RSL);
        MdmParamValueBlock<BigDecimal> defaultBlock = extractBlock(serviceBlocks2, KnownMdmParams.DELIVERY_TIME);

        Assertions.assertThat(measurementBlock.getSource()).isEqualTo(measurementSource);
        Assertions.assertThat(supplierBlock1.getSource()).isEqualTo(supplierSource);
        Assertions.assertThat(supplierBlock2.getSource()).isEqualTo(supplierSource);
        Assertions.assertThat(operatorBlock.getSource()).isEqualTo(operatorSource);
        Assertions.assertThat(mdmRslBlock.getSource()).isEqualTo(mdmRslSource);
        Assertions.assertThat(defaultBlock.getSource()).isEqualTo(defaultSource);
    }

    @Test
    public void sourceTsIsPreserved() {
        Instant now = Instant.now();
        Instant old = now.minusSeconds(100);
        Instant newest = now.plusSeconds(100);

        var ssku = builder()
            .with(KnownMdmParams.WIDTH, 14L).customized(v -> v.setSourceUpdatedTs(old))
            .with(KnownMdmParams.HEIGHT, 14L).customized(v -> v.setSourceUpdatedTs(old))
            .with(KnownMdmParams.LENGTH, 14L).customized(v -> v.setSourceUpdatedTs(old))
            .with(KnownMdmParams.WEIGHT_GROSS, 14L).customized(v -> v.setSourceUpdatedTs(old))
            .withShelfLife(2, TimeInUnits.TimeUnit.YEAR, null).customized(v -> v.setSourceUpdatedTs(newest))
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай").customized(v -> v.setSourceUpdatedTs(now))
            .startServiceValues(1)
                .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 6L).customized(v -> v.setSourceUpdatedTs(old))
                .with(KnownMdmParams.QUANTITY_IN_PACK, 5L).customized(v -> v.setSourceUpdatedTs(newest))
                .with(KnownMdmParams.RSL_IN_DAYS, 60L).customized(v -> v.setSourceUpdatedTs(now))
                .with(KnownMdmParams.RSL_OUT_DAYS, 20L).customized(v -> v.setSourceUpdatedTs(old))
            .endServiceValues()
            .startServiceValues(2)
                .with(KnownMdmParams.DELIVERY_TIME, 7L).customized(v -> v.setSourceUpdatedTs(old))
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);

        var basicBlocks = sskuBlocks.getBasicBlocks();
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        var serviceBlocks2 = sskuBlocks.getServiceBlocks(2);

        DimensionsBlock oldBlock1 = extractBlock(basicBlocks, ItemBlock.BlockType.DIMENSIONS);
        ValueCommentBlock newestBlock1 = extractBlock(basicBlocks, ItemBlock.BlockType.SHELF_LIFE);
        MdmParamValueBlock<String> nowBlock1 = extractBlock(basicBlocks, KnownMdmParams.MANUFACTURER_COUNTRY);
        TransportUnitBlock newestBlock2 = extractBlock(serviceBlocks1, ItemBlock.BlockType.TRANSPORT_UNIT);
        RslBlock rslNowBlock2 = extractBlock(serviceBlocks1, ItemBlock.BlockType.RSL);
        MdmParamValueBlock<BigDecimal> oldBlock2 = extractBlock(serviceBlocks2, KnownMdmParams.DELIVERY_TIME);

        Assertions.assertThat(maxSourceTsFromValues(oldBlock1)).isEqualTo(old);
        Assertions.assertThat(maxSourceTsFromValues(oldBlock2)).isEqualTo(old);
        Assertions.assertThat(maxSourceTsFromValues(nowBlock1)).isEqualTo(now);
        Assertions.assertThat(maxSourceTsFromValues(newestBlock1)).isEqualTo(newest);
        Assertions.assertThat(maxSourceTsFromValues(newestBlock2)).isEqualTo(newest);

        // У ОСГ своя атмосфера - у них всё хранится не в параметрах, а в прото-полях, и там нет sourceUpdatedTs вообще.
        // Хорошие новости: единственный поставщик ОСГ - это мы сами, поэтому updatedTs можно считать source-ом.
        // Правда, тут нам это никак не помогает. Поскольку там всегда дефолт в Instant.now(), то хотя бы проверим,
        // что параметры "свежее", чем штамп на начало тестового прогона.
        Assertions.assertThat(maxSourceTsFromValues(rslNowBlock2)).isAfterOrEqualTo(now);
    }

    @Test
    public void updatedTsIsPreserved() {
        Instant now = Instant.now();
        Instant old = now.minusSeconds(1);
        Instant newest = now.plusSeconds(1);

        var ssku = builder()
            .with(KnownMdmParams.WIDTH, 14L).customized(v -> v.setUpdatedTs(old))
            .with(KnownMdmParams.HEIGHT, 14L).customized(v -> v.setUpdatedTs(old))
            .with(KnownMdmParams.LENGTH, 14L).customized(v -> v.setUpdatedTs(old))
            .with(KnownMdmParams.WEIGHT_GROSS, 14L).customized(v -> v.setUpdatedTs(old))
            .withShelfLife(2, TimeInUnits.TimeUnit.YEAR, null).customized(v -> v.setUpdatedTs(newest))
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай").customized(v -> v.setUpdatedTs(now))
            .startServiceValues(1)
                .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 6L).customized(v -> v.setUpdatedTs(old))
                .with(KnownMdmParams.QUANTITY_IN_PACK, 5L).customized(v -> v.setUpdatedTs(newest))
                .with(KnownMdmParams.RSL_IN_DAYS, 60L).customized(v -> v.setUpdatedTs(now))
                .with(KnownMdmParams.RSL_OUT_DAYS, 20L).customized(v -> v.setUpdatedTs(old))
            .endServiceValues()
            .startServiceValues(2)
                .with(KnownMdmParams.DELIVERY_TIME, 7L).customized(v -> v.setUpdatedTs(old))
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);

        var basicBlocks = sskuBlocks.getBasicBlocks();
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        var serviceBlocks2 = sskuBlocks.getServiceBlocks(2);

        DimensionsBlock oldBlock1 = extractBlock(basicBlocks, ItemBlock.BlockType.DIMENSIONS);
        ValueCommentBlock newestBlock1 = extractBlock(basicBlocks, ItemBlock.BlockType.SHELF_LIFE);
        MdmParamValueBlock<String> nowBlock1 = extractBlock(basicBlocks, KnownMdmParams.MANUFACTURER_COUNTRY);
        TransportUnitBlock newestBlock2 = extractBlock(serviceBlocks1, ItemBlock.BlockType.TRANSPORT_UNIT);
        RslBlock nowBlock2 = extractBlock(serviceBlocks1, ItemBlock.BlockType.RSL);
        MdmParamValueBlock<BigDecimal> oldBlock2 = extractBlock(serviceBlocks2, KnownMdmParams.DELIVERY_TIME);

        Assertions.assertThat(maxUpdatedTsFromValues(oldBlock1)).isEqualTo(old);
        Assertions.assertThat(maxUpdatedTsFromValues(oldBlock2)).isEqualTo(old);
        Assertions.assertThat(maxUpdatedTsFromValues(nowBlock1)).isEqualTo(now);
        Assertions.assertThat(maxUpdatedTsFromValues(newestBlock1)).isEqualTo(newest);
        Assertions.assertThat(maxUpdatedTsFromValues(newestBlock2)).isEqualTo(newest);
        // У ОСГ есть фиктивный параметр с датой, у которого рандом в ts. Потому явно проверим updatedTs через метод
        // блока. Кроме того, из-за хранения в прото там ts транкейтится до миллисекунд, потому сравнение через них.
        Assertions.assertThat(nowBlock2.getMaxUpdatedTs().toEpochMilli()).isEqualTo(now.toEpochMilli());
    }

    @Test
    public void partialWeightDimensionsSplitIntoDamagedBlocks() {
        // Мы хотим валидировать в том числе и частичные данные.
        // Потому поддерживаем блоки, собранные из половинчатых данных.
        var ssku = builder()
            .with(KnownMdmParams.WIDTH, 10L)
            .with(KnownMdmParams.WEIGHT_GROSS, 13L)
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);
        var basicBlocks = sskuBlocks.getBasicBlocks();

        DimensionsBlock vghBlock = extractBlock(basicBlocks, ItemBlock.BlockType.DIMENSIONS);
        Assertions.assertThat(vghBlock).isNotNull();
        Assertions.assertThat(vghBlock.getWidth().longValue()).isEqualTo(10L);
        Assertions.assertThat(vghBlock.getWeightGross().longValue()).isEqualTo(13L);
        Assertions.assertThat(vghBlock.getHeight()).isNull();
        Assertions.assertThat(vghBlock.getLength()).isNull();
    }

    @Test
    public void partialValueCommentsSplitIntoDamagedBlocks() {
        var ssku = builder()
            .with(KnownMdmParams.SHELF_LIFE, 1L)
            .with(KnownMdmParams.LIFE_TIME_UNIT, new MdmParamOption(2))
            .with(KnownMdmParams.GUARANTEE_PERIOD_COMMENT, "Comment")
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);
        var basicBlocks = sskuBlocks.getBasicBlocks();

        ValueCommentBlock shelfLifeBlock = extractBlock(basicBlocks, ItemBlock.BlockType.SHELF_LIFE);
        ValueCommentBlock lifeTimeBlock = extractBlock(basicBlocks, ItemBlock.BlockType.LIFE_TIME);
        ValueCommentBlock guaranteePeriodBlock = extractBlock(basicBlocks, ItemBlock.BlockType.GUARANTEE_PERIOD);

        Assertions.assertThat(shelfLifeBlock).isNotNull();
        Assertions.assertThat(lifeTimeBlock).isNotNull();
        Assertions.assertThat(guaranteePeriodBlock).isNotNull();

        // Особенность: конструктор таких блоков разрешает голый комментарий, но не разрешает голое число или юнит.
        // Поэтому у тех, что без коммента, здесь будет пустота, а у третьего с комментом - голый коммент.
        Assertions.assertThat(shelfLifeBlock.getMdmParamValues()).isEmpty();
        Assertions.assertThat(lifeTimeBlock.getMdmParamValues()).isEmpty();
        Assertions.assertThat(guaranteePeriodBlock.getMdmParamValues()).hasSize(1);
        Assertions.assertThat(guaranteePeriodBlock.getValueComment()).isEqualTo("Comment");
    }

    @Test
    public void partialTransportUnitSplitIntoDamagedBlocks() {
        var ssku = builder()
            .startServiceValues(1)
                .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 6L)
            .endServiceValues()
            .startServiceValues(2)
                .with(KnownMdmParams.QUANTITY_IN_PACK, 5L)
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        var serviceBlocks2 = sskuBlocks.getServiceBlocks(2);

        TransportUnitBlock transportUnitBlock1 = extractBlock(serviceBlocks1, ItemBlock.BlockType.TRANSPORT_UNIT);
        TransportUnitBlock transportUnitBlock2 = extractBlock(serviceBlocks2, ItemBlock.BlockType.TRANSPORT_UNIT);

        Assertions.assertThat(transportUnitBlock1).isNotNull();
        Assertions.assertThat(transportUnitBlock2).isNotNull();

        Assertions.assertThat(transportUnitBlock1.getTransportUnit().getTransportUnitSize()).isEqualTo(6);
        Assertions.assertThat(transportUnitBlock1.getTransportUnit().getQuantityInPack()).isEqualTo(0);
        Assertions.assertThat(transportUnitBlock2.getTransportUnit().getTransportUnitSize()).isEqualTo(0);
        Assertions.assertThat(transportUnitBlock2.getTransportUnit().getQuantityInPack()).isEqualTo(5);
    }

    @Test
    public void partialRslsSplitIntoDamagedBlocks() {
        var ssku = builder()
            .startServiceValues(1)
                .with(KnownMdmParams.RSL_OUT_DAYS, 20L)
            .endServiceValues()
            .build();

        SskuBlocks sskuBlocks = service.split(ssku);
        var serviceBlocks1 = sskuBlocks.getServiceBlocks(1);
        RslBlock rslBlock = extractBlock(serviceBlocks1, ItemBlock.BlockType.RSL);

        Assertions.assertThat(rslBlock).isNotNull();
        Assertions.assertThat(rslBlock.getMinOutboundLifetimeDay().get(0).getValue()).isEqualTo(20);
        Assertions.assertThat(rslBlock.getMinInboundLifetimeDay()).isEmpty();
        Assertions.assertThat(rslBlock.getMinInboundLifetimePercentage()).isEmpty();
        Assertions.assertThat(rslBlock.getMinOutboundLifetimePercentage()).isEmpty();
    }

    private CommonSskuBuilder builder() {
        return new CommonSskuBuilder(mdmParamCache, KEY);
    }

    @Nullable
    private static <T> T extractBlock(Collection<? extends ItemBlock> blocks, ItemBlock.BlockType type) {
        return (T) blocks.stream().filter(b -> b.getBlockType() == type).findAny().orElse(null);
    }

    @Nullable
    private static <T> T extractBlock(Collection<? extends ItemBlock> blocks, long mdmParamId) {
        return (T) blocks.stream()
            .filter(b -> b.getBlockType() == ItemBlock.BlockType.MDM_PARAM && b.getBlockSubtypeId() == mdmParamId)
            .findAny()
            .orElse(null);
    }

    private static Instant maxSourceTsFromValues(ItemBlock block) {
        return block.getMdmParamValues().stream().map(MdmParamValue::getSourceUpdatedTs)
            .filter(Objects::nonNull)
            .max(Instant::compareTo).get();
    }

    private static Instant maxUpdatedTsFromValues(ItemBlock block) {
        return block.getMdmParamValues().stream().map(MdmParamValue::getUpdatedTs).max(Instant::compareTo).get();
    }
}
