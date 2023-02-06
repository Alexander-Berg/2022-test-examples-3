package ru.yandex.market.logistics.iris.service.mdm.conversion;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.configuration.protobuf.ProtobufMapper;
import ru.yandex.market.logistics.iris.core.domain.target.TargetType;
import ru.yandex.market.logistics.iris.core.index.ImmutableReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.model.ItemDTO;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.logistics.iris.model.ItemNaturalKeyDTO;
import ru.yandex.market.logistics.iris.model.SourceDTO;
import ru.yandex.market.logistics.iris.model.TargetDTO;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyBooleanKey;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;
import ru.yandex.market.logistics.iris.util.UtcTimestampProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;

import static ru.yandex.market.logistics.iris.model.TargetDTO.DEFAULT_TARGET;
import static ru.yandex.market.logistics.iris.service.mdm.conversion.MdmToIrisConversionService.MDM_SOURCE;

public class MdmToIrisConversionServiceTest extends AbstractContextualTest {

    private static final ZonedDateTime UPDATED_DATE_TIME = ZonedDateTime.of(
            LocalDate.of(1970, 1, 2).atStartOfDay(),
            ZoneOffset.UTC
    );

    @Autowired
    ProtobufMapper protobufMapper;

    @Autowired
    private MdmToIrisConversionService mdmToIrisConversionService;

    @Autowired
    private ChangeTrackingReferenceIndexer referenceIndexer;

    @SpyBean
    private UtcTimestampProvider utcTimestampProvider;

    @SpyBean
    private SystemPropertyService systemPropertyService;

    /**
     * Проверяем, что пустой MDM CompleteItem не будет преобразован в заполненную коллекцию ItemDTO.
     */
    @Test
    public void convertEmptyCompleteItem() {
        MdmIrisPayload.CompleteItem completeItem = MdmIrisPayload.CompleteItem.newBuilder().build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromCompleteItem(completeItem);

        assertions().assertThat(result).isEmpty();
    }

    /**
     * Проверяем, что MDM CompleteItem только с идентификатором товара
     * будет преобразован в заполненную коллекцию c одним ItemDTO.
     */
    @Test
    public void convertEmptyCompleteItemWithItemIdentifier() {
        MdmIrisPayload.CompleteItem completeItem = MdmIrisPayload.CompleteItem.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setShopSku("ssku").setSupplierId(10))
                .build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromCompleteItem(completeItem);

        assertions().assertThat(result).hasSize(1);
    }

    /**
     * Проверяем, что MDM CompleteItem с идентификатором товара и набором из пустых данных
     * будет преобразован в заполненную коллекцию ItemDTO.
     */
    @Test
    public void convertEmptyCompleteItemWithItemIdAndEmptyInformation() {
        MdmIrisPayload.CompleteItem completeItem = MdmIrisPayload.CompleteItem.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setShopSku("ssku").setSupplierId(10))
                .addTrustworthyInformation(MdmIrisPayload.ReferenceInformation.newBuilder().build())
                .addRemainingInformation(MdmIrisPayload.ReferenceInformation.newBuilder().build())
                .build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromCompleteItem(completeItem);

        assertions().assertThat(result.size()).isEqualTo(1);

        for (ItemDTO actualItem : result) {
            ImmutableReferenceIndex actualImmutableReferenceIndex = actualItem.getReferenceIndex();
            assertions().assertThat(actualImmutableReferenceIndex.getFields())
                    .containsExactlyInAnyOrder(PredefinedFields.TRUSTWORTHY_VERSION_FIELD);

            ItemNaturalKeyDTO actualNaturalKey = actualItem.getNaturalKey();

            ItemIdentifierDTO actualItemIdentifier = actualNaturalKey.getItemIdentifier();
            assertions().assertThat(actualItemIdentifier).isEqualTo(new ItemIdentifierDTO("10", "ssku"));

            SourceDTO actualSource = actualNaturalKey.getSource();
            assertions().assertThat(actualSource).isEqualTo(MDM_SOURCE);

            TargetDTO actualTarget = actualNaturalKey.getTarget();
            assertions().assertThat(actualTarget).isEqualTo(DEFAULT_TARGET);
        }
    }

    /**
     * Проверяем игнорирование information с источником типа Supplier в CompleteItem.
     */
    @Test
    public void convertCompleteItemIgnoresSupplierInformation() {
        MdmIrisPayload.ShippingUnit.Builder warehouseShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
                .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(1L).setUpdatedTs(1L))
                .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(2L).setUpdatedTs(1L))
                .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(3L).setUpdatedTs(1L))
                .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(4L).setUpdatedTs(1L));

        MdmIrisPayload.ShippingUnit.Builder supplierShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
                .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(5L).setUpdatedTs(1L));
        // 1. If MDM sends warehouse and supplier information, we should ignore supplier information
        MdmIrisPayload.CompleteItem completeItemWithWarehouseAndSupplierInfo = MdmIrisPayload.CompleteItem.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setShopSku("ssku").setSupplierId(10))
                .addTrustworthyInformation(MdmIrisPayload.ReferenceInformation.newBuilder()
                        .setSource(MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE))
                        .setItemShippingUnit(warehouseShippingUnit))
                .addTrustworthyInformation(MdmIrisPayload.ReferenceInformation.newBuilder()
                        .setSource(MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.SUPPLIER))
                        .setItemShippingUnit(supplierShippingUnit))
                .addRemainingInformation(MdmIrisPayload.ReferenceInformation.newBuilder().setVersionId(16).build())
                .build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromCompleteItem(
                completeItemWithWarehouseAndSupplierInfo);
        assertions().assertThat(result.size()).isEqualTo(1);
        ItemDTO item = result.iterator().next();
        assertions().assertThat(item.getNaturalKey().getSource()).isEqualTo(MDM_SOURCE);
        assertions().assertThat(item.getReferenceIndex().getFields())
                .containsExactlyInAnyOrder(PredefinedFields.WEIGHT_GROSS, PredefinedFields.DIMENSIONS,
                        PredefinedFields.TRUSTWORTHY_VERSION_FIELD);

        // 2. If MDM sends only supplier information, we should treat it as a CompleteItem with no information at all
        MdmIrisPayload.CompleteItem completeItemWithOnlySupplierInfo = MdmIrisPayload.CompleteItem.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setShopSku("ssku").setSupplierId(10))
                .addTrustworthyInformation(MdmIrisPayload.ReferenceInformation.newBuilder()
                        .setSource(MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.SUPPLIER))
                        .setItemShippingUnit(supplierShippingUnit))
                .addRemainingInformation(MdmIrisPayload.ReferenceInformation.newBuilder().setVersionId(17).build())
                .build();

        result = mdmToIrisConversionService.fromCompleteItem(completeItemWithOnlySupplierInfo);

        assertions().assertThat(result.size()).isEqualTo(1);
        item = result.iterator().next();
        assertions().assertThat(item.getNaturalKey().getSource()).isEqualTo(MDM_SOURCE);
        assertions().assertThat(item.getReferenceIndex().getFields()).isEmpty();
    }

    /**
     * Проверяем сценарий c конвертацией ОСГ и ВГХ.
     * <p>
     * Данные по ОСГ должны взяться только из trustworthy_information по типу записи MDM,
     * ВГХ должны взяться только из trustworthy_information по типу записи WAREHOUSE.
     */
    @Test
    public void convertItemsWithRemainingLifetime() {
        MdmIrisPayload.ShippingUnit.Builder warehouseShippingUnit = MdmIrisPayload.ShippingUnit.newBuilder()
                .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(1L).setUpdatedTs(1L))
                .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(2L).setUpdatedTs(1L))
                .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(3L).setUpdatedTs(1L))
                .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(4L).setUpdatedTs(1L));

        MdmIrisPayload.RemainingLifetime.Builder inboundDays = MdmIrisPayload.RemainingLifetime.newBuilder()
                .setValue(15)
                .setUpdatedTs(UPDATED_DATE_TIME.toInstant().toEpochMilli());

        MdmIrisPayload.CompleteItem completeItemWithWarehouseAndSupplierInfo = MdmIrisPayload.CompleteItem.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setShopSku("ssku").setSupplierId(10))
                .addTrustworthyInformation(MdmIrisPayload.ReferenceInformation.newBuilder()
                        .setSource(MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM))
                        .addMinInboundLifetimeDay(inboundDays.build()))
                .addTrustworthyInformation(MdmIrisPayload.ReferenceInformation.newBuilder()
                        .setSource(MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE))
                        .setItemShippingUnit(warehouseShippingUnit))
                .addRemainingInformation(MdmIrisPayload.ReferenceInformation.newBuilder().build())
                .build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromCompleteItem(
                completeItemWithWarehouseAndSupplierInfo);
        assertions().assertThat(result.size()).isEqualTo(1);
        ItemDTO item = result.iterator().next();
        assertions().assertThat(item.getNaturalKey().getSource()).isEqualTo(MDM_SOURCE);
        assertions().assertThat(item.getReferenceIndex().getFields())
                .containsExactlyInAnyOrder(
                        PredefinedFields.WEIGHT_GROSS,
                        PredefinedFields.DIMENSIONS,
                        PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_DAY_FIELD,
                        PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                        PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_DAY_FIELD,
                        PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                        PredefinedFields.TRUSTWORTHY_VERSION_FIELD
                );
    }

    /**
     * Проверяем сценарий с полноценной конвертацией.
     * <p>
     * Данные должны взяться только из trustworthy_information в полном объеме.
     * На выходе должен получиться объект ItemDTO с корректно заполненными 2умя ReferenceIndex'ами.
     */
    @Test
    public void fullConversionOfCompleteItem() throws IOException {
        Mockito.doReturn(UPDATED_DATE_TIME).when(utcTimestampProvider).getCurrentUtcTimestamp();

        String completeItemJson = extractFileContent("fixtures/data/mdm_to_iris_conversion/mdm_complete_item.json");
        MdmIrisPayload.CompleteItem.Builder builder = MdmIrisPayload.CompleteItem.newBuilder();

        protobufMapper.mergeJson(completeItemJson, builder);
        MdmIrisPayload.CompleteItem completeItem = builder.build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromCompleteItem(completeItem);

        assertFullConversionResults(
                result,
                "fixtures/data/mdm_to_iris_conversion/mdm.json",
                "fixtures/data/mdm_to_iris_conversion/mdm_wh_145.json"
        );
    }

    /**
     * Проверяем, что пустой MDM Item не будет преобразован в корректный набор ItemDTO.
     */
    @Test
    public void convertEmptyItem() {
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder().build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromItem(item);

        assertions().assertThat(result).isEmpty();
    }

    /**
     * Проверяем сценарий с полноценной конвертацией.
     * <p>
     * Данные должны взяться из information в полном объеме.
     * На выходе должен получиться объект ItemDTO с корректно заполненными 2умя ReferenceIndex'ами.
     */
    @Test
    public void fullConversionOfItem() throws IOException {
        Mockito.doReturn(UPDATED_DATE_TIME).when(utcTimestampProvider).getCurrentUtcTimestamp();

        String mdmItemJson = extractFileContent("fixtures/data/mdm_to_iris_conversion/mdm_item.json");
        MdmIrisPayload.Item.Builder builder = MdmIrisPayload.Item.newBuilder();

        protobufMapper.mergeJson(mdmItemJson, builder);
        MdmIrisPayload.Item item = builder.build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromItem(item);

        assertFullConversionResults(
                result,
                "fixtures/data/mdm_to_iris_conversion/mdm_with_measurement_state.json",
                "fixtures/data/mdm_to_iris_conversion/mdm_wh_145_with_measurement_state.json"
        );
    }

    /**
     * Проверяем сценарий конвертации с отсутствием ReferenceIndex.
     * <p>
     * Данные information отсутствуют.
     * На выходе должен получиться один объект ItemDTO с пустым ReferenceIndex'ом.
     */
    @Test
    public void convertItemWithoutInformation() {
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setShopSku("ssku").setSupplierId(10))
                .addInformation(MdmIrisPayload.ReferenceInformation.newBuilder().build())
                .build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromItem(item);

        assertions().assertThat(result).hasSize(1);

        Optional<ItemDTO> resultItem = result.stream().findFirst();

        assertions().assertThat(resultItem).isPresent();

        ItemIdentifierDTO itemIdentifier = resultItem.get().getNaturalKey().getItemIdentifier();

        assertions().assertThat(itemIdentifier.getPartnerId()).isEqualTo("10");
        assertions().assertThat(itemIdentifier.getPartnerSku()).isEqualTo("ssku");

        ImmutableReferenceIndex mdmEmptyInfo = resultItem.get().getReferenceIndex();
        String mdmEmptyInfoJson = mdmEmptyInfo.toJson(referenceIndexer);

        assertions().assertThat(mdmEmptyInfoJson)
                .isEqualTo("{\"trustworthy_version\":{\"value\":0,\"utcTimestamp\":\"1970-01-01T00:00:00.001\"}}");
    }

    /**
     * Проверяем сценарий с полноценной конвертацией и игнорированием данных по source_id.
     * <p>
     * Данные должны взяться только из trustworthy_information в полном объеме.
     * На выходе должен получиться объект ItemDTO с корректно заполненными 2умя ReferenceIndex'ами.
     * Все source_id, где есть ключ с типом supplier игнорируются
     */
    @Test
    public void fullConversionOfCompleteItemWithIgnoringSupplierInSourceId() throws IOException {
        Mockito.doReturn(UPDATED_DATE_TIME).when(utcTimestampProvider).getCurrentUtcTimestamp();

        String completeItemJson = extractFileContent(
                "fixtures/data/mdm_to_iris_conversion/mdm_complete_item_with_ignoring_supplier_in_source_id.json");
        MdmIrisPayload.CompleteItem.Builder builder = MdmIrisPayload.CompleteItem.newBuilder();

        protobufMapper.mergeJson(completeItemJson, builder);
        MdmIrisPayload.CompleteItem completeItem = builder.build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromCompleteItem(completeItem);

        assertFullConversionResults(
                result,
                "fixtures/data/mdm_to_iris_conversion/mdm.json",
                "fixtures/data/mdm_to_iris_conversion/mdm_wh_145.json"
        );
    }

    /**
     * Проверяем сценарий с полноценной конвертацией и отключеным игнорированием данных по source_id.
     * <p>
     * Данные должны взяться только из trustworthy_information в полном объеме.
     * На выходе должен получиться объект ItemDTO с корректно заполненным  ReferenceIndex'ом.
     * Все source_id, где есть ключ с типом supplier не игнорируются
     */
    @Test
    public void shouldConversionOfCompleteItemWithOutIgnoringSupplierInSourceId() throws IOException {
        Mockito.doReturn(false)
                .when(systemPropertyService)
                .getBooleanProperty(SystemPropertyBooleanKey.ENABLE_FILTERING_RECORDS_BY_SUPPLIER_SOURCE);
        Mockito.doReturn(UPDATED_DATE_TIME).when(utcTimestampProvider).getCurrentUtcTimestamp();

        String completeItemJson = extractFileContent(
                "fixtures/data/mdm_to_iris_conversion/mdm_complete_item_with_out_ignoring_supplier_in_source_id.json");
        MdmIrisPayload.CompleteItem.Builder builder = MdmIrisPayload.CompleteItem.newBuilder();

        protobufMapper.mergeJson(completeItemJson, builder);
        MdmIrisPayload.CompleteItem completeItem = builder.build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromCompleteItem(completeItem);

        assertions().assertThat(result).hasSize(1);

        Optional<ItemDTO> actualItemDTO = result.stream().findFirst();

        actualItemDTO.ifPresent(item -> {
            ItemIdentifierDTO itemIdentifier = item.getNaturalKey().getItemIdentifier();
            assertions().assertThat(itemIdentifier.getPartnerId()).isEqualTo("123");
            assertions().assertThat(itemIdentifier.getPartnerSku()).isEqualTo("psku");

            ImmutableReferenceIndex mdmInfo = item.getReferenceIndex();

            String mdmInfoJson = mdmInfo.toJson(referenceIndexer);

            String expectedMdmInfoJson = extractFileContent(
                    "fixtures/expected/mdm_to_iris_conversion" +
                            "/mdm_complete_item_with_out_ignoring_supplier_in_source_id.json"
            );
            assertions().assertThat(mdmInfoJson).is(jsonMatch(expectedMdmInfoJson));
        });
    }

    /**
     * Проверяем сценарий с конвертацией ОСГ из двух information.
     * <p>
     * Данные по ОСГ должны браться из заполненных полей information.
     * Если ни в одной information нет ОСГ - это означает сброс ОСГ.
     */
    @Test
    public void shouldConvertOfItemWithRemainingLifetimeInTwoRecords() throws IOException {
        Mockito.doReturn(UPDATED_DATE_TIME).when(utcTimestampProvider).getCurrentUtcTimestamp();

        String completeItemJson = extractFileContent(
                "fixtures/data/mdm_to_iris_conversion/mdm_item_with_remaining_lifetimes_in_two_records.json");
        MdmIrisPayload.Item.Builder builder = MdmIrisPayload.Item.newBuilder();

        protobufMapper.mergeJson(completeItemJson, builder);
        MdmIrisPayload.Item item = builder.build();

        Collection<ItemDTO> result = mdmToIrisConversionService.fromItem(item);

        assertRemainingLifetimeConversionResults(result);
    }

    private void assertRemainingLifetimeConversionResults(Collection<ItemDTO> result) {
        assertions().assertThat(result).hasSize(1);

        Optional<ItemDTO> mdmRecord = result.stream().findFirst();

        assertions().assertThat(mdmRecord).isPresent();

        mdmRecord.ifPresent(item -> {
            ItemIdentifierDTO itemIdentifier = item.getNaturalKey().getItemIdentifier();
            assertions().assertThat(itemIdentifier.getPartnerId()).isEqualTo("123");
            assertions().assertThat(itemIdentifier.getPartnerSku()).isEqualTo("psku");

            ImmutableReferenceIndex mdmInfo = item.getReferenceIndex();

            String mdmInfoJson = mdmInfo.toJson(referenceIndexer);

            String expectedMdmInfoJson = extractFileContent(
                    "fixtures/expected/mdm_to_iris_conversion/mdm_item_with_remaining_lifetimes_in_two_records.json"
            );
            assertions().assertThat(mdmInfoJson).is(jsonMatch(expectedMdmInfoJson));
        });
    }

    private void assertFullConversionResults(Collection<ItemDTO> result,
                                             String defaultTargetExpectedFilename,
                                             String warehouseTargetExpectedFilename) {
        assertions().assertThat(result).hasSize(2);

        Optional<ItemDTO> mdmWithDefaultTarget = result.stream()
                .filter(v -> v.getNaturalKey().getTarget().equals(DEFAULT_TARGET))
                .findFirst();

        assertions().assertThat(mdmWithDefaultTarget).isPresent();

        mdmWithDefaultTarget.ifPresent(item -> {
            ItemIdentifierDTO itemIdentifier = item.getNaturalKey().getItemIdentifier();
            assertions().assertThat(itemIdentifier.getPartnerId()).isEqualTo("123");
            assertions().assertThat(itemIdentifier.getPartnerSku()).isEqualTo("psku");

            ImmutableReferenceIndex mdmInfo = item.getReferenceIndex();

            String mdmInfoJson = mdmInfo.toJson(referenceIndexer);

            String expectedMdmInfoJson = extractFileContent(defaultTargetExpectedFilename);
            assertions().assertThat(mdmInfoJson).is(jsonMatch(expectedMdmInfoJson));
        });

        Optional<ItemDTO> mdmWithTarget = result.stream()
                .filter(v -> v.getNaturalKey().getTarget().getType() == TargetType.WAREHOUSE)
                .findFirst();

        assertions().assertThat(mdmWithTarget).isPresent();

        mdmWithTarget.ifPresent(item -> {
            ItemIdentifierDTO itemIdentifier = item.getNaturalKey().getItemIdentifier();
            assertions().assertThat(itemIdentifier.getPartnerId()).isEqualTo("123");
            assertions().assertThat(itemIdentifier.getPartnerSku()).isEqualTo("psku");

            ImmutableReferenceIndex mdmWh145Info = item.getReferenceIndex();

            String mdmWh145InfoJson = mdmWh145Info.toJson(referenceIndexer);

            String expectedMdmWh145InfoJson = extractFileContent(warehouseTargetExpectedFilename);

            assertions().assertThat(mdmWh145InfoJson).is(jsonMatch(expectedMdmWh145InfoJson));
        });
    }
}
