package ru.yandex.market.logistics.iris.service.mdm.conversion;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.configuration.protobuf.ProtobufMapper;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.domain.target.TargetType;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.ReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.complex.Dimension;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.model.ItemDTO;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.logistics.iris.model.ItemNaturalKeyDTO;
import ru.yandex.market.logistics.iris.model.SourceAndTargetDTO;
import ru.yandex.market.logistics.iris.model.SourceDTO;
import ru.yandex.market.logistics.iris.model.TargetDTO;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyBooleanKey;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;

import static org.mockito.Mockito.doReturn;

public class IrisToMdmConversionServiceTest extends AbstractContextualTest {

    private static final ItemIdentifierDTO ITEM_ID = new ItemIdentifierDTO(
            "123",
            "psku"
    );

    private static final SourceAndTargetDTO WH_145 = new SourceAndTargetDTO(
            new SourceDTO("145", SourceType.WAREHOUSE)
    );

    private static final SourceAndTargetDTO WH_145_UNKNOWN = new SourceAndTargetDTO(
            new SourceDTO("145", SourceType.WAREHOUSE),
            new TargetDTO("197", TargetType.UNKNOWN)
    );

    private static final SourceAndTargetDTO WH_145_WH_147 = new SourceAndTargetDTO(
            new SourceDTO("145", SourceType.WAREHOUSE),
            new TargetDTO("147", TargetType.WAREHOUSE)
    );

    private static final SourceAndTargetDTO UNKNOWN = new SourceAndTargetDTO(
            new SourceDTO("233", SourceType.UNKNOWN)
    );

    private static final SourceAndTargetDTO UNKNOWN_UNKNOWN = new SourceAndTargetDTO(
            new SourceDTO("233", SourceType.UNKNOWN),
            new TargetDTO("233", TargetType.UNKNOWN)
    );

    private static final SourceAndTargetDTO UNKNOWN_WH_147 = new SourceAndTargetDTO(
            new SourceDTO("233", SourceType.UNKNOWN),
            new TargetDTO("147", TargetType.WAREHOUSE)
    );

    private static final SourceAndTargetDTO MDM = new SourceAndTargetDTO(
            new SourceDTO("-1", SourceType.MDM)
    );

    private static final SourceAndTargetDTO MDM_WH_145 = new SourceAndTargetDTO(
            new SourceDTO("-1", SourceType.MDM),
            new TargetDTO("145", TargetType.WAREHOUSE)
    );

    private static final SourceAndTargetDTO MDM_UNKNOWN = new SourceAndTargetDTO(
            new SourceDTO("-1", SourceType.MDM),
            new TargetDTO("145", TargetType.UNKNOWN)
    );

    private static final ZonedDateTime VALUE_DATE_TIME = ZonedDateTime.of(
            LocalDate.of(1970, 1, 2).atStartOfDay(),
            ZoneOffset.UTC
    );

    @Autowired
    private ChangeTrackingReferenceIndexer indexer;

    @Autowired
    private ProtobufMapper protobufMapper;

    @Autowired
    private IrisToMdmConversionService irisToMdmConversionService;

    @SpyBean
    private SystemPropertyService systemPropertyService;

    /**
     * Проверяем, что вызов со следующими данными:
     * Collections.emptyMap
     * <p>
     * В ответ вернет:
     * Optional.empty
     */
    @Test
    public void completeItemEmptyDataConversion() {
        Optional<MdmIrisPayload.CompleteItem> result = irisToMdmConversionService.toCompleteItem(
                ITEM_ID,
                Collections.emptyMap()
        );

        assertions().assertThat(result).isEmpty();
    }

    /**
     * Проверяем, что вызов со следующими данными:
     * * WH 145 -> пустой индекс
     * <p>
     * В ответ вернет:
     * Optional.empty.
     */
    @Test
    public void completeItemEmptyIndexesDataConversion() {
        Optional<MdmIrisPayload.CompleteItem> result = irisToMdmConversionService.toCompleteItem(
                ITEM_ID,
                ImmutableMap.of(WH_145, indexer.createEmptyIndex())
        );

        assertions().assertThat(result).isEmpty();
    }

    /**
     * Проверяем, что вызов со следующими данными:
     * UNKNOWN 233 -> корректный индекс
     * <p>
     * В ответ вернет:
     * Optional.empty.
     */
    @Test
    public void completeItemFilledIndexWithUnmappedRemainingInformationSourceConversion() {
        ChangeTrackingReferenceIndex index = indexer.createEmptyIndex();
        index.set(PredefinedFields.WEIGHT_GROSS, Dimension.of(BigDecimal.valueOf(10)), VALUE_DATE_TIME);

        Optional<MdmIrisPayload.CompleteItem> result = irisToMdmConversionService.toCompleteItem(
                ITEM_ID,
                ImmutableMap.of(UNKNOWN, index)
        );

        assertions().assertThat(result).isEmpty();
    }


    /**
     * Проверяем, что вызов со следующими данными:
     * WH 145 -> корректный индекс
     * <p>
     * В ответ вернет:
     * Корретную модель, remaining_information которой будет 1 объект с информацией по weight_gross
     */
    @Test
    public void completeItemFilledIndexWithMappedRemainingInformationSourceConversion() throws IOException {
        ChangeTrackingReferenceIndex index = indexer.createEmptyIndex();
        index.set(PredefinedFields.WEIGHT_GROSS, Dimension.of(BigDecimal.valueOf(10)), VALUE_DATE_TIME);

        Optional<MdmIrisPayload.CompleteItem> result = irisToMdmConversionService.toCompleteItem(
                ITEM_ID,
                ImmutableMap.of(WH_145, index)
        );

        assertResult(result, "fixtures/data/iris_to_mdm_conversion/1.json");
    }

    /**
     * Проверяем, что вызов со следующими данными:
     * MDM 145 -> корректный индекс
     * <p>
     * В ответ вернет:
     * Корретную модель, trustworthy_information которой будет содержать 1 объект с информацией по weight_gross
     */
    @Test
    public void completeItemFilledIndexWithMappedTrustworthyInformationSourceConversion() throws IOException {
        ChangeTrackingReferenceIndex index = indexer.createEmptyIndex();
        index.set(PredefinedFields.WEIGHT_GROSS, Dimension.of(BigDecimal.valueOf(10)), VALUE_DATE_TIME);

        Optional<MdmIrisPayload.CompleteItem> result = irisToMdmConversionService.toCompleteItem(
                ITEM_ID,
                ImmutableMap.of(MDM, index)
        );

        assertResult(result, "fixtures/data/iris_to_mdm_conversion/2.json");
    }


    /**
     * Проверяем полную конвертацию со следующими входными параметрами и ожидаемым поведением:
     * .put(MDM, index) -> Попадает в итоговый CompleteItem в trustworthy_information
     * .put(MDM_UNKNOWN, index) -> Пропускается, т.к. цель неизвестна.
     * .put(MDM_WH_145, index) -> Попадает в итоговый CompleteItem в trustworthy_information
     * .put(WH_145, index) -> Попадает в итоговый CompleteItem в remaining_information
     * .put(WH_145_UNKNOWN, index) -> Пропускается, т.к. цель неизвестна.
     * .put(WH_145_WH_147, index) -> Попадает в итоговый CompleteItem в remaining_information
     * .put(UNKNOWN, index) -> Пропускается, т.к. источник неизвестен.
     * .put(UNKNOWN_UNKNOWN, index) -> Пропускается, т.к. источник неизвестен.
     * .put(UNKNOWN_WH_147, index) -> Пропускается, т.к. источник неизвестен.
     */
    @Test
    public void completeItemFullConversion() throws IOException {
        String indexJson = extractFileContent("fixtures/data/iris_to_mdm_conversion/reference_index.json");
        ReferenceIndex index = indexer.fromJson(indexJson);

        ImmutableMap<SourceAndTargetDTO, ReferenceIndex> data =
                ImmutableMap.<SourceAndTargetDTO, ReferenceIndex>builder()
                        .put(MDM, index)
                        .put(MDM_UNKNOWN, index)
                        .put(MDM_WH_145, index)
                        .put(WH_145, index)
                        .put(WH_145_UNKNOWN, index)
                        .put(WH_145_WH_147, index)
                        .put(UNKNOWN, index)
                        .put(UNKNOWN_UNKNOWN, index)
                        .put(UNKNOWN_WH_147, index)
                        .build();

        Optional<MdmIrisPayload.CompleteItem> result = irisToMdmConversionService.toCompleteItem(
                ITEM_ID,
                data
        );

        assertResult(result, "fixtures/data/iris_to_mdm_conversion/full_conversion_complete_item.json");
    }


    /**
     * Проверяем, что попытка конвертировать справочную информацию с пустым индексом в ответ вернет Optional.empty.
     */
    @Test
    public void itemEmptyDataConversion() {
        ItemDTO item = new ItemDTO(
                new ItemNaturalKeyDTO(ITEM_ID, WH_145),
                indexer.createEmptyIndex()
        );

        Optional<MdmIrisPayload.ItemBatch> result = irisToMdmConversionService.toItem(item);

        assertions().assertThat(result).isEmpty();
    }

    @Test
    public void itemFullConversion() {

        doReturn(true).when(systemPropertyService)
                .getBooleanProperty(SystemPropertyBooleanKey.ENABLE_NEW_SHELF_LIFE_FEATURE);

        String indexJson = extractFileContent("fixtures/data/iris_to_mdm_conversion/reference_index.json");
        ChangeTrackingReferenceIndex index = indexer.fromJson(indexJson);

        ItemDTO item = new ItemDTO(
                new ItemNaturalKeyDTO(ITEM_ID, WH_145),
                index
        );

        Optional<MdmIrisPayload.ItemBatch> result = irisToMdmConversionService.toItem(item);

        assertions().assertThat(result).isPresent();
        List<MdmIrisPayload.Item> items = result.get().getItemList();
        assertions().assertThat(items).hasSize(1);

        MdmIrisPayload.Item actualMdmItem = items.get(0);
        String actualJSON = protobufMapper.toJsonString(actualMdmItem);

        String expectedJSON = extractFileContent("fixtures/data/iris_to_mdm_conversion/mdm_item.json");

        assertions().assertThat(actualJSON).is(jsonMatch(expectedJSON));
    }

    @Test
    public void itemConversionWithNullDimensions() {
        String indexJson =
                extractFileContent("fixtures/data/iris_to_mdm_conversion/reference_index_with_null_width.json");
        ChangeTrackingReferenceIndex index = indexer.fromJson(indexJson);

        ItemDTO item = new ItemDTO(
                new ItemNaturalKeyDTO(ITEM_ID, WH_145),
                index
        );

        Optional<MdmIrisPayload.ItemBatch> result = irisToMdmConversionService.toItem(item);

        assertions().assertThat(result).isPresent();
        List<MdmIrisPayload.Item> items = result.get().getItemList();
        assertions().assertThat(items).hasSize(1);

        MdmIrisPayload.Item actualMdmItem = items.get(0);
        String actualJSON = protobufMapper.toJsonString(actualMdmItem);

        String expectedJSON =
                extractFileContent("fixtures/expected/iris_to_mdm_conversion/mdm_item_with_empty_dimensions.json");

        assertions().assertThat(actualJSON).is(jsonMatch(expectedJSON));
    }

    @Test
    public void itemBatchConversion() {
        Optional<MdmIrisPayload.ItemBatch> result = irisToMdmConversionService.toItem(createItemDTO());

        assertions().assertThat(result).isPresent();
        List<MdmIrisPayload.Item> items = result.get().getItemList();
        assertions().assertThat(items).hasSize(2);

        MdmIrisPayload.Item firstActualMdmItem = items.get(0);
        assertItem(firstActualMdmItem,
                "fixtures/expected/iris_to_mdm_conversion/reference_index_batch/first_index.json");

        MdmIrisPayload.Item secondActualMdmItem = items.get(1);
        assertItem(secondActualMdmItem,
                "fixtures/expected/iris_to_mdm_conversion/reference_index_batch/second_index.json");
    }

    private void assertResult(Optional<MdmIrisPayload.CompleteItem> result,
                              String jsonPath) throws IOException {
        String expectedJson = extractFileContent(jsonPath);

        MdmIrisPayload.CompleteItem.Builder builder = MdmIrisPayload.CompleteItem.newBuilder();

        protobufMapper.mergeJson(expectedJson, builder);
        MdmIrisPayload.CompleteItem expectedItem = builder.build();

        assertions().assertThat(result).isPresent();

        MdmIrisPayload.CompleteItem actualCompleteItem = result.get();
        assertions().assertThat(actualCompleteItem).isEqualTo(expectedItem);
    }

    private void assertItem(MdmIrisPayload.Item item, String expectedJsonPath) {
        String secondActualJSON = protobufMapper.toJsonString(item);
        String secondExpectedJSON = extractFileContent(expectedJsonPath);

        assertions().assertThat(secondActualJSON).is(jsonMatch(secondExpectedJSON));
    }

    private List<ItemDTO> createItemDTO() {
        String firstIndexJson =
                extractFileContent("fixtures/data/iris_to_mdm_conversion/reference_index_batch/first_index.json");
        String secondIndexJson =
                extractFileContent("fixtures/data/iris_to_mdm_conversion/reference_index_batch/second_index.json");

        return ImmutableList.of(
                new ItemDTO(new ItemNaturalKeyDTO(ITEM_ID, WH_145), indexer.fromJson(firstIndexJson)),
                new ItemDTO(new ItemNaturalKeyDTO(ITEM_ID, WH_145_WH_147), indexer.fromJson(secondIndexJson)));
    }
}
