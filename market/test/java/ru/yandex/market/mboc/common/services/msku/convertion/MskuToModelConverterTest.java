package ru.yandex.market.mboc.common.services.msku.convertion;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.db.jooq.generated.msku.enums.SkuQualityEnum;
import ru.yandex.market.mboc.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.mboc.common.repo.bindings.proto.ByteArrayToModelStorageParameterValueConverter;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;

public class MskuToModelConverterTest {

    private static final long SEED = 10062123;

    private static final int RETRY_COUNT = 100;

    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .overrideDefaultInitialization(true)
        .build();

    @Test
    public void shouldConvertSimpleNotNullFields() {
        for (int i = 0; i < RETRY_COUNT; i++) {
            Msku msku = nextMsku();

            Assertions.assertThat(
                MskuToModelConverter.convertSimple(msku))
                .isEqualTo(new SimpleModel()
                    .setId(msku.getMarketSkuId())
                    .setCategoryId(msku.getCategoryId())
                    .setVendorId(Math.toIntExact(msku.getVendorId()))
                    .setModelType(Model.ModelType.valueOf(msku.getSkuType().getLiteral()))
                    .setDeleted(msku.getDeleted())
                    .setSkuParentModelId(msku.getParentModelId())
                    .setCreatedTs(msku.getCreationTs())
                    .setModifiedTs(msku.getModificationTs())
                    .setSkuModel(msku.getIsSku()));
        }
    }

    @Test
    public void shouldConvertNotNullFields() {
        for (int i = 0; i < RETRY_COUNT; i++) {
            Msku msku = nextMsku();

            Assertions.assertThat(
                MskuToModelConverter.convert(msku))
                .isEqualTo(new Model()
                    .setId(msku.getMarketSkuId())
                    .setCategoryId(msku.getCategoryId())
                    .setVendorId(Math.toIntExact(msku.getVendorId()))
                    .setTitle(msku.getTitle())
                    .setModelType(Model.ModelType.valueOf(msku.getSkuType().getLiteral()))
                    .setDeleted(msku.getDeleted())
                    .setSkuParentModelId(msku.getParentModelId())
                    .setPublishedOnMarket(msku.getPublishedOnMarket())
                    .setPublishedOnBlueMarket(msku.getPublishedOnBlueMarket())
                    .setPictureUrl(msku.getPictureUrl())
                    .setCreatedTs(msku.getCreationTs())
                    .setModifiedTs(msku.getModificationTs())
                    .setSkuModel(msku.getIsSku())
                    .setSupplierId(msku.getSupplierId())
                    .setVendorCodes(msku.getVendorCodes() == null ? null : Arrays.asList(msku.getVendorCodes()))
                    .setBarCodes(msku.getBarCodes() == null ? null : Arrays.asList(msku.getBarCodes()))
                    .setParameterValues(
                        ByteArrayToModelStorageParameterValueConverter.INSTANCE.from(msku.getParameterValuesProto())));
        }
    }

    private Msku nextMsku() {
        long id = random.nextLong();
        long categoryId = random.nextLong();
        int vendorId = random.nextInt();
        String title = random.nextObject(String.class);
        String type = "SKU";
        String quality = "OPERATOR";
        boolean deleted = random.nextBoolean();
        long parentModelId = random.nextLong();
        boolean publishedOnMarket = random.nextBoolean();
        boolean publishedOnBlueMarket = random.nextBoolean();
        String pictureUrl = random.nextObject(String.class);
        Instant createdTs = Instant.now();
        Instant modifiedTs = Instant.now();
        boolean isSku = random.nextBoolean();
        long supplierId = random.nextLong();
        String[] vendorCodes = new String[]{random.nextObject(String.class), random.nextObject(String.class)};
        String[] barCodes = new String[]{random.nextObject(String.class), random.nextObject(String.class)};
        Long[] cargoIds = new Long[]{random.nextLong(), random.nextLong(), random.nextLong()};

        ModelStorage.ParameterValue paramterValue = ModelStorage.ParameterValue.newBuilder()
            .setXslName(random.nextObject(String.class))
            .setBoolValue(random.nextBoolean())
            .setParamId(random.nextLong())
            .setOptionId(random.nextInt())
            .setRuleId(random.nextInt())
            .setValueSource(random.nextObject(ModelStorage.ModificationSource.class))
            .setValueType(random.nextObject(MboParameters.ValueType.class))
            .build();

        return new Msku()
            .setMarketSkuId(id)
            .setCategoryId(categoryId)
            .setVendorId((long) vendorId)
            .setTitle(title)
            .setSkuType(SkuTypeEnum.valueOf(type))
            .setSkuQuality(SkuQualityEnum.valueOf(quality))
            .setDeleted(deleted)
            .setParentModelId(parentModelId)
            .setPublishedOnMarket(publishedOnMarket)
            .setPublishedOnBlueMarket(publishedOnBlueMarket)
            .setPictureUrl(pictureUrl)
            .setCreationTs(createdTs)
            .setModificationTs(modifiedTs)
            .setIsSku(isSku)
            .setSupplierId(supplierId)
            .setVendorCodes(vendorCodes)
            .setBarCodes(barCodes)
            .setCargoTypeLmsIds(cargoIds)
            .setParameterValuesProto(
                ByteArrayToModelStorageParameterValueConverter.INSTANCE
                    .to(Collections.singletonList(paramterValue)));
    }
}
