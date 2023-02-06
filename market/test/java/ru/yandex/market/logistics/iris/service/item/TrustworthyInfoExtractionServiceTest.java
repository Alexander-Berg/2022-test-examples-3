package ru.yandex.market.logistics.iris.service.item;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistics.iris.converter.ItemIdentifierConverter;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.index.field.Field;
import ru.yandex.market.logistics.iris.core.index.field.FieldValue;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFieldProvider;
import ru.yandex.market.logistics.iris.core.index.field.Value;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.core.index.implementation.Reference;
import ru.yandex.market.logistics.iris.core.index.implementation.ReferenceIndexImpl;
import ru.yandex.market.logistics.iris.core.index.json.mixin.ReferenceIndexImplMixin;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.entity.item.ItemEntity;
import ru.yandex.market.logistics.iris.picker.TrustworthyFieldValuePicker;
import ru.yandex.market.logistics.iris.util.UtcTimestampProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.iris.service.item.TestPredefinedField.DUMMY;
import static ru.yandex.market.logistics.iris.service.item.TestPredefinedField.GUMMY;
import static ru.yandex.market.logistics.iris.service.item.TestPredefinedField.YUMMY;

public class TrustworthyInfoExtractionServiceTest {
    private static final EmbeddableSource SOURCE = new EmbeddableSource(Source.ADMIN_SOURCE.getSourceId(), SourceType.WAREHOUSE);

    private static final ItemIdentifier ITEM1 = ItemIdentifier.of("partner", "sku0001");
    private static final ItemIdentifier ITEM2 = ItemIdentifier.of("partner", "sku0002");
    private static final ItemIdentifier ITEM3 = ItemIdentifier.of("partner", "sku0003");

    private static final Map<Field<String>, String> ITEM1_PROPS = ImmutableMap.of(DUMMY, "dummy #1", YUMMY, "yummy #1", GUMMY, "gummy #1");
    private static final Map<Field<String>, String> ITEM2_PROPS = ImmutableMap.of(DUMMY, "dummy #2", YUMMY, "yummy #2");
    private static final Map<Field<String>, String> ITEM3_PROPS = ImmutableMap.of(DUMMY, "dummy #3");

    private TrustworthyFieldValuePicker picker;
    private ChangeTrackingReferenceIndexer indexer;
    private ItemFindService itemFindService;
    private PredefinedFieldProvider predefinedFieldProvider;
    private HasLifetimeFieldFiltrationService hasLifetimeFieldFiltrationService;
    private UtcTimestampProvider utcTimestampProvider;
    private TrustworthyInfoExtractionService service;

    @Before
    public void setup() {
        picker = mock(TrustworthyFieldValuePicker.class);
        indexer = mock(ChangeTrackingReferenceIndexer.class);
        predefinedFieldProvider = mock(PredefinedFieldProvider.class);
        hasLifetimeFieldFiltrationService = mock(HasLifetimeFieldFiltrationService.class);
        utcTimestampProvider = mock(UtcTimestampProvider.class);
        itemFindService = mock(ItemFindService.class);
        when(utcTimestampProvider.getCurrentUtcTimestamp()).thenReturn(ZonedDateTime.now());
        mockItemProperties(ITEM1, ITEM1_PROPS);
        mockItemProperties(ITEM2, ITEM2_PROPS);
        mockItemProperties(ITEM3, ITEM3_PROPS);
        mockPredefinedFieldProvider(DUMMY, YUMMY, GUMMY);

        service = Mockito.spy(new TrustworthyInfoExtractionService(picker, indexer, itemFindService,
                predefinedFieldProvider, hasLifetimeFieldFiltrationService, utcTimestampProvider));
    }

    /**
     * Тестируем работу при передаче списка имен полей размером 1
     */
    @Test
    public void testGetTrustworthyInfoWith1FieldName() {
        mockItemRepositoryFindAllWithIdentifiers(ITEM1);

        Map<ItemIdentifier, ReferenceIndexImpl> trustworthyInfo = service.getTrustworthyInfo(
                Collections.singletonList(ITEM1),
                Collections.singletonList(DUMMY.getFieldName()),
                false
        );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(trustworthyInfo.size()).isEqualTo(1);
            softly.assertThat(trustworthyInfo.containsKey(ITEM1)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM1))).isEqualTo(ImmutableMap.of(DUMMY,
                    ITEM1_PROPS.get(DUMMY)));
        });
    }

    /**
     * Тестируем работу при передаче списка имен полей размером 3
     */
    @Test
    public void testGetTrustworthyInfoWith3FieldNames() {
        mockItemRepositoryFindAllWithIdentifiers(ITEM1, ITEM2, ITEM3);

        Map<ItemIdentifier, ReferenceIndexImpl> trustworthyInfo = service.getTrustworthyInfo(
                Arrays.asList(ITEM1, ITEM2, ITEM3),
                Arrays.asList(DUMMY.getFieldName(), YUMMY.getFieldName(), GUMMY.getFieldName()),
                false
        );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(trustworthyInfo.size()).isEqualTo(3);

            softly.assertThat(trustworthyInfo.containsKey(ITEM1)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM1))).isEqualTo(ITEM1_PROPS);

            softly.assertThat(trustworthyInfo.containsKey(ITEM2)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM2))).isEqualTo(ITEM2_PROPS);

            softly.assertThat(trustworthyInfo.containsKey(ITEM1)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM3))).isEqualTo(ITEM3_PROPS);
        });
    }

    /**
     * Тестируем работу при передаче пустого списка имен полей, должны вернуться значения для всех известных полей
     */
    @Test
    public void testGetTrustworthyInfoWithEmptyFieldNamesList() {
        mockItemRepositoryFindAllWithIdentifiers(ITEM1, ITEM2, ITEM3);

        Map<ItemIdentifier, ReferenceIndexImpl> trustworthyInfo = service.getTrustworthyInfo(
                Arrays.asList(ITEM1, ITEM2, ITEM3),
                Collections.emptyList(),
                false
        );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(trustworthyInfo.size()).isEqualTo(3);

            softly.assertThat(trustworthyInfo.containsKey(ITEM1)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM1))).isEqualTo(ITEM1_PROPS);

            softly.assertThat(trustworthyInfo.containsKey(ITEM2)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM2))).isEqualTo(ITEM2_PROPS);

            softly.assertThat(trustworthyInfo.containsKey(ITEM1)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM3))).isEqualTo(ITEM3_PROPS);
        });
    }

    /**
     * Тестируем работу при передаче null вместо списка имен полей, должны вернуться значения для всех известных полей
     */
    @Test
    public void testGetTrustworthyInfoWithNullFieldNamesList() {
        mockItemRepositoryFindAllWithIdentifiers(ITEM1, ITEM2, ITEM3);

        Map<ItemIdentifier, ReferenceIndexImpl> trustworthyInfo = service.getTrustworthyInfo(
                Arrays.asList(ITEM1, ITEM2, ITEM3),
                null,
                false
        );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(trustworthyInfo.size()).isEqualTo(3);

            softly.assertThat(trustworthyInfo.containsKey(ITEM1)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM1))).isEqualTo(ITEM1_PROPS);

            softly.assertThat(trustworthyInfo.containsKey(ITEM2)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM2))).isEqualTo(ITEM2_PROPS);

            softly.assertThat(trustworthyInfo.containsKey(ITEM1)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM3))).isEqualTo(ITEM3_PROPS);
        });
    }

    @Test
    public void shouldRemoveDuplicateItemIdentifiersFormInputList() {
        final ItemIdentifier[] itemIdentifiers1 = {ITEM1, ITEM2};
        final ItemIdentifier[] itemIdentifiers2 = {ITEM3};

        when(itemFindService.findByIdentifiersAndSources(any(), any()))
                .thenReturn(Arrays.stream(itemIdentifiers1).map(this::toItemEntity).collect(Collectors.toSet()))
                .thenReturn(Arrays.stream(itemIdentifiers2).map(this::toItemEntity).collect(Collectors.toSet()));

        Mockito.doReturn(2).when(service).getMaxQueryIdsNumber();

        Map<ItemIdentifier, ReferenceIndexImpl> trustworthyInfo = service.getTrustworthyInfo(
                Arrays.asList(ITEM1, ITEM2, ITEM3, ITEM2),
                Arrays.asList(DUMMY.getFieldName(), YUMMY.getFieldName(), GUMMY.getFieldName()),
                false
        );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(trustworthyInfo.size()).isEqualTo(3);

            softly.assertThat(trustworthyInfo.containsKey(ITEM1)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM1))).isEqualTo(ITEM1_PROPS);

            softly.assertThat(trustworthyInfo.containsKey(ITEM2)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM2))).isEqualTo(ITEM2_PROPS);

            softly.assertThat(trustworthyInfo.containsKey(ITEM1)).isTrue();
            softly.assertThat(extractProperties(trustworthyInfo.get(ITEM3))).isEqualTo(ITEM3_PROPS);
        });
    }


    private void mockItemProperties(ItemIdentifier identifier, Map<Field<String>, String> properties) {
        Reference ref = new Reference();
        when(indexer.fromJson(toItemEntity(identifier).getReferenceIndex())).thenReturn(new ReferenceIndexImplMixin(ref));
        properties.forEach((field, value) -> {
            ref.add(field.getFieldName(), FieldValue.of(value, field, ZonedDateTime.now()));
            when(picker.pick(eq(field), argThat(item -> item.getItemIdentifier().equals(identifier))))
                    .thenReturn(Optional.of(new Value<>(ref.getStorage().get(field.getFieldName()).getValue().toString())));
        });
    }

    private void mockPredefinedFieldProvider(Field<?>... predefinedFields) {
        ImmutableMap.Builder<String, Field<?>> builder = ImmutableMap.builder();

        for (Field<?> field : predefinedFields) {
            when(predefinedFieldProvider.find(field.getFieldName())).thenReturn(Optional.of(field));
            builder.put(field.getFieldName(), field);
        }

        when(predefinedFieldProvider.getFields()).thenReturn(builder.build());
    }

    private void mockItemRepositoryFindAllWithIdentifiers(ItemIdentifier... items) {
        Set<EmbeddableItemIdentifier> identifiers = Arrays.stream(items)
            .map(ItemIdentifierConverter::toEmbeddableIdentifier)
            .collect(Collectors.toSet());
        when(itemFindService.findByIdentifiersAndSources(eq(new ArrayList<>(identifiers)), any()))
            .thenReturn(Arrays.stream(items).map(this::toItemEntity).collect(Collectors.toSet()));
    }

    private ItemEntity toItemEntity(ItemIdentifier identifier) {
        return new ItemEntity()
                .setNaturalKey(new EmbeddableItemNaturalKey(ItemIdentifierConverter.toEmbeddableIdentifier(identifier), SOURCE))
                .setReferenceIndex(identifier.getPartnerId() + identifier.getPartnerSku() + "-index");
    }

    private Map<Field<?>, ?> extractProperties(ReferenceIndexImpl refIndex) {
        return refIndex.getReference().getStorage().values().stream()
                .collect(Collectors.toMap(FieldValue::getField, FieldValue::getValue));
    }

}
