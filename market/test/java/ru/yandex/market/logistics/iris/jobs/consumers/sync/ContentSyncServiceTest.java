package ru.yandex.market.logistics.iris.jobs.consumers.sync;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifierWithChangedFields;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.complex.Barcode;
import ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource;
import ru.yandex.market.logistics.iris.core.index.complex.Barcodes;
import ru.yandex.market.logistics.iris.core.index.complex.Dimension;
import ru.yandex.market.logistics.iris.core.index.complex.Dimensions;
import ru.yandex.market.logistics.iris.core.index.complex.VendorCodes;
import ru.yandex.market.logistics.iris.core.index.field.FieldValue;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.core.index.implementation.Reference;
import ru.yandex.market.logistics.iris.core.index.json.mixin.ReferenceIndexImplMixin;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKeyWithChangedFields;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.logistics.iris.repository.JdbcItemRepository;
import ru.yandex.market.logistics.iris.service.index.ReferenceIndexMergeService;
import ru.yandex.market.logistics.iris.service.item.put.PutReferenceItemsService;
import ru.yandex.market.logistics.iris.service.mbo.SupplierContentMapping;
import ru.yandex.market.logistics.iris.service.mbo.SupplierMappingService;
import ru.yandex.market.logistics.iris.util.UtcTimestampProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContentSyncServiceTest {

    @Test
    public void shouldDoNothingIfBatchDoesNotContainAnyUnit() {
        SupplierMappingService supplierMappingService = Mockito.mock(SupplierMappingService.class);
        UtcTimestampProvider utcTimestampProvider = Mockito.mock(UtcTimestampProvider.class);


        ChangeTrackingReferenceIndexer changeTrackingReferenceIndexer =
                Mockito.mock(ChangeTrackingReferenceIndexer.class);
        ReferenceIndexMergeService referenceIndexMergeService = Mockito.mock(ReferenceIndexMergeService.class);
        PutReferenceItemsService putReferenceItemsService = Mockito.mock(PutReferenceItemsService.class);
        JdbcItemRepository jdbcItemRepository = Mockito.mock(JdbcItemRepository.class);

        ContentSyncService contentSyncService = createContentSyncServiceStub(
                supplierMappingService,
                changeTrackingReferenceIndexer,
                utcTimestampProvider,
                referenceIndexMergeService,
                putReferenceItemsService,
                jdbcItemRepository);

        contentSyncService.doProcess(ImmutableSet.of());

        Mockito.verifyZeroInteractions(supplierMappingService, utcTimestampProvider, changeTrackingReferenceIndexer,
                referenceIndexMergeService, putReferenceItemsService);
    }

    @Test
    public void shouldCreateAndMergeEmptyValues() {
        SupplierMappingService supplierMappingService = Mockito.mock(SupplierMappingService.class);
        List<EmbeddableItemIdentifier> itemIdentifiersToFoundMappingFor =
                ImmutableList.of(new EmbeddableItemIdentifier("partner_id", "partner_sku"));
        Mockito.when(supplierMappingService.getMarketSkuMapping(itemIdentifiersToFoundMappingFor))
                .thenReturn(Collections.emptyMap());
        ZonedDateTime zonedDateTimeToSetInFieldValues = ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.UTC);
        UtcTimestampProvider utcTimestampProvider = mockUtcTimestampProvider(zonedDateTimeToSetInFieldValues);

        ChangeTrackingReferenceIndexer changeTrackingReferenceIndexer = mockChangeTrackingReferenceIndexer();
        ReferenceIndexMergeService referenceIndexMergeService = Mockito.spy(ReferenceIndexMergeService.class);
        Mockito.when(referenceIndexMergeService.merge(ArgumentMatchers.anyMap())).thenReturn(Collections.emptyList());
        PutReferenceItemsService putReferenceItemsService = Mockito.mock(PutReferenceItemsService.class);
        JdbcItemRepository jdbcItemRepository = Mockito.mock(JdbcItemRepository.class);

        ContentSyncService contentSyncService = createContentSyncServiceStub(
                supplierMappingService,
                changeTrackingReferenceIndexer,
                utcTimestampProvider,
                referenceIndexMergeService,
                putReferenceItemsService,
                jdbcItemRepository);

        contentSyncService.doProcess(ImmutableSet.of(new ItemIdentifierDTO("partner_id", "partner_sku")));

        Mockito.verify(supplierMappingService).getMarketSkuMapping(ImmutableList.of(new EmbeddableItemIdentifier(
                "partner_id", "partner_sku")));
        Mockito.verify(utcTimestampProvider, Mockito.never()).getCurrentUtcTimestamp();
        Mockito.verify(changeTrackingReferenceIndexer).createEmptyIndex();

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(referenceIndexMergeService).merge(argumentCaptor.capture());
        Map<EmbeddableItemNaturalKey, ChangeTrackingReferenceIndex> actualValuesToMerge = argumentCaptor.getValue();
        Assert.assertNotNull(actualValuesToMerge);
        Assert.assertEquals(1, actualValuesToMerge.size());

        EmbeddableItemNaturalKey expectedItemNaturalKey = new EmbeddableItemNaturalKey(
                new EmbeddableItemIdentifier("partner_id", "partner_sku"),
                new EmbeddableSource("1", SourceType.CONTENT)
        );

        Assert.assertTrue(actualValuesToMerge.containsKey(expectedItemNaturalKey));

        ChangeTrackingReferenceIndex actualIndexToMerge = actualValuesToMerge.get(expectedItemNaturalKey);

        Assert.assertNotNull(actualIndexToMerge);

        assertEquals(0, actualIndexToMerge.getFields().size());

        Mockito.verifyZeroInteractions(putReferenceItemsService);
    }

    @Test
    public void shouldCreateAndMergeValues() {

        EmbeddableItemNaturalKey embeddableItemNaturalKey =
                new EmbeddableItemNaturalKey(new EmbeddableItemIdentifier("partner_id", "partner_sku"), null);

        SupplierMappingService supplierMappingService = mockSupplierMappingService();

        ZonedDateTime zonedDateTimeToSetInFieldValues = ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.UTC);
        UtcTimestampProvider utcTimestampProvider = mockUtcTimestampProvider(zonedDateTimeToSetInFieldValues);

        ChangeTrackingReferenceIndexer changeTrackingReferenceIndexer = mockChangeTrackingReferenceIndexer();
        ReferenceIndexMergeService referenceIndexMergeService = Mockito.spy(ReferenceIndexMergeService.class);
        Mockito.when(referenceIndexMergeService.merge(ArgumentMatchers.anyMap()))
                .thenReturn(Collections.singletonList(
                        new EmbeddableItemNaturalKeyWithChangedFields(embeddableItemNaturalKey,
                                Collections.emptyList()))
                );
        PutReferenceItemsService putReferenceItemsService = Mockito.mock(PutReferenceItemsService.class);
        JdbcItemRepository jdbcItemRepository = Mockito.mock(JdbcItemRepository.class);

        ContentSyncService contentSyncService = createContentSyncServiceStub(
                supplierMappingService,
                changeTrackingReferenceIndexer,
                utcTimestampProvider,
                referenceIndexMergeService,
                putReferenceItemsService,
                jdbcItemRepository);

        contentSyncService.doProcess(ImmutableSet.of(new ItemIdentifierDTO("partner_id", "partner_sku")));

        Mockito.verify(supplierMappingService).getMarketSkuMapping(ImmutableList.of(new EmbeddableItemIdentifier(
                "partner_id", "partner_sku")));
        Mockito.verify(utcTimestampProvider).getCurrentUtcTimestamp();
        Mockito.verify(changeTrackingReferenceIndexer).createEmptyIndex();

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(referenceIndexMergeService).merge(argumentCaptor.capture());
        Map<EmbeddableItemNaturalKey, ChangeTrackingReferenceIndex> actualValuesToMerge = argumentCaptor.getValue();

        Assert.assertNotNull(actualValuesToMerge);
        Assert.assertEquals(1, actualValuesToMerge.size());

        EmbeddableItemNaturalKey expectedItemNaturalKey = new EmbeddableItemNaturalKey(
                new EmbeddableItemIdentifier("partner_id", "partner_sku"),
                new EmbeddableSource("1", SourceType.CONTENT)
        );

        Mockito.verify(putReferenceItemsService).putAsync(
                Collections.singletonList(
                        new ItemIdentifierWithChangedFields(
                                ItemIdentifier.of("partner_id", "partner_sku"), Collections.emptyList()
                        )
                )
        );

        Assert.assertTrue(actualValuesToMerge.containsKey(expectedItemNaturalKey));

        ChangeTrackingReferenceIndex actualIndexToMerge = actualValuesToMerge.get(expectedItemNaturalKey);

        Assert.assertNotNull(actualIndexToMerge);

        Optional<FieldValue<Long>> actualMsku = actualIndexToMerge.getFieldValue(PredefinedFields.MSKU_FIELD);
        Assert.assertTrue(actualMsku.isPresent());
        Assert.assertEquals(new Long(6), actualMsku.get().getValue());
        Assert.assertEquals(zonedDateTimeToSetInFieldValues, actualMsku.get().getUtcTimestamp());

        Optional<FieldValue<String>> actualName = actualIndexToMerge.getFieldValue(PredefinedFields.NAME_FIELD);
        Assert.assertTrue(actualName.isPresent());
        Assert.assertEquals("marketName", actualName.get().getValue());
        Assert.assertEquals(zonedDateTimeToSetInFieldValues, actualName.get().getUtcTimestamp());

        Optional<FieldValue<VendorCodes>> actualVendorCodes =
                actualIndexToMerge.getFieldValue(PredefinedFields.VENDOR_CODES_FIELD);
        Assert.assertTrue(actualVendorCodes.isPresent());
        Assert.assertNotNull(actualVendorCodes.get().getValue());
        Assert.assertEquals(ImmutableList.of("1111122222", "333334444"),
                actualVendorCodes.get().getValue().getVendorCodes());
        Assert.assertEquals(zonedDateTimeToSetInFieldValues, actualVendorCodes.get().getUtcTimestamp());

        Optional<FieldValue<Barcodes>> actualBarcodes = actualIndexToMerge.getFieldValue(PredefinedFields.BARCODES);
        Assert.assertTrue(actualBarcodes.isPresent());
        Assert.assertNotNull(actualBarcodes.get());
        Assert.assertEquals(Barcodes.of(ImmutableList.of(new Barcode("marketBarcodes", null, BarcodeSource.UNKNOWN)))
                , actualBarcodes.get().getValue());
        Assert.assertEquals(zonedDateTimeToSetInFieldValues, actualBarcodes.get().getUtcTimestamp());

        Optional<FieldValue<Boolean>> actualHasLifeTime =
                actualIndexToMerge.getFieldValue(PredefinedFields.HAS_LIFETIME_FIELD);
        Assert.assertTrue(actualHasLifeTime.isPresent());
        Assert.assertTrue(actualHasLifeTime.get().getValue());
        Assert.assertEquals(zonedDateTimeToSetInFieldValues, actualHasLifeTime.get().getUtcTimestamp());

        Optional<FieldValue<Integer>> actualBoxCapacity =
                actualIndexToMerge.getFieldValue(PredefinedFields.BOX_CAPACITY_FIELD);
        Assert.assertTrue(actualBoxCapacity.isPresent());
        Assert.assertEquals(new Integer(9), actualBoxCapacity.get().getValue());
        Assert.assertEquals(zonedDateTimeToSetInFieldValues, actualBoxCapacity.get().getUtcTimestamp());

        Optional<FieldValue<Integer>> actualBoxCount =
                actualIndexToMerge.getFieldValue(PredefinedFields.BOX_COUNT_FIELD);
        Assert.assertTrue(actualBoxCount.isPresent());
        Assert.assertEquals(new Integer(10), actualBoxCount.get().getValue());
        Assert.assertEquals(zonedDateTimeToSetInFieldValues, actualBoxCount.get().getUtcTimestamp());

        Optional<FieldValue<Dimension>> actualWeightGross =
                actualIndexToMerge.getFieldValue(PredefinedFields.WEIGHT_GROSS);
        Assert.assertFalse(actualWeightGross.isPresent());

        Optional<FieldValue<Dimension>> actualWeightNett =
                actualIndexToMerge.getFieldValue(PredefinedFields.WEIGHT_NETT);
        Assert.assertFalse(actualWeightNett.isPresent());

        Optional<FieldValue<Dimension>> actualWeightTare =
                actualIndexToMerge.getFieldValue(PredefinedFields.WEIGHT_TARE);
        Assert.assertFalse(actualWeightTare.isPresent());

        Optional<FieldValue<Dimensions>> actualDimensions =
                actualIndexToMerge.getFieldValue(PredefinedFields.DIMENSIONS);
        Assert.assertFalse(actualDimensions.isPresent());

        Optional<FieldValue<Integer>> actualLifetime =
                actualIndexToMerge.getFieldValue(PredefinedFields.LIFETIME_DAYS_FIELD);
        Assert.assertFalse(actualLifetime.isPresent());
    }

    private SupplierMappingService mockSupplierMappingService() {
        SupplierMappingService supplierMappingService = Mockito.mock(SupplierMappingService.class);
        List<EmbeddableItemIdentifier> itemIdentifiersToFoundMappingFor =
                ImmutableList.of(new EmbeddableItemIdentifier("partner_id", "partner_sku"));
        Map<ItemIdentifierDTO, SupplierContentMapping> mappingFromContent = ImmutableMap.of(
                new ItemIdentifierDTO("partner_id", "partner_sku"),
                SupplierContentMapping.builder("supplierSku", 6L, "name")
                        .setMarketName("marketName")
                        .setMarketVendorCodes(ImmutableList.of("1111122222", "333334444"))
                        .setMarketBarcodes(ImmutableList.of("marketBarcodes"))
                        .setHasExpirationDate(true)
                        .setPackageNumInSpike(9)
                        .setBoxCount(10)
                        .build()
        );
        Mockito.when(supplierMappingService.getMarketSkuMapping(itemIdentifiersToFoundMappingFor)).thenReturn(mappingFromContent);
        return supplierMappingService;
    }

    private UtcTimestampProvider mockUtcTimestampProvider(ZonedDateTime zonedDateTimeToSetInFieldValues) {
        UtcTimestampProvider utcTimestampProvider = Mockito.mock(UtcTimestampProvider.class);
        Mockito.when(utcTimestampProvider.getCurrentUtcTimestamp()).thenReturn(zonedDateTimeToSetInFieldValues);
        return utcTimestampProvider;
    }

    private ChangeTrackingReferenceIndexer mockChangeTrackingReferenceIndexer() {
        ChangeTrackingReferenceIndexer changeTrackingReferenceIndexer =
                Mockito.mock(ChangeTrackingReferenceIndexer.class);
        Mockito.when(changeTrackingReferenceIndexer.createEmptyIndex()).thenReturn(new ReferenceIndexImplMixin(new Reference()));
        return changeTrackingReferenceIndexer;
    }

    private ContentSyncService createContentSyncServiceStub(SupplierMappingService supplierMappingService,
                                                            ChangeTrackingReferenceIndexer indexer,
                                                            UtcTimestampProvider utcTimestampProvider,
                                                            ReferenceIndexMergeService indexMergeService,
                                                            PutReferenceItemsService putReferenceItemsService,
                                                            JdbcItemRepository jdbcItemRepository) {
        return new ContentSyncService(supplierMappingService, indexer, utcTimestampProvider, indexMergeService, putReferenceItemsService,
                jdbcItemRepository);
    }
}
