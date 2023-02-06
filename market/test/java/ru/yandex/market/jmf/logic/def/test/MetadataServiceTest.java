package ru.yandex.market.jmf.logic.def.test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.impl.CreateOrEditMetaclassDto;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.metainfo.Source;

import static ru.yandex.market.jmf.logic.def.test.MetadataServiceTestUtil.checkPresentations;

/**
 * Тест проверяет создание и редактирование метаклассов.
 */
@SpringJUnitConfig({
        InternalLogicDefaultTestConfiguration.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MetadataServiceTest {
    private static final Fqn FQN_NEW_ENTITY = Fqn.of("newEntity");
    private static final Fqn FQN_PARENTLESS_ENTITY = Fqn.of("parentlessEntity");
    private static final Fqn FQN_ROOT = Fqn.of("rootEntity");
    private static final Fqn FQN_ROOT_CHILD = Fqn.of("rootEntity$child");
    private static final Fqn FQN_ROOT_NEW_ENTITY = Fqn.of("rootEntity$newEntity");
    private static final Fqn FQN_ENTITY = Fqn.of("entity");
    private static final Fqn FQN_SYSTEM_ENTITY = Fqn.of("systemEntity");

    @Inject
    private MetadataService metadataService;

    @Test
    public void addNew_addAttribute_dateTime() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, null, 0);
        CreateOrEditMetaclassDto.Attribute attribute = MetadataServiceTestUtil.createAttribute("attr0", "string");
        attribute.getType().setCode("dateTime");
        attribute.getType().setProperties(Map.of("truncateTo", "SECONDS"));
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        Assertions.assertEquals(1, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0");
    }

    @Test
    public void addNew_addAttribute_object() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, null, 0);
        CreateOrEditMetaclassDto.Attribute attribute = MetadataServiceTestUtil.createAttribute("attr0", "");
        attribute.setSortable(false);
        attribute.setSortable(false);
        attribute.getType().setCode("object");
        attribute.getType().setProperties(Map.of(
                "fqn", "parentlessEntity",
                "fqnTemplate", "${self.fqn}",
                "withForeignKey", false));
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        Assertions.assertEquals(1, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0");
    }

    @Test
    public void addNew_addAttributeWithReqEditSort() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, null, 0);
        CreateOrEditMetaclassDto.Attribute attribute = MetadataServiceTestUtil.createAttribute("attr0", "string");
        attribute.setRequired(true);
        attribute.setEditable(true);
        attribute.setSortable(true);
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        Assertions.assertEquals(1, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0");
    }

    @Test
    public void editExist_editAttributeWithReqEditSort() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);
        Assertions.assertNotNull(metaclass);

        // Все true
        CreateOrEditMetaclassDto dto =
                MetadataServiceTestUtil.createMetaclass(FQN_PARENTLESS_ENTITY, null, metaclass.getVersion());
        CreateOrEditMetaclassDto.Attribute attr = MetadataServiceTestUtil.createAttribute("attrParentless", null);
        attr.setRequired(true);
        attr.setEditable(true);
        attr.setSortable(true);
        dto.setAttributes(List.of(attr));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrParentless");

        // Все false
        dto = MetadataServiceTestUtil.createMetaclass(FQN_PARENTLESS_ENTITY, null, newMetaclass.getVersion());
        attr = MetadataServiceTestUtil.createAttribute("attrParentless", null);
        attr.setRequired(false);
        attr.setEditable(false);
        attr.setSortable(false);
        dto.setAttributes(List.of(attr));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        newMetaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrParentless");
    }

    @Test
    public void addNew_addAttributeWithPresentations() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        String viewPresentation = "testViewPresentation";
        String editPresentation = "testEditPresentation";

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, null, 0);
        CreateOrEditMetaclassDto.Attribute attribute1 = MetadataServiceTestUtil.createAttribute("attr1", "string");
        CreateOrEditMetaclassDto.Attribute attribute2 = MetadataServiceTestUtil.createAttribute("attr2", "string");
        CreateOrEditMetaclassDto.Attribute attribute3 = MetadataServiceTestUtil.createAttribute("attr3", "string");

        attribute1.setPresentations(Map.of(
                "view", viewPresentation,
                "edit", editPresentation
        ));
        attribute2.setPresentations(Map.of("view", viewPresentation));
        attribute3.setPresentations(Map.of("edit", editPresentation));
        dto.setAttributes(List.of(attribute1, attribute2, attribute3));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        Assertions.assertEquals(3, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(),
                attribute1.getCode(), attribute2.getCode(), attribute3.getCode());

        checkPresentations(newMetaclass.getAttribute(attribute1.getCode()), viewPresentation, editPresentation);
        checkPresentations(newMetaclass.getAttribute(attribute2.getCode()), viewPresentation, null);
        checkPresentations(newMetaclass.getAttribute(attribute3.getCode()), null, editPresentation);
    }

    @Test
    public void editExist_editAttributeWithPresentations() {
        Fqn existingFqn = FQN_PARENTLESS_ENTITY;
        Metaclass metaclass = metadataService.getMetaclass(existingFqn);
        Assertions.assertNotNull(metaclass);

        String viewPresentation1 = "testViewPresentation";
        String editPresentation1 = "testEditPresentation";

        String viewPresentation2 = "testViewPresentation2";
        String editPresentation2 = "testEditPresentation2";

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(existingFqn, null, 1);
        CreateOrEditMetaclassDto.Attribute attribute = MetadataServiceTestUtil.createAttribute("attrPresentation",
                null);

        // without presentation
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(existingFqn);
        checkPresentations(newMetaclass.getAttribute(attribute.getCode()), viewPresentation1, editPresentation1);

        // new value
        dto.setMetaclassVersion(2);
        attribute.setPresentations(Map.of(
                "view", viewPresentation2,
                "edit", editPresentation2
        ));
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        newMetaclass = metadataService.getMetaclass(existingFqn);
        checkPresentations(newMetaclass.getAttribute(attribute.getCode()), viewPresentation2, editPresentation2);

        // edit null
        dto.setMetaclassVersion(3);
        attribute.setPresentations(Map.of("view", viewPresentation1));
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        newMetaclass = metadataService.getMetaclass(existingFqn);
        checkPresentations(newMetaclass.getAttribute(attribute.getCode()), viewPresentation1, editPresentation1);

        // view null
        dto.setMetaclassVersion(4);
        attribute.setPresentations(Map.of("edit", editPresentation1));
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        newMetaclass = metadataService.getMetaclass(existingFqn);
        checkPresentations(newMetaclass.getAttribute(attribute.getCode()), viewPresentation1, editPresentation1);
    }

    @Test
    public void addNew_addAttributeWithFiltrationScript() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        String filtrationScript = "filtrationScriptCode";

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, null, 0);
        CreateOrEditMetaclassDto.Attribute attribute = MetadataServiceTestUtil.createAttribute("attr1", "string");

        attribute.setFiltrationScriptCode(filtrationScript);
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        Assertions.assertEquals(1, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), attribute.getCode());
        Assertions.assertEquals(filtrationScript,
                newMetaclass.getAttribute(attribute.getCode()).getFiltrationScriptCode());
    }

    @Test
    public void editExist_editAttributeWithFiltrationScript() {
        Fqn existingFqn = FQN_PARENTLESS_ENTITY;
        Metaclass metaclass = metadataService.getMetaclass(existingFqn);
        Assertions.assertNotNull(metaclass);

        String filtrationScript = "filtrationScriptCode";

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(existingFqn, null, 1);
        CreateOrEditMetaclassDto.Attribute attribute = MetadataServiceTestUtil.createAttribute("objectAttr", null);

        // not null value
        attribute.setFiltrationScriptCode(filtrationScript);
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(existingFqn);
        Assertions.assertEquals(filtrationScript,
                newMetaclass.getAttribute(attribute.getCode()).getFiltrationScriptCode());

        // null value
        dto.setMetaclassVersion(2);
        attribute.setFiltrationScriptCode(null);
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        newMetaclass = metadataService.getMetaclass(existingFqn);
        Assertions.assertEquals(filtrationScript,
                newMetaclass.getAttribute(attribute.getCode()).getFiltrationScriptCode());

        // empty value
        dto.setMetaclassVersion(3);
        attribute.setFiltrationScriptCode("");
        dto.setAttributes(List.of(attribute));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        newMetaclass = metadataService.getMetaclass(existingFqn);
        Assertions.assertNull(newMetaclass.getAttribute(attribute.getCode()).getFiltrationScriptCode());
    }

    @Test
    public void addNew_abstract_parentNull() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, null, 0);
        dto.setAbstracted(true);
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr0", "string"),
                MetadataServiceTestUtil.createAttribute("attr1", "string")));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        Assertions.assertEquals(2, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0", "attr1");
    }

    @Test
    public void addNew_addAttribute_parentNull() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, null, 0);
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr0", "string"),
                MetadataServiceTestUtil.createAttribute("attr1", "string")));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        Assertions.assertEquals(2, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0", "attr1");
    }

    @Test
    public void addNew_addAttribute_parentRoot() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY));
        Metaclass rootMetaclass = metadataService.getMetaclass(FQN_ROOT);

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT_NEW_ENTITY, FQN_ROOT, 0);
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr0", "string"),
                MetadataServiceTestUtil.createAttribute("attr1", "string")));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass newRootMetaclass = metadataService.getMetaclass(FQN_ROOT);

        Assertions.assertEquals(rootMetaclass.getAttributes().size() + 2, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(rootMetaclass, newRootMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0", "attr1");
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrRoot");
    }

    @Test
    public void addNew_addAttribute_parentEntity() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));
        Metaclass entityMetaclass = metadataService.getMetaclass(FQN_ENTITY);

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, FQN_ENTITY, 0);
        dto.setAttributes(List.of(MetadataServiceTestUtil.createAttribute("attr1", "string")));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);
        Metaclass newEntityMetaclass = metadataService.getMetaclass(FQN_ENTITY);

        Assertions.assertEquals(entityMetaclass.getAttributes().size() + 1, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(entityMetaclass, newEntityMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr1");
    }

    @Test
    public void addNew_addAttribute_parentSystemEntity() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));
        Metaclass systemEntityMetaclass = metadataService.getMetaclass(FQN_SYSTEM_ENTITY);

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, FQN_SYSTEM_ENTITY, 0);
        dto.setAttributes(List.of(MetadataServiceTestUtil.createAttribute("attr1", "string")));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);
        Metaclass newSystemEntityMetaclass = metadataService.getMetaclass(FQN_SYSTEM_ENTITY);

        Assertions.assertEquals(systemEntityMetaclass.getAttributes().size() + 1, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(systemEntityMetaclass, newSystemEntityMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr1");
    }

    @Test
    public void addNew_overrideAttribute_parentRoot() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY));
        Metaclass rootMetaclass = metadataService.getMetaclass(FQN_ROOT);

        CreateOrEditMetaclassDto.Attribute attrOverrideDto = MetadataServiceTestUtil.createAttribute("attrRoot", null);
        // Пока реализованы только эти свойства атрибута
        attrOverrideDto.setTitle(Randoms.string());
        attrOverrideDto.setDescription(Randoms.string());
        attrOverrideDto.setDefaultValue(Randoms.string());
        attrOverrideDto.setRequired(true);

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT_NEW_ENTITY, FQN_ROOT, 0);
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr1", "string"),
                attrOverrideDto
        ));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass newRootMetaclass = metadataService.getMetaclass(FQN_ROOT);

        Assertions.assertEquals(rootMetaclass.getAttributes().size() + 1, newMetaclass.getAttributes().size());
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(rootMetaclass, newRootMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr1");
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrRoot");
    }

    @Test
    public void editNew_parentNull_attributeCreate() {
        addNew_addAttribute_parentNull();
        Metaclass metaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);
        Assertions.assertNotNull(metaclass);
        Assertions.assertEquals(2, metaclass.getAttributes().size());

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(
                FQN_NEW_ENTITY,
                null,
                metaclass.getVersion());
        dto.setAttributes(List.of(MetadataServiceTestUtil.createAttribute("attr2", "string")));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        Assertions.assertEquals(metaclass.getAttributes().size() + 1, newMetaclass.getAttributes().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr2");
    }

    @Test
    public void editNew_parentNull_attributeEdit() {
        addNew_addAttribute_parentNull();
        Metaclass metaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);
        Assertions.assertNotNull(metaclass);
        Assertions.assertEquals(2, metaclass.getAttributes().size());

        CreateOrEditMetaclassDto.Attribute attrEditDto = MetadataServiceTestUtil.createAttribute("attr1", null);
        // Пока реализованы только эти свойства атрибута
        attrEditDto.setTitle(Randoms.string());
        attrEditDto.setDescription(Randoms.string());
        attrEditDto.setDefaultValue(Randoms.string());
        attrEditDto.setRequired(true);

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(
                FQN_NEW_ENTITY,
                null,
                metaclass.getVersion());
        dto.setAttributes(List.of(attrEditDto));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        Assertions.assertEquals(metaclass.getAttributes().size(), newMetaclass.getAttributes().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr1");
    }

    @Test
    public void editNew_parentNull_attributeDelete() {
        addNew_addAttribute_parentNull();
        Metaclass metaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);
        Assertions.assertNotNull(metaclass);
        Assertions.assertEquals(2, metaclass.getAttributes().size());

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(
                FQN_NEW_ENTITY,
                null,
                metaclass.getVersion());
        dto.setAttributesDeleted(List.of("attr1"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);
        Assertions.assertEquals(metaclass.getAttributes().size() - 1, newMetaclass.getAttributes().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
    }

    @Test
    public void editNew_parentNull_attributeCreateAndDeleteAndEdit() {
        addNew_addAttribute_parentNull();
        Metaclass metaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);
        Assertions.assertNotNull(metaclass);
        Assertions.assertEquals(2, metaclass.getAttributes().size());

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(
                FQN_NEW_ENTITY,
                null,
                metaclass.getVersion());
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr0", "string"),
                MetadataServiceTestUtil.createAttribute("attr2", "string")));
        dto.setAttributesDeleted(List.of("attr1"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);
        Assertions.assertEquals(metaclass.getAttributes().size() - 1 + 1, newMetaclass.getAttributes().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0", "attr2");
    }

    @Test
    public void editNew_parentRoot_attributeCreate() {
        addNew_addAttribute_parentRoot();
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass rootMetaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertNotNull(metaclass);
        Assertions.assertEquals(5, metaclass.getAttributes().size());

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(
                FQN_ROOT_NEW_ENTITY,
                null,
                metaclass.getVersion());
        dto.setAttributes(List.of(MetadataServiceTestUtil.createAttribute("attr2", "string")));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass newRootMetaclass = metadataService.getMetaclass(FQN_ROOT);

        Assertions.assertEquals(metaclass.getAttributes().size() + 1, newMetaclass.getAttributes().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(rootMetaclass, newRootMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr2");
    }

    @Test
    public void editNew_parentRoot_attributesEdit() {
        addNew_addAttribute_parentRoot();
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass rootMetaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertNotNull(metaclass);
        Assertions.assertEquals(5, metaclass.getAttributes().size());

        List<CreateOrEditMetaclassDto.Attribute> attrsOveride = metaclass.getAttributes().stream()
                .map(a -> MetadataServiceTestUtil.createAttribute(a.getCode(), null))
                .collect(Collectors.toList());

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(
                FQN_ROOT_NEW_ENTITY,
                null,
                metaclass.getVersion());
        dto.setAttributes(attrsOveride);
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass newRootMetaclass = metadataService.getMetaclass(FQN_ROOT);

        Assertions.assertEquals(metaclass.getAttributes().size(), newMetaclass.getAttributes().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(rootMetaclass, newRootMetaclass);
    }

    @Test
    public void editNew_parentRoot_attributeDelete() {
        addNew_addAttribute_parentRoot();
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass rootMetaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertNotNull(metaclass);
        Assertions.assertEquals(5, metaclass.getAttributes().size());

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(
                FQN_ROOT_NEW_ENTITY,
                null,
                metaclass.getVersion());
        dto.setAttributesDeleted(List.of("attr1"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass newRootMetaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertEquals(metaclass.getAttributes().size() - 1, newMetaclass.getAttributes().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(rootMetaclass, newRootMetaclass);
    }

    @Test
    public void editNew_parentRoot_attributeCreateAndDeleteAndEdit() {
        addNew_addAttribute_parentRoot();
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass rootMetaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertNotNull(metaclass);
        Assertions.assertEquals(5, metaclass.getAttributes().size());

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(
                FQN_ROOT_NEW_ENTITY,
                null,
                metaclass.getVersion());
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr0", "string"),
                MetadataServiceTestUtil.createAttribute("attr2", "string")));
        dto.setAttributesDeleted(List.of("attr1"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Metaclass newRootMetaclass = metadataService.getMetaclass(FQN_ROOT);

        Assertions.assertEquals(metaclass.getAttributes().size() - 1 + 1, newMetaclass.getAttributes().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(rootMetaclass, newRootMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0", "attr2");
    }

    @Test
    public void editExist_addAttribute_parentless() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto =
                MetadataServiceTestUtil.createMetaclass(FQN_PARENTLESS_ENTITY, null, metaclass.getVersion());
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr0", "string"),
                MetadataServiceTestUtil.createAttribute("attr1", "string")));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0", "attr1");
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrParentless");
        MetadataServiceTestUtil.assertEquals(metaclass.getAttributes(), newMetaclass.getAttributes(), "attrParentless");
    }

    @Test
    public void editExist_editAttribute_parentless() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto =
                MetadataServiceTestUtil.createMetaclass(FQN_PARENTLESS_ENTITY, null, metaclass.getVersion());
        dto.setAttributes(List.of(MetadataServiceTestUtil.createAttribute("attrParentless", null)));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrParentless");
    }

    @Test
    public void editExist_deleteAttribute_parentless() {
        editExist_addAttribute_parentless();
        Metaclass metaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto =
                MetadataServiceTestUtil.createMetaclass(FQN_PARENTLESS_ENTITY, null, metaclass.getVersion());
        dto.setAttributesDeleted(List.of("attr0"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
    }

    @Test
    public void editExist_addAndEditAndDeleteAttribute_parentless() {
        editExist_addAttribute_parentless();
        Metaclass metaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto =
                MetadataServiceTestUtil.createMetaclass(FQN_PARENTLESS_ENTITY, null, metaclass.getVersion());
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attrParentless", null),// override
                MetadataServiceTestUtil.createAttribute("attr1", null),// edit
                MetadataServiceTestUtil.createAttribute("attr2", "string")));// new
        dto.setAttributesDeleted(List.of("attr0"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_PARENTLESS_ENTITY);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr1", "attr2");
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrParentless");
    }

    @Test
    public void editExist_addAttribute_rootChild() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_CHILD);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto =
                MetadataServiceTestUtil.createMetaclass(FQN_ROOT_CHILD, null, metaclass.getVersion());
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr0", "string"),
                MetadataServiceTestUtil.createAttribute("attr1", "string")));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_CHILD);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr0", "attr1");
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrRoot", "attrChild");
        MetadataServiceTestUtil.assertEquals(metaclass.getAttributes(), newMetaclass.getAttributes(), "attrRoot",
                "attrChild");
    }

    @Test
    public void editExist_editAttribute_rootChild() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_CHILD);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto =
                MetadataServiceTestUtil.createMetaclass(FQN_ROOT_CHILD, null, metaclass.getVersion());
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attrRoot", null),
                MetadataServiceTestUtil.createAttribute("attrChild", null)));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_CHILD);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrRoot", "attrChild");
    }

    @Test
    public void editExist_deleteAttribute_rootChild() {
        editExist_addAttribute_rootChild();
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_CHILD);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto =
                MetadataServiceTestUtil.createMetaclass(FQN_ROOT_CHILD, null, metaclass.getVersion());
        dto.setAttributesDeleted(List.of("attr0"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_CHILD);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr1");
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrRoot", "attrChild");
    }

    @Test
    public void editExist_addAndEditAndDeleteAttribute_rootChild() {
        editExist_addAttribute_rootChild();
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_CHILD);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto =
                MetadataServiceTestUtil.createMetaclass(FQN_ROOT_CHILD, null, metaclass.getVersion());
        dto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attrChild", null),// override
                MetadataServiceTestUtil.createAttribute("attr1", null),// edit
                MetadataServiceTestUtil.createAttribute("attr2", "string")));// new
        dto.setAttributesDeleted(List.of("attr0"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_CHILD);

        Assertions.assertEquals(Source.SYSTEM, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        MetadataServiceTestUtil.assertEquals(Source.USER, newMetaclass.getAttributes(), "attr1", "attr2");
        MetadataServiceTestUtil.assertEquals(Source.SYSTEM, newMetaclass.getAttributes(), "attrRoot", "attrChild");
        MetadataServiceTestUtil.assertEquals(metaclass.getAttributes(), newMetaclass.getAttributes(), "attrRoot");
    }

    @Test
    public void addNew_addChild() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_ENTITY);
        Assertions.assertNotNull(metaclass);

        Fqn newFqn = Fqn.of("new");
        CreateOrEditMetaclassDto newDto = MetadataServiceTestUtil.createMetaclass(newFqn, FQN_ENTITY, 0);
        newDto.setTyped(true);
        newDto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr0", "integer"),
                MetadataServiceTestUtil.createAttribute("attr1", "string")));
        metadataService.createOrEditMetaclass(newDto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(newFqn);
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(newDto, newMetaclass);

        Fqn childFqn = newFqn.ofType("child");
        CreateOrEditMetaclassDto childDto = MetadataServiceTestUtil.createMetaclass(childFqn, newFqn, 0);
        childDto.setAttributes(List.of(
                MetadataServiceTestUtil.createAttribute("attr1", null),
                MetadataServiceTestUtil.createAttribute("attr2", "string")));
        metadataService.createOrEditMetaclass(childDto, metadataService.getConfVersion());

        Metaclass childMetaclass = metadataService.getMetaclass(childFqn);
        Assertions.assertEquals(Source.USER, childMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(childDto, childMetaclass);
    }

    @Test
    public void addNew_addLogic() {
        Assertions.assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        CreateOrEditMetaclassDto newDto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, FQN_ENTITY, 0);
        newDto.setLogics(List.of("withTitle"));
        newDto.setAttributes(List.of(MetadataServiceTestUtil.createAttribute("attr0", "integer")));
        metadataService.createOrEditMetaclass(newDto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);
        Assertions.assertEquals(Source.USER, newMetaclass.getSource());
        MetadataServiceTestUtil.assertEquals(newDto, newMetaclass);
        Assertions.assertNotNull(newMetaclass.getAttribute("title"));
    }

    @Test
    public void editNew_addAttributeGroup() {
        addNew_addAttribute_parentRoot();

        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT_NEW_ENTITY, null,
                metaclass.getVersion());
        CreateOrEditMetaclassDto.AttributeGroup attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("testGroup");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attr1", "attrRoot1"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertEquals(metaclass.getAttributeGroups().size() + 1, newMetaclass.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
    }

    @Test
    public void editNew_editAttributeGroup() {
        addNew_addAttribute_parentRoot();

        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT_NEW_ENTITY, null,
                metaclass.getVersion());
        CreateOrEditMetaclassDto.AttributeGroup attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("rootGroup");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attr1"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertEquals(metaclass.getAttributeGroups().size(), newMetaclass.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
    }

    @Test
    public void editNew_addAttributeGroupAndEdit() {
        addNew_addAttribute_parentRoot();

        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertNotNull(metaclass);

        // Add
        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT_NEW_ENTITY, null,
                metaclass.getVersion());
        CreateOrEditMetaclassDto.AttributeGroup attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("testGroup");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attr1", "attrRoot1"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass metaclassAfterAdd = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertEquals(metaclass.getAttributeGroups().size() + 1,
                metaclassAfterAdd.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, metaclassAfterAdd);

        // Edit
        dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT_NEW_ENTITY, null, metaclassAfterAdd.getVersion());
        attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("testGroup");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attr0", "attrRoot1"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass metaclassAfterEdit = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertEquals(metaclassAfterAdd.getAttributeGroups().size(),
                metaclassAfterEdit.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, metaclassAfterEdit);
    }

    @Test
    public void editNew_addAttributeGroupAndDelete() {
        addNew_addAttribute_parentRoot();

        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertNotNull(metaclass);

        // Add
        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT_NEW_ENTITY, null,
                metaclass.getVersion());
        CreateOrEditMetaclassDto.AttributeGroup attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("testGroup");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attr1", "attrRoot1"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass metaclassAfterAdd = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertEquals(metaclass.getAttributeGroups().size() + 1,
                metaclassAfterAdd.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, metaclassAfterAdd);

        // Delete
        dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT_NEW_ENTITY, null, metaclassAfterAdd.getVersion());
        dto.setAttributeGroupsDeleted(List.of("testGroup"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass metaclassAfterDelete = metadataService.getMetaclass(FQN_ROOT_NEW_ENTITY);
        Assertions.assertEquals(metaclassAfterAdd.getAttributeGroups().size() - 1,
                metaclassAfterDelete.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, metaclassAfterDelete);
    }

    @Test
    public void editExist_addAttributeGroup() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT, null, metaclass.getVersion());
        CreateOrEditMetaclassDto.AttributeGroup attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("testGroup");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attrRoot", "attrRoot1"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertEquals(metaclass.getAttributeGroups().size() + 1, newMetaclass.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
    }

    @Test
    public void editExist_editAttributeGroup() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertNotNull(metaclass);

        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT, null, metaclass.getVersion());
        CreateOrEditMetaclassDto.AttributeGroup attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("rootGroup");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attrRoot"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertEquals(metaclass.getAttributeGroups().size(), newMetaclass.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
    }

    @Test
    public void editExist_addAttributeGroupAndEdit() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertNotNull(metaclass);

        // Add
        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT, null, metaclass.getVersion());
        CreateOrEditMetaclassDto.AttributeGroup attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("testGroup1");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attrRoot", "attrRoot1"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass metaclassAfterAdd = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertEquals(metaclass.getAttributeGroups().size() + 1,
                metaclassAfterAdd.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, metaclassAfterAdd);

        // Edit
        dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT, null, metaclassAfterAdd.getVersion());
        attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("testGroup1");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attrRoot1", "attrRoot2"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass metaclassAfterEdit = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertEquals(metaclassAfterAdd.getAttributeGroups().size(),
                metaclassAfterEdit.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, metaclassAfterEdit);
    }

    @Test
    public void editExist_addAttributeGroupAndDelete() {
        Metaclass metaclass = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertNotNull(metaclass);

        // Add
        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT, null, metaclass.getVersion());
        CreateOrEditMetaclassDto.AttributeGroup attrGroup = new CreateOrEditMetaclassDto.AttributeGroup();
        attrGroup.setCode("testGroup");
        attrGroup.setTitle(Randoms.string());
        attrGroup.setAttributes(List.of("attrRoot", "attrRoot1"));
        dto.setAttributeGroups(List.of(attrGroup));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass metaclassAfterAdd = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertEquals(metaclass.getAttributeGroups().size() + 1,
                metaclassAfterAdd.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, metaclassAfterAdd);

        // Delete
        dto = MetadataServiceTestUtil.createMetaclass(FQN_ROOT, null, metaclassAfterAdd.getVersion());
        dto.setAttributeGroupsDeleted(List.of("testGroup"));
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion());

        Metaclass metaclassAfterDelete = metadataService.getMetaclass(FQN_ROOT);
        Assertions.assertEquals(metaclassAfterAdd.getAttributeGroups().size() - 1,
                metaclassAfterDelete.getAttributeGroups().size());
        MetadataServiceTestUtil.assertEquals(dto, metaclassAfterDelete);
    }
}
