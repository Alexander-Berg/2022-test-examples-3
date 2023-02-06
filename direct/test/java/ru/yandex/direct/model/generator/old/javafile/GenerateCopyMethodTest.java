package ru.yandex.direct.model.generator.old.javafile;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.model.generator.old.conf.ModelClassConf;
import ru.yandex.direct.model.generator.old.registry.ModelConfRegistry;
import ru.yandex.direct.model.generator.old.spec.ClassSpec;
import ru.yandex.direct.model.generator.old.spec.factory.JavaFileSpecFactory;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.model.generator.old.javafile.ModelClassBuilder.COPYABLE_MODEL;
import static ru.yandex.direct.model.generator.old.javafile.ModelClassBuilder.COPY_METHOD_NAME;
import static ru.yandex.direct.model.generator.old.javafile.ModelClassBuilder.COPY_TO_METHOD_NAME;

public class GenerateCopyMethodTest {

    private ModelClassConf.Builder modelBuilder;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void initTestData() {
        modelBuilder = new ModelClassConf.Builder("test.package", "testClass")
                .withGenerateCopyMethod(true)
                .withAttrs(Collections.emptyList())
                .withAnnotations(Collections.emptyList());
    }


    @Test
    public void checkGenerateCopyMethod() {
        ModelClassConf modelConf = modelBuilder.build();
        Map<String, TypeSpec> typeSpecsByClassName = buildTypeSpecs(modelConf);

        TypeSpec typeSpec = typeSpecsByClassName.get(modelConf.getName());
        Set<String> methodNames = getMethodNames(typeSpec);
        Set<ClassName> superInterfaces = getSuperInterfaces(typeSpec);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(methodNames).contains(COPY_METHOD_NAME, COPY_TO_METHOD_NAME);
        soft.assertThat(superInterfaces).contains(COPYABLE_MODEL);
        soft.assertAll();
    }

    @Test
    public void checkGenerateOnlyCopyToMethod_WhenClassHasAbstractModifier() {
        ModelClassConf modelConf = modelBuilder
                .withModifiers(Collections.singletonList(Modifier.ABSTRACT))
                .build();
        Map<String, TypeSpec> typeSpecsByClassName = buildTypeSpecs(modelConf);

        TypeSpec typeSpec = typeSpecsByClassName.get(modelConf.getName());
        Set<String> methodNames = getMethodNames(typeSpec);

        assertThat(methodNames)
                .contains(COPY_TO_METHOD_NAME)
                .doesNotContain(COPY_METHOD_NAME);
    }

    @Test
    public void checkNotGenerateCopyMethod_WhenGenerateCopyMethod_IsFalse() {
        ModelClassConf modelConf = modelBuilder
                .withGenerateCopyMethod(false)
                .build();
        Map<String, TypeSpec> typeSpecsByClassName = buildTypeSpecs(modelConf);

        TypeSpec typeSpec = typeSpecsByClassName.get(modelConf.getName());
        Set<String> methodNames = getMethodNames(typeSpec);

        assertThat(methodNames).doesNotContain(COPY_METHOD_NAME, COPY_TO_METHOD_NAME);
    }

    @Test
    public void checkGenerateCopyMethod_ForChildClass() {
        ModelClassConf superModelConf = modelBuilder.build();
        ModelClassConf childModelConf = new ModelClassConf.Builder(superModelConf.getPackageName(), "childClass")
                .withGenerateCopyMethod(false)
                .withAttrs(Collections.emptyList())
                .withAnnotations(Collections.emptyList())
                .withExtendsClass(superModelConf.getName())
                .build();
        Map<String, TypeSpec> typeSpecsByClassName = buildTypeSpecs(childModelConf, superModelConf);

        TypeSpec childTypeSpec = typeSpecsByClassName.get(childModelConf.getName());
        Set<String> methodNames = getMethodNames(childTypeSpec);
        Set<ClassName> superInterfaces = getSuperInterfaces(childTypeSpec);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(methodNames).contains(COPY_METHOD_NAME, COPY_TO_METHOD_NAME);
        soft.assertThat(superInterfaces).doesNotContain(COPYABLE_MODEL);
        soft.assertAll();
    }

    @Test
    public void checkThrowException_WhenSuperClassHasNotCopyMethod() {
        ModelClassConf someModel = modelBuilder
                .withExtendsClass("someSuperClassWithoutCopyMethod")
                .build();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(
                format("Can't generate copy method for %s. Super class must have copy method", someModel.getName()));
        buildTypeSpecs(someModel);
    }


    private Map<String, TypeSpec> buildTypeSpecs(ModelClassConf... confs) {
        ModelConfRegistry modelConfRegistry = new ModelConfRegistry(Arrays.asList(confs));
        JavaFileSpecFactory javaFileSpecFactory = new JavaFileSpecFactory(modelConfRegistry);
        return javaFileSpecFactory.convertAllConfigsToSpecs().stream()
                .map(ClassSpec.class::cast)
                .map(ModelClassBuilder::new)
                .map(ModelClassBuilder::build)
                .collect(Collectors.toMap(typeSpec -> typeSpec.name, Function.identity()));
    }

    private Set<String> getMethodNames(TypeSpec typeSpec) {
        return typeSpec.methodSpecs.stream()
                .map(methodSpec -> methodSpec.name)
                .collect(Collectors.toSet());
    }

    private Set<ClassName> getSuperInterfaces(TypeSpec typeSpec) {
        return typeSpec.superinterfaces.stream()
                .map(ClassName.class::cast)
                .collect(Collectors.toSet());
    }
}
