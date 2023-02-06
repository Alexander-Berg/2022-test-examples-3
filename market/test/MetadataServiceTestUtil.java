package ru.yandex.market.jmf.logic.def.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.datetime.DateTimeType;
import ru.yandex.market.jmf.attributes.object.ObjectType;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.impl.CreateOrEditMetaclassDto;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.AttributeGroup;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.metainfo.Source;

public class MetadataServiceTestUtil {
    public static CreateOrEditMetaclassDto createMetaclass(
            Fqn fqn,
            Fqn parent,
            Integer version) {
        CreateOrEditMetaclassDto metaclass = new CreateOrEditMetaclassDto();
        metaclass.setMetaclassVersion(version);
        metaclass.setFqn(fqn);
        metaclass.setParent(parent);
        metaclass.setTitle(Randoms.string());
        metaclass.setDescription(Randoms.string());

        return metaclass;
    }

    public static CreateOrEditMetaclassDto.Attribute createAttribute(String code, String typeCode) {
        CreateOrEditMetaclassDto.Attribute attribute = new CreateOrEditMetaclassDto.Attribute();
        attribute.setCode(code);
        attribute.setTitle(Randoms.string());
        attribute.setDescription(Randoms.string());
        attribute.setDefaultValue(Randoms.string());
        attribute.setRequired(Randoms.booleanValue());
        attribute.setEditable(Randoms.booleanValue());
        attribute.setSortable(Randoms.booleanValue());

        if (typeCode != null) {
            CreateOrEditMetaclassDto.Attribute.Type type = new CreateOrEditMetaclassDto.Attribute.Type();
            type.setCode(typeCode);
            attribute.setType(type);
        }

        return attribute;
    }

    public static void assertEquals(CreateOrEditMetaclassDto expected, Metaclass actual) {
        Assertions.assertNotNull(expected);
        Assertions.assertNotNull(actual);

        Assertions.assertEquals(expected.getMetaclassVersion() + 1, actual.getVersion());
        Assertions.assertEquals(expected.getFqn(), actual.getFqn());
        Assertions.assertEquals(expected.getTitle(), actual.getTitle());

        if (expected.getAbstracted() != null) {
            Assertions.assertEquals(expected.getAbstracted(), actual.isAbstract());
        }
        if (expected.getParent() != null) {
            Assertions.assertEquals(expected.getParent(), actual.getParent().getFqn());
        }
        if (expected.getTyped() != null) {
            Assertions.assertEquals(expected.getTyped(), actual.isTyped());
        }
        if (expected.getDescription() != null) {
            Assertions.assertEquals(expected.getDescription(), actual.getDescription());
        }
        if (expected.getLogics() != null) {
            Assertions.assertNotNull(actual.getLogics());
            Assertions.assertTrue(actual.getLogics().containsAll(expected.getLogics()));
        }
        if (expected.getAttributes() != null) {
            Assertions.assertNotNull(actual.getAttributes());
            for (CreateOrEditMetaclassDto.Attribute expectedAttribute : expected.getAttributes()) {
                Attribute actualAttribute = actual.getAttributes().stream()
                        .filter(a -> a.getCode().equals(expectedAttribute.getCode()))
                        .findAny()
                        .orElse(null);
                Assertions.assertNotNull(actualAttribute);
                assertEquals(expectedAttribute, actualAttribute);
            }
        }
        if (expected.getAttributesDeleted() != null) {
            Collection<Attribute> actualAttributes = Objects.requireNonNullElse(actual.getAttributes(), List.of());
            Assertions.assertTrue(actualAttributes.stream()
                    .noneMatch(a -> expected.getAttributesDeleted().contains(a.getCode()))
            );
        }
        if (expected.getAttributeGroups() != null) {
            Assertions.assertNotNull(actual.getAttributeGroups());
            for (CreateOrEditMetaclassDto.AttributeGroup expectedGroup : expected.getAttributeGroups()) {
                AttributeGroup actualGroup = actual.getAttributeGroup(expectedGroup.getCode());
                Assertions.assertNotNull(actualGroup);
                assertEquals(expectedGroup, actualGroup);
            }
        }
        if (expected.getAttributeGroupsDeleted() != null) {
            Collection<AttributeGroup> groups = Objects.requireNonNullElse(actual.getAttributeGroups(), List.of());
            Assertions.assertTrue(groups.stream()
                    .noneMatch(a -> expected.getAttributeGroupsDeleted().contains(a.getCode()))
            );
        }
    }

    public static void assertEquals(CreateOrEditMetaclassDto.Attribute expected, Attribute actual) {
        if (expected.getTitle() != null) {
            Assertions.assertEquals(expected.getTitle(), actual.getTitle());
        }
        if (expected.getDescription() != null) {
            Assertions.assertEquals(expected.getDescription(), actual.getDescription());
        }
        if (expected.getType() != null) {
            if ("dateTime".equals(expected.getType().getCode())) {
                Assertions.assertTrue(actual.getType() instanceof DateTimeType);
                DateTimeType dateTimeType = actual.getType();
                Assertions.assertEquals(expected.getType().getProperties().get("truncateTo"),
                        dateTimeType.truncateTo().name());
            }
            if ("object".equals(expected.getType().getCode())) {
                Assertions.assertTrue(actual.getType() instanceof ObjectType);
                Map<String, Object> expectedMap = expected.getType().getProperties();
                ObjectType actualType = actual.getType();
                Assertions.assertEquals(expectedMap.get("fqn"), actualType.getMetaclass().toString());
                Assertions.assertEquals(expectedMap.get("fqnTemplate"), "${self.fqn}"); // Хардкод в тесте
                Assertions.assertEquals(expectedMap.get("withForeignKey"), actualType.isWithForeignKey());
            } else {
                Assertions.assertEquals(expected.getType().getCode(), actual.getType().getCode());
            }

        }
        if (expected.getDefaultValue() != null) {
            Assertions.assertEquals(expected.getDefaultValue(), actual.getDefaultValue().valueTemplate());
        }
        if (expected.getRequired() != null) {
            Assertions.assertEquals(expected.getRequired(), actual.isRequired());
        }
        if (expected.getEditable() != null) {
            Assertions.assertEquals(expected.getEditable(), actual.isEditable());
        }
        // Sortable нельзя переопределять
        if (expected.getSortable() != null && actual.getSource() != Source.SYSTEM && !actual.isInherited()) {
            Assertions.assertEquals(expected.getSortable(), actual.isSortable());
        }
    }

    private static void assertEquals(CreateOrEditMetaclassDto.AttributeGroup expected, AttributeGroup actual) {
        Assertions.assertEquals(expected.getCode(), actual.getCode());
        // Assertions.assertEquals(expected.getTitle(), actual.getTitle());

        List<String> actualAttributes = actual.getAttributes().stream()
                .map(Attribute::getCode)
                .collect(Collectors.toList());
        Assertions.assertTrue(actualAttributes.containsAll(expected.getAttributes()));
    }

    public static void assertEquals(Metaclass expected, Metaclass actual) {
        Assertions.assertNotNull(expected);
        Assertions.assertNotNull(actual);

        Assertions.assertEquals(expected.getSource(), actual.getSource());
        Assertions.assertEquals(expected.getFqn(), actual.getFqn());
        Assertions.assertEquals(expected.getTitle(), actual.getTitle());
        Assertions.assertEquals(expected.getParent(), actual.getParent());
        Assertions.assertEquals(expected.isAbstract(), actual.isAbstract());
        Assertions.assertEquals(expected.isTyped(), actual.isTyped());
        Assertions.assertEquals(expected.getDescription(), actual.getDescription());
        Assertions.assertLinesMatch(List.copyOf(expected.getLogics()), List.copyOf(actual.getLogics()));
        assertEquals(expected.getAttributes(), actual.getAttributes());
    }

    public static void assertEquals(Collection<Attribute> expected, Collection<Attribute> actual) {
        Assertions.assertNotNull(expected);
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(expected.size(), actual.size());

        for (Attribute expectedAttribute : expected) {
            Attribute actualAttribute = actual.stream()
                    .filter(a -> a.getCode().equals(expectedAttribute.getCode()))
                    .findFirst()
                    .orElse(null);
            assertEquals(expectedAttribute, actualAttribute);
        }
    }

    public static void assertEquals(Attribute expected, Attribute actual) {
        Assertions.assertNotNull(expected);
        Assertions.assertNotNull(actual);

        Assertions.assertEquals(expected.getSource(), actual.getSource());
        Assertions.assertEquals(expected.getFqn(), actual.getFqn());
        Assertions.assertEquals(expected.getTitle(), actual.getTitle());
        Assertions.assertEquals(expected.getDescription(), actual.getDescription());
        Assertions.assertEquals(expected.getType().getCode(), actual.getType().getCode());
        Assertions.assertEquals(expected.getDefaultValue(), actual.getDefaultValue());
        Assertions.assertEquals(expected.isRequired(), actual.isRequired());
        Assertions.assertEquals(expected.isEditable(), actual.isEditable());
        Assertions.assertEquals(expected.isSortable(), actual.isSortable());
    }

    public static void assertEquals(Source expected, Collection<Attribute> actualAttributes, String... codes) {
        for (String code : codes) {
            Attribute actualAttribute = actualAttributes.stream()
                    .filter(a -> a.getCode().equals(code))
                    .findAny()
                    .orElse(null);
            Assertions.assertNotNull(actualAttribute);
            Assertions.assertEquals(expected, actualAttribute.getSource());
        }
    }

    public static void assertEquals(Collection<Attribute> expected, Collection<Attribute> actual, String... codes) {
        List<String> codesList = Arrays.asList(codes);
        Assertions.assertFalse(codesList.isEmpty());

        List<Attribute> expectedAttributes = expected.stream()
                .filter(a -> codesList.contains(a.getCode()))
                .collect(Collectors.toList());
        List<Attribute> actualAttributes = actual.stream()
                .filter(a -> codesList.contains(a.getCode()))
                .collect(Collectors.toList());

        for (Attribute expectedAttribute : expectedAttributes) {
            Attribute actualAttribute = actualAttributes.stream()
                    .filter(a -> a.getCode().equals(expectedAttribute.getCode()))
                    .findAny()
                    .orElse(null);
            Assertions.assertNotNull(actualAttribute);

            assertEquals(expectedAttribute, actualAttribute);
        }
    }

    public static void checkPresentations(Attribute attribute, String view, String edit) {
        Assertions.assertEquals(view, attribute.getViewPresentation());
        Assertions.assertEquals(edit, attribute.getEditPresentation());
    }
}
