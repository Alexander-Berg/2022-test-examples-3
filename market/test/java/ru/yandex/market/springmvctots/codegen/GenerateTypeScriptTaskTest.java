package ru.yandex.market.springmvctots.codegen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Scanner;

import byteresponse.ByteResponseController;
import convert.args_to_object.ConvertArgsToObjectController;
import extracontrollerscontroller.SimpleController1;
import extracontrollerscontrollerother.SimpleController2;
import org.assertj.core.api.Assertions;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author yuramalinov
 * @created 24.09.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class GenerateTypeScriptTaskTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testSimpleGeneration() throws IOException {
        assertGeneration("simple", "simple-definitions.ts");
    }

    @Test
    public void testUrlsGeneration() throws IOException {
        assertGeneration("requesturls", "requesturls-definitions.ts");
    }

    @Test
    public void testResponseEntity() throws IOException {
        assertGeneration("responseentity", "responseentity-definitions.ts");
    }

    @Test
    public void testByteResponse() throws IOException {
        assertGeneration("byteresponse", "definitions.ts");
    }

    @Test
    public void testIgnore() throws IOException {
        assertGeneration("ignore", "definitions.ts");
    }

    @Test
    public void testFormData() throws IOException {
        assertGeneration("formdata", "definitions.ts");
    }

    @Test
    public void testRequestBody() throws IOException {
        assertGeneration("requestbody", "definitions.ts");
    }

    @Test
    public void testOptionalArgs() throws IOException {
        assertGeneration("optionalargs", "definitions.ts");
    }

    @Test
    public void testConverters() throws IOException {
        assertGeneration("converters", "definitions.ts");
    }

    @Test
    public void testStringify() throws IOException {
        assertGeneration("stringify", "definitions.ts");
    }

    @Test
    public void testTsReturnType() throws IOException {
        assertGeneration("tsreturntype", "definitions.ts");
    }

    @Test
    public void testTsReturnTypeArray() throws IOException {
        assertGeneration("tsreturntypearray", "definitions.ts");
    }

    @Test
    public void testNullableField() throws IOException {
        assertGeneration("nullablefield", "definitions.ts");
    }

    @Test
    public void testConvertArgsToObjectCase1() throws IOException {
        CustomSettings customSettings = new CustomSettings();
        customSettings.convertArgsToObject = true;
        customSettings.maxArgs = 4;
        customSettings.maxOptionalArgs = 2;
        assertGeneration("convert/args_to_object", "default.ts", customSettings);
    }

    @Test
    public void testConvertArgsToObjectCase1Multiple() throws IOException {
        CustomSettings customSettings = new CustomSettings();
        customSettings.convertArgsToObject = true;
        customSettings.maxArgs = 4;
        customSettings.maxOptionalArgs = 2;
        assertGeneration("convert/args_to_object", "default.ts", customSettings);
        assertGeneration("convert/args_to_object", "default.ts", customSettings);
        assertGeneration("convert/args_to_object", "default.ts", customSettings);
        assertGeneration("convert/args_to_object", "default.ts", customSettings);
    }

    @Test
    public void testConvertArgsToObjectCase2() throws IOException {
        CustomSettings customSettings = new CustomSettings();
        customSettings.convertArgsToObject = true;
        customSettings.maxArgs = 3;
        customSettings.maxOptionalArgs = 2;
        assertGeneration("convert/args_to_object", "converted.ts", customSettings);
    }

    @Test
    public void testConvertArgsToObjectCase3() throws IOException {
        CustomSettings customSettings = new CustomSettings();
        customSettings.convertArgsToObject = true;
        customSettings.maxArgs = 4;
        customSettings.maxOptionalArgs = 1;
        assertGeneration("convert/args_to_object", "converted.ts", customSettings);
    }

    @Test
    public void testReplaceType() throws IOException {
        CustomSettings customSettings = new CustomSettings();
        customSettings.skipTypeScriptClasses = Collections.singleton("SomeTooSmartType");
        customSettings.customTypeAliases = Collections.singletonMap("SomeTooSmartType", "string");
        assertGeneration("replacetype", "definitions.ts", customSettings);
    }

    @Test
    public void testCustomControllerAnnotation() throws IOException {
        assertGeneration("customannotation", "custom-annotation-definitions.ts");
    }

    @Test
    public void testLocalDateInUriGeneration() throws IOException {
        assertGeneration("localdate", "localdate-in-uri-definitions.ts");
    }

    @Test
    public void testExtraControllers() throws IOException {
        Assertions.assertThatThrownBy(() ->
            assertGeneration("extracontrollers", "extracontrollers.ts")
        ).isInstanceOf(ComparisonFailure.class);

        assertGeneration("extracontrollers", "extracontrollers.ts",
            new CustomSettings().setExtraControllers(
                SimpleController1.class,
                SimpleController2.class
            ));
    }

    @Test
    public void testSeveralPackages() throws IOException {
        CustomSettings settings = new CustomSettings();
        settings.setBasePackageNames(ByteResponseController.class, ConvertArgsToObjectController.class);
        settings.setIncludeCommonHeaders(false);
        settings.importDeclarations = Collections.singletonList(
            "import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions}" +
                " from '../../../main/resources/common'");

        assertGeneration(settings, "severalpackages/severalpackages-definitions.ts");
    }

    @Test
    public void testDeprecation() throws IOException {
        assertGeneration("deprecation", "definitions.ts");
    }

    private void assertGeneration(String packageName, String resultResourceName)
        throws IOException {
        assertGeneration(packageName, resultResourceName, new CustomSettings());
    }

    private void assertGeneration(String packageName, String resultResourceName, CustomSettings customSettings)
        throws IOException {
        customSettings.basePackageNames = Collections.singletonList(packageName);
        customSettings.includeCommonHeaders = false;
        customSettings.importDeclarations = Collections.singletonList(
            "import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions}" +
                " from '../../../main/resources/common'");

        assertGeneration(customSettings, packageName + "/" + resultResourceName);
    }

    private void assertGeneration(CustomSettings customSettings, String fullResourceName) throws IOException {
        Path outputPath = getOutputPath("out/definitions.ts");
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            MvcToTsTask mvcToTsTask = new MvcToTsTask(outputPath,
                new AppDefinitionsGenerator(customSettings, contextClassLoader));
            mvcToTsTask.run();
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertThat(getContent(outputPath)).isEqualTo(getResource(fullResourceName));
    }

    private Path getOutputPath(String name) {
        return temporaryFolder.getRoot().toPath().resolve(name);
    }

    private String getContent(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String getResource(String name) {
        InputStream resource = getClass().getClassLoader().getResourceAsStream(name);
        if (resource == null) {
            throw new IllegalArgumentException("Can't find resource '" + name + "'");
        }
        Scanner scanner = new Scanner(resource).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
