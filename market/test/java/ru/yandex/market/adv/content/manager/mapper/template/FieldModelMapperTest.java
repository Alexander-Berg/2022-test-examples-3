package ru.yandex.market.adv.content.manager.mapper.template;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cms.client.model.CmsReferenceType;
import ru.yandex.cms.client.model.CmsTemplateEntity;
import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.mj.generated.server.model.FieldEntity;

/**
 * Date: 23.09.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class FieldModelMapperTest extends AbstractContentManagerTest {

    @Autowired
    private FieldModelMapper fieldModelMapper;

    @DisplayName("Маппинг из FieldEntity в TemplateEntity.")
    @Test
    void mapTo_allOf_successful() {
        Map<String, Object> resultMap = fieldModelMapper.mapTo(
                Map.of(
                        "a", List.of(),
                        "b", List.of(
                                getEntity("1", null, null, "String1",
                                        FieldEntity.ValueTypeEnum.STRING)
                        ),
                        "c", List.of(
                                getEntity("2", "Type1", "User", null,
                                        FieldEntity.ValueTypeEnum.ENTITY)
                        ),
                        "d", List.of(
                                getEntity("3", null, null, "String2",
                                        FieldEntity.ValueTypeEnum.STRING_ARRAY),
                                getEntity("4", "Type2", "User", "String3",
                                        FieldEntity.ValueTypeEnum.STRING_ARRAY)
                        ),
                        "e", List.of(
                                getEntity("5", "Type3", "User", null,
                                        FieldEntity.ValueTypeEnum.ENTITY_ARRAY),
                                getEntity("6", "Type4", "Entry", "String4",
                                        FieldEntity.ValueTypeEnum.ENTITY_ARRAY)
                        )
                )
        );

        Assertions.assertThat(resultMap)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "a", List.of(),
                                "b", "String1",
                                "c", getEntity("2", "Type1", CmsReferenceType.User),
                                "d", List.of("String2", "String3"),
                                "e", List.of(
                                        getEntity("5", "Type3", CmsReferenceType.User),
                                        getEntity("6", "Type4", CmsReferenceType.Entry)
                                )
                        )
                );
    }

    @DisplayName("Маппинг из TemplateEntity в FieldEntity.")
    @Test
    void mapFrom_allOf_successful() {
        Map<String, List<FieldEntity>> resultMap = fieldModelMapper.mapFrom(
                Map.of(
                        "a", List.of(),
                        "b", "String1",
                        "c", getMapEntity("2", "Type1", CmsReferenceType.User),
                        "d", List.of("String2", "String3"),
                        "e", List.of(
                                getMapEntity("5", "Type3", CmsReferenceType.User),
                                getMapEntity("6", "Type4", CmsReferenceType.Entry)
                        )
                )
        );

        resultMap.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(fieldEntity -> fieldEntity.getValueType() == FieldEntity.ValueTypeEnum.STRING_ARRAY
                        || fieldEntity.getValueType() == FieldEntity.ValueTypeEnum.STRING)
                .forEach(fieldEntity -> {
                    Assertions.assertThat(fieldEntity.getId())
                            .isNotNull();
                    fieldEntity.setId(null);
                });

        Assertions.assertThat(resultMap)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "a", List.of(),
                                "b", List.of(
                                        getEntity(null, null, null, "String1",
                                                FieldEntity.ValueTypeEnum.STRING)
                                ),
                                "c", List.of(
                                        getEntity("2", "Type1", "User", null,
                                                FieldEntity.ValueTypeEnum.ENTITY)
                                ),
                                "d", List.of(
                                        getEntity(null, null, null, "String2",
                                                FieldEntity.ValueTypeEnum.STRING_ARRAY),
                                        getEntity(null, null, null, "String3",
                                                FieldEntity.ValueTypeEnum.STRING_ARRAY)
                                ),
                                "e", List.of(
                                        getEntity("5", "Type3", "User", null,
                                                FieldEntity.ValueTypeEnum.ENTITY_ARRAY),
                                        getEntity("6", "Type4", "Entry", null,
                                                FieldEntity.ValueTypeEnum.ENTITY_ARRAY)
                                )
                        )
                );
    }

    @Nonnull
    private FieldEntity getEntity(String id,
                                  String type,
                                  String referenceType,
                                  String value,
                                  FieldEntity.ValueTypeEnum valueTypeEnum) {
        FieldEntity fieldEntity = new FieldEntity();
        fieldEntity.setId(id);
        fieldEntity.setType(type);
        fieldEntity.setReferenceType(referenceType);
        fieldEntity.setValue(value);
        fieldEntity.setValueType(valueTypeEnum);
        return fieldEntity;
    }

    @Nonnull
    private CmsTemplateEntity getEntity(String id,
                                        String type,
                                        CmsReferenceType referenceType) {
        CmsTemplateEntity fieldEntity = new CmsTemplateEntity();
        fieldEntity.setId(id);
        fieldEntity.setType(type);
        fieldEntity.setReferenceType(referenceType);
        return fieldEntity;
    }

    @Nonnull
    private Map<String, String> getMapEntity(String id,
                                             String type,
                                             @Nonnull CmsReferenceType referenceType) {
        Map<String, String> map = new HashMap<>();

        map.put("id", id);
        map.put("type", type);
        map.put("referenceType", referenceType.name());

        return map;
    }
}
