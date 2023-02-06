package ru.yandex.direct.model.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.squareup.javapoet.JavaFile;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.yandex.direct.model.Copyable;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.model.ModelWithId;
import ru.yandex.direct.model.generator.example.ExampleStatus;
import ru.yandex.direct.model.generator.example.TestAdgroup;
import ru.yandex.direct.model.generator.example.TestAdgroupDescendant;
import ru.yandex.direct.model.generator.old.conf.AttrConf;
import ru.yandex.direct.model.generator.old.conf.ModelConfFactory;
import ru.yandex.direct.model.generator.old.conf.UpperLevelModelConf;
import ru.yandex.direct.model.generator.old.javafile.JavaFileFactory;
import ru.yandex.direct.model.generator.old.registry.ModelConfRegistry;
import ru.yandex.direct.model.generator.old.spec.factory.JavaFileSpecFactory;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

public class CompileTest {

    private static final String INTERFACE_WITH_ID = "AdGroupCampaignPair";
    private static final String INTERFACE_WITH_TYPE = "SimpleAdGroup";
    private static final String INTERFACE_WITH_INHERITED_ID = "NotSoSimpleAdGroup";
    private static final String INTERFACE_WITHOUT_ID = "AdGroupWithoutId";
    private static final String INTERFACE_WITH_ANNOTATIONS = "AdGroupAnnotationCheck";
    private static final String STANDALONE_INTERFACE = "AdGroupStandaloneInterface";

    private static final String MODEL_CONF_RESOURCE_NAME = "examples/adgroup.conf";
    private static final String MODEL_CONF_DESC_RESOURCE_NAME = "examples/adgroup-descendant.conf";
    private static final String MODEL_CONF_WITH_COPY_METHOD = "examples/model_with_copy_method.conf";
    private static final String DTO_RESOURCE_NAME = "examples/dto.conf";
    private static final String STANDALONE_INTERFACE_RESOURCE_NAME = "examples/i_standalone.conf";
    private static final String INTERFACE_WITH_ENUM_RESOURCE_NAME = "examples/i_with_enum.conf";

    private static final String COPY_METHOD_NAME = "copy";
    private static final String ALL_MODEL_PROPERTIES_METHOD_NAME = "allModelProperties";

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    private static UpperLevelModelConf modelConf;
    private static UpperLevelModelConf modelDtoConf;
    private static UpperLevelModelConf modelDescendantConf;

    private static UpperLevelModelConf modelWithCopyMethod;
    private static UpperLevelModelConf standaloneInterfaceConf;
    private static UpperLevelModelConf interfaceWithEnumConf;
    private static ClassLoader classLoader;
    private static ModelConfFactory modelConfFactory;

    private TestAdgroup testAdgroup;
    private Class<?> adgroupClass;
    private Class<?> enumClass;
    private Class<?> enumJsonAnnotatedClass;
    private Class<?> dtoClass;
    private Class<?> standaloneInterfaceClass;
    private Class<?> interfaceWithEnumClass;
    private Class<?> enumFromInterfaceClass;

    private Object enumNo;
    private Object enumYes;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Path path = folder.getRoot().toPath();
        //path = Paths.get("/Users/zhur/tmp/s/");

        modelConfFactory = new ModelConfFactory();
        JavaFileFactory javaFileFactory = new JavaFileFactory();

        modelConf = createConf(MODEL_CONF_RESOURCE_NAME);
        modelDescendantConf = createConf(MODEL_CONF_DESC_RESOURCE_NAME);
        modelDtoConf = createConf(DTO_RESOURCE_NAME);
        modelWithCopyMethod = createConf(MODEL_CONF_WITH_COPY_METHOD);
        standaloneInterfaceConf = createConf(STANDALONE_INTERFACE_RESOURCE_NAME);
        interfaceWithEnumConf = createConf(INTERFACE_WITH_ENUM_RESOURCE_NAME);

        ModelConfRegistry modelConfRegistry = new ModelConfRegistry(asList(
                modelConf, modelDescendantConf, modelDtoConf, modelWithCopyMethod, standaloneInterfaceConf,
                interfaceWithEnumConf));
        JavaFileSpecFactory javaFileSpecFactory = new JavaFileSpecFactory(modelConfRegistry);

        javaFileSpecFactory.convertAllConfigsToSpecs().stream()
                .map(javaFileFactory::buildJavaFile)
                .forEach(javaFile -> write(javaFile, path));

        classLoader = compile(path);
    }

    private static UpperLevelModelConf createConf(String resourceName) {
        URL resource = ClassLoader.getSystemResource(resourceName);
        return modelConfFactory.createModelConf(resource);
    }

    private static void write(JavaFile javaFile, Path target) {
        try {
            javaFile.writeTo(target);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Before
    public void before() throws Exception {
        adgroupClass = classLoader.loadClass(modelConf.getClassName().toString());
        testAdgroup = (TestAdgroup) adgroupClass.getDeclaredConstructor().newInstance();
        enumClass = classLoader.loadClass(modelConf.getPackageName() + "." + "ExampleStatusCopy");
        enumJsonAnnotatedClass = classLoader.loadClass(modelConf.getPackageName() + "." + "AdGroupType");
        dtoClass = classLoader.loadClass(modelDtoConf.getClassName().toString());
        standaloneInterfaceClass = classLoader.loadClass(standaloneInterfaceConf.getClassName().toString());
        interfaceWithEnumClass = classLoader.loadClass(interfaceWithEnumConf.getClassName().toString());
        enumFromInterfaceClass = classLoader.loadClass(modelConf.getPackageName() + "." + "InterfaceEnumType");

        Method valueOf = enumClass.getMethod("valueOf", String.class);
        enumYes = valueOf.invoke(enumClass, "YES");
        enumNo = valueOf.invoke(enumClass, "NO");
    }

    @Test
    public void isInstanceOfModel() {
        assertThat(testAdgroup).isInstanceOf(Model.class);
    }

    @Test
    public void isInstanceOfModelWithId() {
        assertThat(testAdgroup).isInstanceOf(ModelWithId.class);
    }

    @Test
    public void isInterfaceWithoutId_NotExtendsModelWithId() {
        Class<?> adGroupWithoutIdInterface = getInterfaceClass(adgroupClass, INTERFACE_WITHOUT_ID);
        assertThat(adGroupWithoutIdInterface).isNotNull();

        assertThat(Model.class.isAssignableFrom(adGroupWithoutIdInterface)).isTrue();
        assertThat(ModelWithId.class.isAssignableFrom(adGroupWithoutIdInterface)).isFalse();
    }

    @Test
    public void isInterfaceAnnotations_NotExtendsModelWithId() throws Exception {
        Class<?> adGroupInterface = getInterfaceClass(adgroupClass, INTERFACE_WITH_ANNOTATIONS);
        assertThat(adGroupInterface).isNotNull();

        assertThat(adGroupInterface.getMethod("getGetterAnnotated").isAnnotationPresent(Deprecated.class))
                .isTrue();

        assertThat(adGroupInterface.getMethod("getFieldAnnotated").isAnnotationPresent(Deprecated.class))
                .isFalse();

        assertThat(adGroupInterface.getMethod("getFieldAndGetterAnnotated").isAnnotationPresent(Deprecated.class))
                .isTrue();
    }

    @Test
    public void isInterfaceWithId_ExtendsModelWithId() {
        Class<?> adGroupWithIdInterface = getInterfaceClass(adgroupClass, INTERFACE_WITH_ID);
        assertThat(adGroupWithIdInterface).isNotNull();

        assertThat(ModelWithId.class.isAssignableFrom(adGroupWithIdInterface)).isTrue();
    }

    @Test
    public void isInterfaceWithInheritedId_ExtendsModelWithId() {
        Class<?> adGroupWithIdInterface = getInterfaceClass(adgroupClass, INTERFACE_WITH_INHERITED_ID);
        assertThat(adGroupWithIdInterface).isNotNull();

        assertThat(ModelWithId.class.isAssignableFrom(adGroupWithIdInterface)).isTrue();
    }

    @Test
    public void isInterfaceWithType_HasTypeStaticModelProperty() throws Exception {
        Class<?> adGroupWithTypeInterface = getInterfaceClass(adgroupClass, INTERFACE_WITH_TYPE);
        assertThat(adGroupWithTypeInterface).isNotNull();

        String attributeName = "type";
        String attributeType = "AdGroupType";
        String expectedFieldName = attributeName.toUpperCase();

        Field propertyField = adGroupWithTypeInterface.getField(expectedFieldName);

        assertThat(propertyField.getGenericType().getTypeName())
                .matches(expectedModelPropertyStaticFieldTypeRegex(attributeType));
    }

    @Test
    public void isStandaloneInterface_Loaded() {
        assertThat(standaloneInterfaceClass).isNotNull();
    }

    @Test
    public void isStandaloneInterface_ImplementingItsSupers() {
        Class<?>[] supers = standaloneInterfaceClass.getInterfaces();
        Set<String> superNames = Stream.of(supers).map(Class::getSimpleName).collect(toSet());

        assertThat(superNames).contains(INTERFACE_WITH_TYPE, INTERFACE_WITH_ID);
    }

    @Test
    public void isStandaloneInterface_ImplementedByDescendant() throws Exception {
        Class<?> descendantClass = classLoader.loadClass(modelDescendantConf.getClassName().toString());

        assertThat(standaloneInterfaceClass.isAssignableFrom(descendantClass)).isTrue();
    }

    @Test
    public void testStandaloneInterface_isTypeAnnotated() {
        assertThat(standaloneInterfaceClass.isAnnotationPresent(ParametersAreNonnullByDefault.class))
                .isTrue();
    }

    @Test
    public void testStandaloneInterface_isJsonSubtypesAnnotated() {
        JsonSubTypes ann = standaloneInterfaceClass.getAnnotation(JsonSubTypes.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value())
                .hasSize(1);
    }

    @Test
    public void testStandaloneInterface_isGetterAnnotated() throws Exception {
        assertThat(standaloneInterfaceClass.getMethod("getAnnotated").isAnnotationPresent(Deprecated.class))
                .isTrue();
    }

    @Test
    public void isInterfaceWithEnum_Loaded() {
        assertThat(interfaceWithEnumClass).isNotNull();
    }

    @Test
    public void testInterfaceWithEnum_isTypeAnnotated() {
        assertThat(interfaceWithEnumClass.isAnnotationPresent(ParametersAreNonnullByDefault.class))
                .isTrue();
    }

    @Test
    public void testInterfaceWithEnum_isEnumCreated() {
        assertThat(enumFromInterfaceClass).isNotNull();
    }

    @Test
    public void isInterfaceModelProperty_PresentInDescendant() throws Exception {
        Class<?> descendantClass = classLoader.loadClass(modelDescendantConf.getClassName().toString());

        Field typeModelProperty = descendantClass.getField("TYPE");

        assertThat(typeModelProperty.getGenericType().getTypeName())
                .matches(expectedModelPropertyStaticFieldTypeRegex("AdGroupType"));
    }

    private static String expectedModelPropertyStaticFieldTypeRegex(String attrType) {
        String propertyRawType = ModelProperty.class.getName();
        return format("%s<.+, .+%s>", propertyRawType, attrType);
    }

    @Test
    public void initialCampaignIdIsNull() {
        assertThat(testAdgroup.getCampaignId()).isNull();
    }

    @Test
    public void setCampaignIdWorks() {
        testAdgroup.setCampaignId(123L);
        assertThat(testAdgroup.getCampaignId()).isEqualTo(123);
    }

    @Test
    public void withSetterWorks() {
        assertThat(testAdgroup.withCampaignId(124L)).isSameAs(testAdgroup);
        assertThat(testAdgroup.getCampaignId()).isEqualTo(124);
    }

    @Test
    public void hasModelProperty() throws Exception {
        Object prop = testAdgroup.getClass().getField("CAMPAIGN_ID").get(testAdgroup);
        assertThat(prop).isInstanceOf(ModelProperty.class);
    }

    @Test
    public void testModel_isTypeAnnotated() {
        assertThat(testAdgroup.getClass().isAnnotationPresent(Deprecated.class))
                .isTrue();
        assertThat(testAdgroup.getClass().isAnnotationPresent(ParametersAreNonnullByDefault.class))
                .isTrue();
    }

    @Test
    public void testModel_isGetterAnnotated() throws Exception {
        assertThat(testAdgroup.getClass().getDeclaredField("getterAnnotated").isAnnotationPresent(Deprecated.class))
                .isFalse();
        assertThat(testAdgroup.getClass().getMethod("getGetterAnnotated").isAnnotationPresent(Deprecated.class))
                .isTrue();
    }

    @Test
    public void testModel_isFieldAnnotated() throws Exception {
        assertThat(testAdgroup.getClass().getDeclaredField("fieldAnnotated").isAnnotationPresent(Deprecated.class))
                .isTrue();
        assertThat(testAdgroup.getClass().getMethod("getFieldAnnotated").isAnnotationPresent(Deprecated.class))
                .isFalse();
    }

    @Test
    public void testModel_isFieldAndGetterAnnotated() throws Exception {
        assertThat(testAdgroup.getClass().getDeclaredField("fieldAndGetterAnnotated")
                .isAnnotationPresent(Deprecated.class))
                .isTrue();
        assertThat(testAdgroup.getClass().getMethod("getFieldAndGetterAnnotated")
                .isAnnotationPresent(Deprecated.class))
                .isTrue();
    }

    @Test
    public void testModel_isSetterParameterAnnotated() throws Exception {
        assertThat(testAdgroup.getClass().getDeclaredField("setterParameterAnnotated")
                .isAnnotationPresent(Deprecated.class))
                .isFalse();

        Method setter = testAdgroup.getClass().getMethod("setSetterParameterAnnotated", Long.class);
        assertThat(setter.isAnnotationPresent(Deprecated.class))
                .isFalse();

        assertThat(setter.getParameterAnnotations())
                .hasDimensions(1, 1);
        assertThat(setter.getParameterAnnotations()[0][0])
                .isInstanceOf(Deprecated.class);
    }

    @Test
    public void testModel_isAnnotatedWithParams() throws Exception {
        JsonProperty annotation =
                testAdgroup.getClass().getDeclaredField("annotatedWithParams").getAnnotation(JsonProperty.class);

        assertThat(annotation)
                .isNotNull();

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(annotation.value())
                .isEqualTo("some_name");
        soft.assertThat(annotation.index())
                .isEqualTo(1);
        soft.assertThat(annotation.required())
                .isTrue();
        soft.assertThat(annotation.access())
                .isEqualTo(JsonProperty.Access.READ_ONLY);

        soft.assertAll();
    }

    @Test
    public void arrayTypeWords() throws Exception {
        byte[] ba = new byte[10];
        testAdgroup.setBinaryData(ba);

        ModelProperty modelProperty = (ModelProperty) testAdgroup.getClass().getField("BINARY_DATA").get(testAdgroup);

        @SuppressWarnings("unchecked")
        Object val = modelProperty.get(testAdgroup);

        assertThat(val).isSameAs(ba);
    }

    @Test
    public void modelPropertyWorks() throws Exception {
        testAdgroup.setCampaignId(124L);

        ModelProperty modelProperty = (ModelProperty) testAdgroup.getClass().getField("CAMPAIGN_ID").get(testAdgroup);

        @SuppressWarnings("unchecked")
        Object val = modelProperty.get(testAdgroup);

        assertThat(val).isInstanceOf(Long.class);
        assertThat(val).isEqualTo(124L);
    }

    @Test
    public void enumValuesCheck() {
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(enumClass.isEnum()).isTrue();
        soft.assertThat(enumClass.getEnumConstants().length).isEqualTo(2);

        soft.assertAll();
    }

    @Test
    public void enumValueOfWorks() throws Exception {
        SoftAssertions soft = new SoftAssertions();

        Method valueOf = enumClass.getMethod("valueOf", String.class);
        soft.assertThat(enumYes).isNotNull();
        soft.assertThat(enumNo).isNotNull();
        soft.assertThatThrownBy(() -> valueOf.invoke("none")).isInstanceOf(Exception.class);
        soft.assertThatThrownBy(() -> valueOf.invoke(null)).isInstanceOf(Exception.class);

        soft.assertAll();
    }

    @Test
    public void enumFromSourceWorks() throws Exception {
        SoftAssertions soft = new SoftAssertions();

        Method valueOf = enumClass.getMethod("valueOf", String.class);

        Method fromSource = enumClass.getMethod("fromSource", ExampleStatus.class);
        soft.assertThat(fromSource.invoke(enumClass, ExampleStatus.yes)).isSameAs(enumYes);
        soft.assertThat(fromSource.invoke(enumClass, ExampleStatus.NO)).isSameAs(enumNo);
        soft.assertThat(fromSource.invoke(enumClass, new Object[]{null})).isNull();

        soft.assertAll();
    }

    @Test
    public void enumToSourceStaticWorks() throws Exception {
        SoftAssertions soft = new SoftAssertions();

        Method toSourceStatic = enumClass.getMethod("toSource", enumClass);
        soft.assertThat(toSourceStatic.invoke(enumClass, enumYes).toString()).isEqualTo("yes");
        soft.assertThat(toSourceStatic.invoke(enumClass, enumNo).toString()).isEqualTo("NO");
        soft.assertThat(toSourceStatic.invoke(enumClass, new Object[]{null})).isNull();

        soft.assertAll();
    }

    @Test
    public void isFieldAnnotatedProperly() throws Exception {
        Field idField = adgroupClass.getDeclaredField("id");
        JsonProperty ann = idField.getAnnotation(JsonProperty.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).isEqualTo("id_json");

        JsonInclude annInclude = idField.getAnnotation(JsonInclude.class);
        assertThat(annInclude).isNotNull();
        assertThat(annInclude.value()).isEqualTo(JsonInclude.Include.NON_NULL);
    }

    @Test
    public void isFieldAliasWorks() throws Exception {
        long value = 1234;
        Method setId = adgroupClass.getMethod("setId", Long.class);
        Method getPid = adgroupClass.getMethod("getPid");

        Object o = adgroupClass.getDeclaredConstructor().newInstance();
        setId.invoke(o, value);
        Long result = (Long) getPid.invoke(o);

        assertThat(result).isEqualTo(value);
    }

    @Test
    public void isFieldAliasWorks_Reverse() throws Exception {
        long value = 1234;
        Method setId = adgroupClass.getMethod("setPid", Long.class);
        Method getPid = adgroupClass.getMethod("getId");

        Object o = adgroupClass.getDeclaredConstructor().newInstance();
        setId.invoke(o, value);
        Long result = (Long) getPid.invoke(o);

        assertThat(result).isEqualTo(value);
    }

    @Test
    public void isPrimitiveIdFieldExists() throws Exception {
        assertThat(dtoClass.getMethod("getId").getReturnType()).isEqualTo(long.class);
        assertThat(dtoClass.getDeclaredField("id").getType()).isEqualTo(long.class);
    }

    @Test
    public void descendantTest() throws Exception {
        Class<?> descendantClass = classLoader.loadClass(modelDescendantConf.getClassName().toString());
        assertThat(descendantClass.getSuperclass()).isEqualTo(adgroupClass);
    }

    @Test
    public void descendantWithMethodInheritanceTest() throws Exception {
        Class<?> descendantClass = classLoader.loadClass(modelDescendantConf.getClassName().toString());
        assertThat(descendantClass.getDeclaredConstructor().newInstance()).isInstanceOf(TestAdgroupDescendant.class);
    }

    @Test
    public void isEnumAnnotatedProperly() throws Exception {
        SoftAssertions soft = new SoftAssertions();
        JsonProperty baseAnnotation = enumJsonAnnotatedClass.getDeclaredField("BASE").getAnnotation(JsonProperty.class);
        JsonProperty dynamicAnnotation =
                enumJsonAnnotatedClass.getDeclaredField("DYNAMIC").getAnnotation(JsonProperty.class);
        soft.assertThat(baseAnnotation).isNotNull();
        soft.assertThat(baseAnnotation.value()).isEqualTo("baseeeee");
        soft.assertThat(dynamicAnnotation).isNull();
        soft.assertAll();
    }

    @Test
    @SuppressWarnings("squid:S2970")    // Sonar не видит выражение assertThat в лямбде и ругается, что нет ассертов
    public void areCorrespondingGeneratedPropertiesEqual() throws Exception {
        Class<?> descendantClass = classLoader.loadClass(modelDescendantConf.getClassName().toString());
        Class<?> adGroupWithInheritedIdInterface = getInterfaceClass(adgroupClass, INTERFACE_WITH_INHERITED_ID);
        Class<?> adGroupWithTypeInterface = getInterfaceClass(adgroupClass, INTERFACE_WITH_TYPE);

        SoftAssertions soft = new SoftAssertions();

        for (Field field : checkedFields()) {
            Object adgroupClassProperty = field.get(adgroupClass);

            Stream.of(descendantClass, adGroupWithInheritedIdInterface,
                            adGroupWithTypeInterface)
                    .map(c -> getFieldValue(field, c))
                    .forEach(prop -> soft.assertThat(prop).isEqualTo(adgroupClassProperty));
        }

        soft.assertAll();
    }

    @Test
    public void checkGenerateGetModelPropsMethodForClass() throws Exception {
        Class<?> loadClass = classLoader.loadClass(modelConf.getClassName().toString());

        List<String> methodNames = StreamEx.of(loadClass.getMethods()).map(Method::getName).toList();
        assertThat(methodNames).contains(ALL_MODEL_PROPERTIES_METHOD_NAME);
    }

    @Test
    public void checkGenerateGetModelPropsMethodForDescendantClass() throws Exception {
        Class<?> loadClass = classLoader.loadClass(modelDescendantConf.getClassName().toString());

        List<String> methodNames = StreamEx.of(loadClass.getMethods()).map(Method::getName).toList();
        assertThat(methodNames).contains(ALL_MODEL_PROPERTIES_METHOD_NAME);
    }

    @Test
    public void checkGenerateGetModelPropsMethodFoInterface() throws Exception {
        Class<?> loadClass = classLoader.loadClass(standaloneInterfaceConf.getClassName().toString());

        List<String> methodNames = StreamEx.of(loadClass.getMethods()).map(Method::getName).toList();
        assertThat(methodNames).contains(ALL_MODEL_PROPERTIES_METHOD_NAME);
    }

    @Test
    public void checkNoGetModelPropsMethod() throws Exception {
        Class<?> loadClass = classLoader.loadClass(modelDtoConf.getClassName().toString());

        List<String> methodNames = StreamEx.of(loadClass.getMethods()).map(Method::getName).toList();
        assertThat(methodNames).doesNotContain(ALL_MODEL_PROPERTIES_METHOD_NAME);
    }

    @Test
    public void checkGetModelPropsMethod() throws Exception {
        Class<?> descendantClass = classLoader.loadClass(modelDescendantConf.getClassName().toString());

        Method getModelPropertiesMethod = descendantClass.getDeclaredMethod(ALL_MODEL_PROPERTIES_METHOD_NAME);
        Set<AttrConf> expectedProps = new HashSet<>(modelDescendantConf.getAttrs());
        expectedProps.addAll(filterList(modelConf.getAttrs(), t -> t.getAliasTo().isEmpty()));

        @SuppressWarnings("unchecked")
        Set<ModelProperty> props = (Set<ModelProperty>) getModelPropertiesMethod.invoke(null);
        assertThat(props).hasSameSizeAs(expectedProps);
    }

    @Test
    public void isGenerateCopyMethod() throws Exception {
        Class<?> loadClass = classLoader.loadClass(modelWithCopyMethod.getClassName().toString());
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(loadClass.getDeclaredConstructor().newInstance()).isInstanceOf(Copyable.class);

        Method copyMethod = loadClass.getDeclaredMethod(COPY_METHOD_NAME);
        soft.assertThat(copyMethod).isNotNull();
        soft.assertThat(copyMethod.getReturnType()).isEqualTo(loadClass);
        soft.assertAll();
    }

    @Test
    public void checkCopyMethod() throws Exception {
        Class<?> loadClass = classLoader.loadClass(modelWithCopyMethod.getClassName().toString());
        Object instance = loadClass.getDeclaredConstructor().newInstance();
        Method copyMethod = loadClass.getDeclaredMethod(COPY_METHOD_NAME);
        Object copyInstance = copyMethod.invoke(instance);

        assertThat(copyInstance).isExactlyInstanceOf(loadClass)
                .isNotSameAs(instance)
                .isEqualToComparingFieldByField(instance);
    }

    @Test
    public void isNotGenerateCopyMethodByDefault() throws Exception {
        //modelConf - without property generateCopyMethod
        Class<?> loadClass = classLoader.loadClass(modelConf.getClassName().toString());

        assertThat(loadClass.getDeclaredConstructor().newInstance()).isNotInstanceOf(Copyable.class);
    }

    private Iterable<Field> checkedFields() {
        return Stream.of("ID", "TYPE", "CAMPAIGN_ID").map(this::getAdgroupClassField).collect(toList());
    }

    private Field getAdgroupClassField(String name) {
        try {
            return adgroupClass.getField(name);
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Object getFieldValue(Field f, Class<?> c) {
        try {
            return f.get(c);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nullable
    private static Class<?> getInterfaceClass(Class<?> generatedClass, String interfaceName) {
        return Arrays.stream(generatedClass.getInterfaces())
                .filter(i -> i.getName().endsWith(interfaceName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Компилируем все java-классы из указанной директории
     *
     * @return - ClassLoader, настроенный для работы с этой иректорией
     */
    private static ClassLoader compile(Path path) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null);

        List<File> files;
        try (Stream<Path> walk = Files.walk(path)) {
            files = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .collect(toList());
        }

        JavaCompiler.CompilationTask task = compiler.getTask(null, manager, null, null, null,
                manager.getJavaFileObjectsFromFiles(files));

        Boolean result = task.call();
        if (result == null || !result) {
            throw new RuntimeException("Compilation failed (errors in stderr).");
        }

        URL classUrl = path.toUri().toURL();
        return new URLClassLoader(new URL[]{classUrl});
    }
}
