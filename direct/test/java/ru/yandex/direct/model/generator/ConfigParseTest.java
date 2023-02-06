package ru.yandex.direct.model.generator;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.model.generator.old.conf.AbstractModelConf;
import ru.yandex.direct.model.generator.old.conf.AnnotationConf;
import ru.yandex.direct.model.generator.old.conf.AttrConf;
import ru.yandex.direct.model.generator.old.conf.EnumConf;
import ru.yandex.direct.model.generator.old.conf.EnumValueConf;
import ru.yandex.direct.model.generator.old.conf.InterfaceConf;
import ru.yandex.direct.model.generator.old.conf.ModelClassConf;
import ru.yandex.direct.model.generator.old.conf.RelationshipConf;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigParseTest {
    private AbstractModelConf modelConf;

    @Before
    public void before() {
        Config cfg = ConfigFactory.load("examples/adgroup.conf");
        modelConf = ModelClassConf.fromConfig(cfg, "adgroup.conf");
    }

    @Test
    public void configParsedCorrectly() {
        String packageName = "ru.yandex.direct.core.entity.adgroup.model";
        AbstractModelConf etalon = new ModelClassConf.Builder(packageName, "AdGroup")
                .withSource("adgroup.conf")
                .withComment("\nЭто супер-пупер класс\n")
                .withEnums(List.of(
                        new EnumConf.Builder("AdGroupType")
                                .withSourceFileName("adgroup.conf")
                                .withPackageName(packageName)
                                .withComment("тип группы")
                                .withValues(List.of(
                                        new EnumValueConf.Builder("BASE")
                                                .withComment("текстовая группа объявлений").withJson("baseeeee")
                                                .build(),
                                        new EnumValueConf.Builder("DYNAMIC")
                                                .withComment("текстовые динамические объявления").build()
                                ))
                                .build(),
                        new EnumConf.Builder("ExampleStatusCopy")
                                .withSourceFileName("adgroup.conf")
                                .withPackageName(packageName)
                                .withValuesSource("ru.yandex.direct.model.generator.example.ExampleStatus")
                                .build()
                ))
                .withAttrs(List.of(
                        AttrConf.of("id", "java.lang.Long", null, "идентификатор группы объявлений",
                                "id_json", "NON_NULL"),
                        AttrConf.of("pid", "java.lang.Long", null, "", "", "", "id", List.of(), null),
                        AttrConf.of("campaignId", "java.lang.Long", null, "", "", "", "", List.of(),
                                RelationshipConf.of("AdGroupContainsAdGroups",
                                        "ru.yandex.direct.core.entity.adgroup.model.AdGroup",
                                        "ru.yandex.direct.core.entity.adgroup.model",
                                        ClassName.get("ru.yandex.direct.core.entity.adgroup.model", "AdGroup"),
                                        TypeName.get(Long.class), "campaignId", "adgroup.conf")),
                        AttrConf.of("type", "ru.yandex.direct.core.entity.adgroup.model.AdGroupType", null),
                        AttrConf.of("status", "ru.yandex.direct.model.generator.example.ExampleStatus", null),
                        AttrConf.of("lastChange", "java.time.LocalDateTime", null, "", "", "", "id", List.of(
                                AnnotationConf.of("ru.yandex.direct.model.generator.example.RequiredAttributes", null,
                                        AnnotationConf.Applicability.GETTER, List.of(new AnnotationConf.Param("value",
                                                "$L", "{ru.yandex.direct.model.generator.example.Attribute" +
                                                ".CAN_EDIT_CAMPAIGN_CONTENT_LANGUAGE_BLOCK,ru.yandex.direct.model" +
                                                ".generator.example.Attribute.OPERATOR_HAS_GRID_FEATURE}")))),
                                null),
                        AttrConf.of("getterAnnotated", "java.lang.Long", null, List.of(
                                AnnotationConf.of("java.lang.Deprecated", null, AnnotationConf.Applicability.GETTER,
                                        emptyList())
                        )),
                        AttrConf.of("fieldAnnotated", "java.lang.Long", null, List.of(
                                AnnotationConf.of("java.lang.Deprecated", null, AnnotationConf.Applicability.FIELD,
                                        emptyList())
                        )),
                        AttrConf.of("fieldAndGetterAnnotated", "java.lang.Long", null, List.of(
                                AnnotationConf
                                        .of("java.lang.Deprecated", null,
                                                EnumSet.of(AnnotationConf.Applicability.FIELD,
                                                        AnnotationConf.Applicability.GETTER),
                                                emptyList())
                        )),
                        AttrConf.of("setterParameterAnnotated", "java.lang.Long", null, List.of(
                                AnnotationConf
                                        .of("java.lang.Deprecated", null, AnnotationConf.Applicability.SETTER_PARAMETER,
                                                emptyList())
                        )),
                        AttrConf.of("binaryData", "byte[]", null, List.of()),
                        AttrConf.of("annotatedWithParams", "java.lang.Long", null, List.of(
                                AnnotationConf.of("com.fasterxml.jackson.annotation.JsonProperty", null,
                                        AnnotationConf.Applicability.FIELD,
                                        List.of(
                                                new AnnotationConf.Param("value", "$S", "some_name"),
                                                new AnnotationConf.Param("index", "$L", "1"),
                                                new AnnotationConf.Param("required", "$L", "true"),
                                                new AnnotationConf.Param("access", "$L",
                                                        "com.fasterxml.jackson.annotation.JsonProperty." +
                                                                "Access.READ_ONLY")
                                        ))
                        ))
                ))
                .withImplementsList(List.of("ru.yandex.direct.model.generator.example.TestAdgroup",
                        "ru.yandex.direct.model.Entity<Long>"))
                .withInterfaces(List.of(
                        new InterfaceConf.Builder("SimpleAdGroup")
                                .withSource("adgroup.conf")
                                .withPackageName(packageName)
                                .withClassConfigName("AdGroup")
                                .withAttrs(List.of("id", "campaignId", "type"))
                                .withReadOnly(false)
                                .withAnnotations(List.of(
                                        AnnotationConf
                                                .of("java.lang.Deprecated", null,
                                                        EnumSet.of(AnnotationConf.Applicability.FIELD,
                                                                AnnotationConf.Applicability.GETTER),
                                                        emptyList())
                                ))
                                .withJsonSubtypes(true)
                                .build(),
                        new InterfaceConf.Builder("NotSoSimpleAdGroup")
                                .withSource("adgroup.conf")
                                .withPackageName(packageName)
                                .withClassConfigName("AdGroup")
                                .withExtendsList(List.of("SimpleAdGroup", "ModelWithId"))
                                .withAttrs(List.of("lastChange"))
                                .withReadOnly(false)
                                .withAnnotations(Collections.emptyList())
                                .build(),
                        new InterfaceConf.Builder("AdGroupCampaignPair")
                                .withSource("adgroup.conf")
                                .withPackageName(packageName)
                                .withClassConfigName("AdGroup")
                                .withAttrs(List.of("id", "campaignId"))
                                .withReadOnly(true)
                                .withAnnotations(Collections.emptyList())
                                .build(),
                        new InterfaceConf.Builder("AdGroupWithoutId")
                                .withSource("adgroup.conf")
                                .withPackageName(packageName)
                                .withClassConfigName("AdGroup")
                                .withAttrs(List.of("type", "lastChange", "campaignId"))
                                .withReadOnly(true)
                                .withAnnotations(Collections.emptyList())
                                .build(),
                        new InterfaceConf.Builder("AdGroupAnnotationCheck")
                                .withSource("adgroup.conf")
                                .withPackageName(packageName)
                                .withClassConfigName("AdGroup")
                                .withAttrs(List.of("id", "getterAnnotated", "fieldAnnotated",
                                        "fieldAndGetterAnnotated"))
                                .withReadOnly(true)
                                .withAnnotations(Collections.emptyList())
                                .build(),
                        new InterfaceConf.Builder("SimpleAdGroupParent")
                                .withSource("adgroup.conf")
                                .withPackageName(packageName)
                                .withClassConfigName("AdGroup")
                                .withAttrs(List.of("id", "campaignId", "type"))
                                .withAnnotations(Collections.emptyList())
                                .withJsonSubtypesWithNameValue(true)
                                .build(),
                        new InterfaceConf.Builder("SimpleAdGroupChild")
                                .withSource("adgroup.conf")
                                .withPackageName(packageName)
                                .withClassConfigName("AdGroup")
                                .withExtendsList(List.of("SimpleAdGroupParent"))
                                .withAttrs(List.of("lastChange"))
                                .withAnnotations(Collections.emptyList())
                                .build()
                        ))
                .withAnnotations(List.of(
                        AnnotationConf.of("javax.annotation.ParametersAreNonnullByDefault", null,
                                EnumSet.of(AnnotationConf.Applicability.FIELD, AnnotationConf.Applicability.GETTER),
                                emptyList()),
                        AnnotationConf.of("java.lang.Deprecated", null,
                                EnumSet.of(AnnotationConf.Applicability.FIELD, AnnotationConf.Applicability.GETTER),
                                emptyList())
                ))
                .build();
        assertThat(modelConf).isEqualToComparingFieldByFieldRecursively(etalon);
    }
}
