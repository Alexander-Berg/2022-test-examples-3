package ru.yandex.direct.model.generator.old.builders;

import java.util.List;
import java.util.Map;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelWithId;
import ru.yandex.direct.model.generator.old.javafile.Util;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class UtilTypeNameParserTest {
    private static final ClassName STRING = ClassName.get(String.class);
    private static final ClassName LONG = ClassName.get(Long.class);
    private static final ClassName LIST = ClassName.get(List.class);
    private static final ClassName MAP = ClassName.get(Map.class);
    private static final ClassName MODEL = ClassName.get(Model.class);
    private static final ClassName MODEL_WITH_ID = ClassName.get(ModelWithId.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameter(0)
    public String name;

    @Parameter(1)
    public String defaultPackage;

    @Parameter(2)
    public TypeName expected;

    @Parameters(name = "{0}, defPackage={1}")
    public static Object[][] params() {
        return new Object[][]{
                {"String", null,
                        STRING},
                {"String", "ru.ya",
                        STRING},
                {"Model", "ru.ya",
                        MODEL},
                {"ModelWithId", "ru.ya",
                        MODEL_WITH_ID},
                {"Model2", "ru.ya",
                        ClassName.get("ru.ya", "Model2")},
                {"List<Long>", null,
                        ParameterizedTypeName.get(LIST, LONG)},
                {"util.MyList<AdGroup>", "ru.ya",
                        ParameterizedTypeName.get(
                                ClassName.get("util", "MyList"),
                                ClassName.get("ru.ya", "AdGroup")
                        )},
                {"util.MyList<ru.ya.AdGroup>", null,
                        ParameterizedTypeName.get(
                                ClassName.get("util", "MyList"),
                                ClassName.get("ru.ya", "AdGroup")
                        )},
                {"util.MyMap<ru.ya.AdGroup, Map<String, ru.ya.Value<my.pkg.Data>>>", null,
                        ParameterizedTypeName.get(
                                ClassName.get("util", "MyMap"),
                                ClassName.get("ru.ya", "AdGroup"),
                                ParameterizedTypeName.get(
                                        MAP,
                                        STRING,
                                        ParameterizedTypeName.get(
                                                ClassName.get("ru.ya", "Value"),
                                                ClassName.get("my.pkg", "Data")
                                        )
                                )
                        )},
                {"Map<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>", "ru.ya",
                        ParameterizedTypeName.get(
                                MAP,
                                STRING,
                                ParameterizedTypeName.get(
                                        MAP,
                                        ClassName.get("ru.ya", "StoreActionForPrices"),
                                        ClassName.get("ru.ya", "MobileContentExternalWorldMoney")
                                )
                        )},
                {"Map<String, Map<ru.ya.StoreActionForPrices, ru.ya.MobileContentExternalWorldMoney>>", null,
                        ParameterizedTypeName.get(
                                MAP,
                                STRING,
                                ParameterizedTypeName.get(
                                        MAP,
                                        ClassName.get("ru.ya", "StoreActionForPrices"),
                                        ClassName.get("ru.ya", "MobileContentExternalWorldMoney")
                                )
                        )},
                {"MyList<ru.ya.AdGroup>", null,
                        null},
                {"List<@javax.annotation.Nullable Long>", null,
                        ParameterizedTypeName.get(
                                LIST,
                                LONG.annotated(
                                        AnnotationSpec
                                                .builder(ClassName.get("javax.annotation", "Nullable"))
                                                .build()
                                ))},
                {"List<@javax.annotation.Nullable @ru.ya.SomeAnother Long>", null,
                        ParameterizedTypeName.get(
                                LIST,
                                LONG.annotated(
                                        AnnotationSpec
                                                .builder(ClassName.get("javax.annotation", "Nullable"))
                                                .build(),
                                        AnnotationSpec
                                                .builder(ClassName.get("ru.ya", "SomeAnother"))
                                                .build()
                                ))},
        };
    }

    @Test
    public void test() {
        if (expected != null) {
            assertThat(Util.typeNameOf(name, defaultPackage))
                    .isEqualTo(expected);
        } else {
            expectedException.expect(Exception.class);
            Util.typeNameOf(name, defaultPackage);
        }
    }
}
