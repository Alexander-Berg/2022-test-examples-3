package ru.yandex.market.framework.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.swagger.v3.core.util.Yaml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import ru.yandex.market.framework.generator.properties.serviceyaml.ServiceYamlGenTime;
import ru.yandex.market.framework.generator.properties.serviceyaml.clients.Client;

import static ru.yandex.market.framework.generator.GeneratorParameters.arcadiaRoot;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarketJavaGeneratorTest {
    @TempDir
    Path tempDir;

    private Path arcadiaDir;
    private Path output;
    private Path frameworkDir;
    private String pathToService;

    @BeforeEach
    public void setUp() {
        output = tempDir.resolve("output");

        arcadiaDir = Path.of(ru.yandex.devtools.test.Paths.getSourcePath(""));
        // workaround to make this test working from Idea
        if (arcadiaDir.startsWith("null")){
            arcadiaDir = Path.of("../../../../../");
        }

        frameworkDir = arcadiaDir.resolve(MarketJavaGenerator.FRAMEWORK_PATH.substring(1));
        pathToService = frameworkDir.resolve("src/test/resources/test_service").toString();
    }

    @Test
    public void testInitializeVariables() throws IOException {
        GeneratorParameters.initializeVariables(new String[]{pathToService, arcadiaDir.toString(), output.toString()});
        Utils.createPaths();

        Assertions.assertEquals(pathToService, GeneratorParameters.curDir);
        Assertions.assertEquals(arcadiaDir.toString(), GeneratorParameters.arcadiaRoot);
        Assertions.assertEquals(output.toString(), GeneratorParameters.outputDir);
        Assertions.assertEquals(arcadiaDir.toString() + GeneratorParameters.FRAMEWORK_PATH, GeneratorParameters.frameworkDir);
        Assertions.assertTrue(Files.exists(Path.of(output.toString(), "yaml-properties")));
    }

    @Test
    public void testCopyServiceYamlInBuildDir() throws IOException {
        GeneratorParameters.initializeVariables(
            new String[]{pathToService, arcadiaDir.toString(), output.toString()}
        );
        Utils.createPaths();
        Utils.copyServiceYamlInBuildDir();

        ServiceYamlGenTime serviceYaml = ServiceYamlReader.readServiceYaml(
            output.toString() + "/yaml-properties/service.yaml"
        );

        Assertions.assertEquals(serviceYaml, GeneratorParameters.serviceYaml);
    }

    @Test
    public void testValidateServiceYaml() throws IOException {
        GeneratorParameters.initializeVariables(
            new String[]{
                frameworkDir.resolve("src/test/resources/validateServiceYaml/service_without_trace/").toString(),
                arcadiaDir.toString(),
                output.toString()}
        );

        IllegalArgumentException exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            ServiceYamlReader::validateServiceYaml
        );

        Assertions.assertEquals("You must specify trace module in service.yaml", exception.getMessage());

        GeneratorParameters.initializeVariables(
            new String[]{
                frameworkDir.resolve("src/test/resources/validateServiceYaml/service_without_name/").toString(),
                arcadiaDir.toString(),
                output.toString()}
        );

        exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            ServiceYamlReader::validateServiceYaml
        );

        Assertions.assertEquals("You must specify java_service.service_name in service.yaml", exception.getMessage());
    }

    @Test
    public void testValidateServiceYamlWithOpenapiAndServiceYaml() throws IOException {
        GeneratorParameters.initializeVariables(
            new String[]{
                frameworkDir.resolve("src/test/resources/validateServiceYaml" +
                    "/service_with_openapi_and_service_path_in_client").toString(),
                arcadiaDir.toString(),
                output.toString()}
        );

        IllegalArgumentException exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            ServiceYamlReader::validateServiceYaml
        );

        Assertions.assertEquals("You must specify only openapi_spec_path or service_yaml_path in clients",
            exception.getMessage());

    }

    @Test
    public void testGenerateControllers() throws IOException {
        GeneratorParameters.initializeVariables(
            new String[]{frameworkDir.resolve("src/test/resources/generateControllers/service_without_api/").toString(),
            arcadiaDir.toString(),
            output.toString()}
        );
        Utils.createPaths();
        ControllerGenerator.generateControllers();

        Assertions.assertFalse(Files.exists(Path.of(output.toString(), "ru")));

        GeneratorParameters.initializeVariables(
            new String[]{frameworkDir.resolve("src/test/resources/generateControllers/service_with_api/").toString(),
                arcadiaDir.toString(),
                output.toString()}
        );
        ControllerGenerator.generateControllers();

        Assertions.assertFalse(Files.exists(Path.of(output.toString(), "server")));
        Assertions.assertTrue(Files.exists(Path.of(output.toString(), "ru")));
        Assertions.assertTrue(Files.exists(Path.of(output.toString(), "ru/yandex/mj/generated/server/api/TestApiApi.java")));
    }

    @Test
    public void testGenerateClient() throws IOException {
        GeneratorParameters.initializeVariables(new String[]{pathToService, arcadiaDir.toString(), output.toString()});

        Client client = GeneratorParameters.serviceYaml.getClients().getList().get("test_service");

        ClientGenerator.generateClient(
            "test_service",
            ClientGenerator.getClientOpenApiSpecPath(client),
            false,
            true
        );

        Assertions.assertFalse(Files.exists(Path.of(output.toString(), "test_service")));
        Assertions.assertTrue(Files.exists(Path.of(output.toString(), "ru")));
        Assertions.assertTrue(Files.exists(Path.of(output.toString(), "ru/yandex/mj/generated/client/test_service/api/TestApiApi.java")));
    }

    @Test
    public void testGenerateClientWithServiceYamlPath() throws IOException {
        GeneratorParameters.initializeVariables(
            new String[]{
                frameworkDir.resolve("src/test/resources/test_service2").toString(),
                arcadiaDir.toString(),
                output.toString()}
        );
        Client client = GeneratorParameters.serviceYaml.getClients().getList().get("test_service");

        ClientGenerator.generateClient(
            "test_service",
            ClientGenerator.getClientOpenApiSpecPath(client),
            false,
            true
        );

        Assertions.assertFalse(Files.exists(Path.of(output.toString(), "test_service")));
        Assertions.assertTrue(Files.exists(Path.of(output.toString(), "ru")));
        Assertions.assertTrue(Files.exists(Path.of(output.toString(), "ru/yandex/mj/generated/client/test_service/api/TestApiApi.java")));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testHandleModules() throws IOException {
        GeneratorParameters.initializeVariables(new String[]{pathToService, arcadiaDir.toString(), output.toString()});

        MarketJavaGenerator.handleModules();

        Assertions.assertTrue(Files.exists(Path.of(output.toString(), "/frames/src")));

        Assertions.assertEquals(
            new HashSet<>(Arrays.asList(Path.of(
                arcadiaDir.toString(),
                MarketJavaGenerator.FRAMEWORK_PATH,
                "/frames/bazinga-frame/src/main/java/ru/yandex/market/javaframework/frames/bazinga/config"
            ).toFile().list())),
            new HashSet<>(Arrays.asList(Path.of(
                output.toString(),
                "frames/src",
                "main/java/ru/yandex/market/javaframework/frames/bazinga/config"
            ).toFile().list()))
        );
    }

    @Test
    public void testSaveMjServiceProperties() throws IOException {
        GeneratorParameters.initializeVariables(new String[]{pathToService, arcadiaDir.toString(), output.toString()});
        Utils.createPaths();

        Map<String, Object> clientPropertiesMap = new HashMap<>();
        clientPropertiesMap.put("openApiYaml", "rawOpenApi");

        Client client = GeneratorParameters.serviceYaml.getClients().getList().get("test_service");
        ClientGenerator.saveMjServiceProperties(
            "test_service",
            ClientGenerator.getClientOpenApiSpecPath(client),
            ClientGenerator.getClientServiceYamlPath(client).get()
        );

        Map<String, Map<String, ?>> map = GeneratorParameters.mapper.readValue(Files.newInputStream(
            Path.of(output.toString(), "yaml-properties", "client_test_service.yaml")
        ), Map.class);

        Assertions.assertTrue(map.get("client-services-properties").containsKey("test_service"));
    }

    /*@Test
    public void testProcessClientsTvmRefs() throws IOException {
        Map<String, Map<String, ?>> clientsServiceYamls = new HashMap<>();
        Map<String, String> fakeRawClientServiceYaml = new HashMap<>();
        fakeRawClientServiceYaml.put("fakeKey", "fakeValue");
        clientsServiceYamls.put("fake_yaml", fakeRawClientServiceYaml);

        GeneratorParameters.initializeVariables(
            new String[]{
                frameworkDir.resolve("src/test/resources/processClientsTvmRefs/service_with_prestable"
            ).toString(), arcadiaDir.toString(), output.toString()}
        );
        Map<String, ?> testServiceMap = GeneratorParameters.mapper.readValue(
            new File(arcadiaDir.toString() + "/market/infra/java-application/mj/v1/src/test/resources/test_service/service.yaml"), Map.class
        );

        ClientGenerator.processClientsTvmRefs(clientsServiceYamls);

        Assertions.assertEquals(fakeRawClientServiceYaml, clientsServiceYamls.get("fake_yaml"));
        Assertions.assertEquals(
            testServiceMap,
            clientsServiceYamls.get("test_service")
        );
        Assertions.assertFalse(clientsServiceYamls.containsKey("test_service-resolved-prestable"));

        clientsServiceYamls.remove("test_service");

        GeneratorParameters.initializeVariables(
            new String[]{
                frameworkDir.resolve("src/test/resources/processClientsTvmRefs/service_without_prestable"
            ).toString(), arcadiaDir.toString(), output.toString()}
        );
        ClientGenerator.processClientsTvmRefs(clientsServiceYamls);

        Assertions.assertEquals(fakeRawClientServiceYaml, clientsServiceYamls.get("fake_yaml"));
        Assertions.assertEquals(
            testServiceMap,
            clientsServiceYamls.get("test_service")
        );
        Assertions.assertEquals(
            ServiceYamlReader.readServiceYamlToMap(
                arcadiaDir.toString() + "/market/infra/java-application/mj/v1/src/test/resources/test_service/service.yaml", Environments.PRESTABLE
            ),
            clientsServiceYamls.get("test_service-resolved-prestable")
        );
    }

    @Test
    public void testReadServiceYamlModuleWithAllEnvs() {
        final TvmEnvironmentProperties tvmEnvironmentProperties =
            ServiceYamlReader.readServiceYamlModuleWithAllEnvs(
                pathToService + "/service.yaml", "tvm", TvmEnvironmentProperties.class
            );

        Assertions.assertEquals(
            new TvmSettings(null, null, Collections.emptySet(), Collections.emptySet(), false, true),
            tvmEnvironmentProperties.getEnv().get("local")
        );

        Assertions.assertEquals(
            new TvmSettings(
                new TvmIdRef(7777, null),
                null,
                new HashSet<>(Arrays.asList(
                    new TvmIdRef(647, null),
                    new TvmIdRef(424, null),
                    new TvmIdRef(null, "market/infra/java-application/templates/mj-template/service.yaml")
                )),
                new HashSet<>(Arrays.asList(
                    new TvmIdRef(2243, null),
                    new TvmIdRef(42213, null)
                )),
                false,
                false),
            tvmEnvironmentProperties.getEnv().get("testing")
        );

        Assertions.assertEquals(
            new TvmSettings(
                new TvmIdRef(745645, null),
                null,
                new HashSet<>(Arrays.asList(
                    new TvmIdRef(767, null),
                    new TvmIdRef(24789, null)
                )),
                new HashSet<>(Arrays.asList(
                    new TvmIdRef(224, null),
                    new TvmIdRef(423, null)
                )),
                false,
                false),
            tvmEnvironmentProperties.getEnv().get("prestable")
        );

        Assertions.assertEquals(
            new TvmSettings(
                new TvmIdRef(8888, null),
                null,
                new HashSet<>(Arrays.asList(
                    new TvmIdRef(645647, null),
                    new TvmIdRef(32343, null),
                    new TvmIdRef(null, "market/infra/java-application/templates/mj-template/service.yaml")
                )),
                new HashSet<>(Arrays.asList(
                    new TvmIdRef(423, null),
                    new TvmIdRef(224, null)
                )),
                false,
                false),
            tvmEnvironmentProperties.getEnv().get("production")
        );

        Assertions.assertNull(tvmEnvironmentProperties.getSelf());
        Assertions.assertNull(tvmEnvironmentProperties.getSecret());
        Assertions.assertEquals(Collections.singleton(new TvmIdRef(2029758, null)), tvmEnvironmentProperties.getSources());
        Assertions.assertEquals(Collections.singleton(new TvmIdRef(2029758, null)), tvmEnvironmentProperties.getDestinations());
        Assertions.assertFalse(tvmEnvironmentProperties.isServerTvmDisabled());
        Assertions.assertFalse(tvmEnvironmentProperties.isClientsTvmDisabled());
    }

    @Test
    public void testReadServiceYaml() {
        String tvmPath = frameworkDir.resolve("src/test/resources/readServiceYaml/service.yaml").toString();

        ServiceYaml serviceYaml = ServiceYamlReader.readServiceYaml(tvmPath);
        Assertions.assertEquals(new TvmIdRef(2, null), serviceYaml.getTvm().getSelf());

        serviceYaml = ServiceYamlReader.readServiceYaml(tvmPath, Environments.LOCAL);
        Assertions.assertEquals(new TvmIdRef(2, null), serviceYaml.getTvm().getSelf());

        serviceYaml = ServiceYamlReader.readServiceYaml(tvmPath, Environments.TESTING);
        Assertions.assertEquals(new TvmIdRef(3, null), serviceYaml.getTvm().getSelf());

        serviceYaml = ServiceYamlReader.readServiceYaml(tvmPath, Environments.PRESTABLE);
        Assertions.assertEquals(new TvmIdRef(1, null), serviceYaml.getTvm().getSelf());

        serviceYaml = ServiceYamlReader.readServiceYaml(tvmPath, Environments.PRODUCTION);
        Assertions.assertEquals(new TvmIdRef(5, null), serviceYaml.getTvm().getSelf());

        Assertions.assertEquals("read-test", serviceYaml.getJavaService().getServiceName());
        Assertions.assertEquals(Module.FRAMEWORK_TEST_SERVICE, serviceYaml.getTrace().getModule());
    }*/

    @Test
    public void testGetAndValidateOpenApiProperties() throws IOException {
        GeneratorParameters.initializeVariables(
            new String[]{pathToService, arcadiaDir.toString(), output.toString()}
        );

        Map<String, Object> properties = ControllerGenerator.getAndValidateOpenApiProperties(
            frameworkDir.resolve("src/test/resources/getAndValidateOpenApiProperties/api_with_urls.yaml").toString()
        );
        Assertions.assertEquals("http://test-api.yandex.net", ((Map) properties.get("servers")).get("url"));

        Exception exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ControllerGenerator.getAndValidateOpenApiProperties(
                frameworkDir.resolve("src/test/resources/getAndValidateOpenApiProperties/api_without_http.yaml").toString()
            )
        );
        Assertions.assertTrue(exception.getMessage().contains("must be started with 'http://' or 'https://'"));

        exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ControllerGenerator.getAndValidateOpenApiProperties(
                frameworkDir.resolve("src/test/resources/getAndValidateOpenApiProperties/api_without_x_servers.yaml").toString()
            )
        );
        Assertions.assertTrue(exception.getMessage().contains("There is no 'x-servers' field in openapi spec"));
    }

    @Test
    public void testGetAndValidateOpenApiPropertiesForRateLimiters() throws IOException {
        GeneratorParameters.initializeVariables(
            new String[]{frameworkDir.resolve("src/test/resources/getAndValidateOpenApiPropertiesForRateLimiters/" +
                "service_with_missing_ratelimiter/").toString(),
                arcadiaDir.toString(),
                output.toString()}
        );

        Exception exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ControllerGenerator.getAndValidateOpenApiProperties(
                frameworkDir.resolve(
                    "src/test/resources/getAndValidateOpenApiPropertiesForRateLimiters/service_with_missing_ratelimiter/src/main/resources/openapi/api/api.yaml"
                ).toString()
            )
        );
        Assertions.assertEquals("There are some rate limiters in api.yaml ([customRateLimiter]), that could not be found in service.yaml", exception.getMessage());

        GeneratorParameters.initializeVariables(
            new String[]{frameworkDir.resolve("src/test/resources/getAndValidateOpenApiPropertiesForRateLimiters/" +
                "service_without_ratelimiters/").toString(),
                arcadiaDir.toString(),
                output.toString()}
        );

        exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ControllerGenerator.getAndValidateOpenApiProperties(
                frameworkDir.resolve(
                    "src/test/resources/getAndValidateOpenApiPropertiesForRateLimiters/service_without_ratelimiters/src/main/resources/openapi/api/api.yaml"
                ).toString()
            )
        );
        Assertions.assertEquals("There are some rate limiters in api.yaml ([customRateLimiter, defaultRateLimiter]), but there is no rate limiters in service.yaml", exception.getMessage());

        GeneratorParameters.initializeVariables(
            new String[]{frameworkDir.resolve("src/test/resources/getAndValidateOpenApiPropertiesForRateLimiters/" +
                "service_with_ratelimiters/").toString(),
                arcadiaDir.toString(),
                output.toString()}
        );

        Assertions.assertDoesNotThrow(
            () -> ControllerGenerator.getAndValidateOpenApiProperties(
                frameworkDir.resolve(
                    "src/test/resources/getAndValidateOpenApiPropertiesForRateLimiters/service_with_ratelimiters/src/main/resources/openapi/api/api.yaml"
                ).toString()
            )
        );
    }

    @Test
    public void testGenerateTestYamlProperties() throws IOException {
        String pathToTestServiceYaml = "src/test/resources/testGenerator/generate_test_yaml_properties/";
        GeneratorParameters.initializeVariables(
            new String[]{
                frameworkDir.resolve(pathToTestServiceYaml).toString(),
                arcadiaDir.toString(),
                output.toString()}
        );

        String selfClientName = "self";

        Utils.createPaths();
        MarketTestJavaGenerator.addTestProperties(selfClientName, true);

        ServiceYamlGenTime testServiceYaml =
            ServiceYamlReader.readServiceYaml(output + "/yaml-properties/service.yaml");

        Assertions.assertEquals(1, testServiceYaml.getClients().getList().size());
        Assertions.assertEquals(
            GeneratorParameters.openApiSpecPath.substring((GeneratorParameters.arcadiaRoot + File.separator).length()),
            testServiceYaml.getClients().getList().get(selfClientName).getOpenapiSpecPath());
        Assertions.assertTrue(Files.exists(
            Path.of(output.toString(), "/yaml-properties/client_" + selfClientName + ".yaml")));

    }

}