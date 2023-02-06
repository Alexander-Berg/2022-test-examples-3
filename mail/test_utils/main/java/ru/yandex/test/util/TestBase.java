package ru.yandex.test.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import ru.yandex.concurrent.SingleNamedThreadFactory;
import ru.yandex.concurrent.ThreadFactoryConfig;
import ru.yandex.function.GenericSupplier;
import ru.yandex.function.NullConsumer;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonWriterBase;
import ru.yandex.json.writer.Utf8JsonWriter;
import ru.yandex.logger.HandlersManager;
import ru.yandex.logger.ImmutableLoggerConfig;
import ru.yandex.logger.LoggerConfigDefaults;
import ru.yandex.logger.LoggerFileConfigDefaults;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.string.CollectionParser;
import ru.yandex.parser.string.EnumParser;
import ru.yandex.parser.string.LocaleParser;
import ru.yandex.parser.string.NonEmptyValidator;
import ru.yandex.parser.string.PositiveIntegerValidator;
import ru.yandex.parser.string.PositiveLongValidator;
import ru.yandex.parser.string.StringLengthValidator;
import ru.yandex.parser.uri.OpenApiDocumentation;
import ru.yandex.parser.uri.OpenApiParameter;
import ru.yandex.parser.uri.OpenApiPath;
import ru.yandex.util.system.CPUMonitor;
import ru.yandex.util.system.RusageMonitor;

public class TestBase {
    static {
        System.setProperty("VERBOSE_MEM_GC_LOG", Boolean.FALSE.toString());
    }

    private static final long DEFAULT_TESTS_INTERVAL = 100L;
    private static final RusageMonitor RUSAGE_MONITOR = new RusageMonitor(
        1000L,
        50L,
        NullConsumer.INSTANCE,
        new SingleNamedThreadFactory(
            new ThreadFactoryConfig("RusageMonitor").daemon(true)));
    protected static final StatusThread STATUS_THREAD = new StatusThread();

    static {
        RUSAGE_MONITOR.start();
        STATUS_THREAD.start();
    }

    public final PrefixedLogger logger;
    @Rule
    public final TestInfo testName;

    private final boolean cleanupBetweenTests;
    private final long sleepInterval;
    private final JsonSchema defaultJsonSchema;
    private long startTime = 0L;
    private JsonSchema currentSchema;
    private Map<String, Object> context;

    protected TestBase() {
        this(
            true,
            DEFAULT_TESTS_INTERVAL,
            (JsonSchema) null);
    }

    protected TestBase(
        final boolean cleanupBetweenTests,
        final long sleepInterval)
    {
        this(
            cleanupBetweenTests,
            sleepInterval,
            (JsonSchema) null);
    }

    protected TestBase(
        final boolean cleanupBetweenTests,
        final long sleepInterval,
        final String defaultJsonSchema)
    {
        this(
            cleanupBetweenTests,
            sleepInterval,
            parseJsonSchema(defaultJsonSchema));
    }

    protected TestBase(
        final boolean cleanupBetweenTests,
        final long sleepInterval,
        final JsonSchema defaultJsonSchema)
    {
        logger = STATUS_THREAD.logger;
        testName = new TestInfo(logger);
        this.cleanupBetweenTests = cleanupBetweenTests;
        this.sleepInterval = sleepInterval;
        this.defaultJsonSchema = defaultJsonSchema;
    }

    public static JsonSchema parseJsonSchema(final String schema) {
        if (schema == null) {
            return null;
        } else {
            try {
                return JsonSchemaFactory.byDefault()
                    .getJsonSchema(JsonLoader.fromString(schema));
            } catch (Exception e) {
                throw new AssertionError(
                    "Failed to parse schema <" + schema + '>',
                    e);
            }
        }
    }

    public void setJsonSchemaForTest(final String schema) {
        setJsonSchemaForTest(parseJsonSchema(schema));
    }

    public void setJsonSchemaForTest(final JsonSchema schema) {
        currentSchema = schema;
    }

    public JsonSchema getJsonSchemaForTest() {
        return currentSchema;
    }

    public PrefixedLogger logger() {
        return logger;
    }

    @Before
    public void beforeTest() {
        logger.info("Cleaning the mess before next test\n\n\n\n\n");
        if (cleanupBetweenTests) {
            System.gc();
            System.runFinalization();
        }
        if (sleepInterval > 0L) {
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
            }
        }
        logger.info(
            ">>>>> Starting test " + testName.description.getClassName()
            + ':' + ':' + testName.getMethodName());
        startTime = System.currentTimeMillis();
        currentSchema = defaultJsonSchema;
        clearContext();
    }

    @After
    public void afterTest() {
        StringBuilder sb = new StringBuilder("<<<<< Test ");
        sb.append(testName.description.getClassName());
        sb.append(':');
        sb.append(':');
        sb.append(testName.getMethodName());
        sb.append(" finished in ");
        sb.append(System.currentTimeMillis() - startTime);
        sb.append(" ms");
        logger.info(new String(sb));
    }

    @BeforeClass
    public static void enableOpenApiGeneration() throws Exception {
        OpenApiDocumentation.enable();
    }

    @AfterClass
    public static void generateOpenApi() throws Exception {
        Path swaggerFile = Path.of(ru.yandex.devtools.test.Paths.getTestOutputsRoot(), "openapi.json");
        //Path swaggerFile = Path.of(System.getenv("PWD"), "openapi.json");

        Map<OpenApiPath, OpenApiPath> documented = OpenApiDocumentation.paths();

        List<OpenApiPath> paths = new ArrayList<>(documented.values());
        paths.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.path(), o2.path()));

        OpenAPI api = new OpenAPI();
        for (OpenApiPath path: paths) {
            Operation operation = new Operation();
            List<Parameter> parameters = new ArrayList<>();
            for (OpenApiParameter item: path.params()) {
                if (item.area() == OpenApiParameter.Area.BODY) {
                    RequestBody requestBody = new RequestBody();
                    Content content = new Content();
                    MediaType mediaType = new MediaType();
                    mediaType.example(String.join("\n\n", item.examples()));
                    mediaType.setSchema(new ObjectSchema());
                    content.addMediaType("application/json", mediaType);
                    requestBody.content(content);
                    operation.setRequestBody(requestBody);
                    continue;
                }
                Parameter parameter = new Parameter();
                parameter.setDescription(item.description());
                parameter.name(item.name());
                parameter.required(item.required());
                parameter.in("query");
                Schema<?> schema;
                switch (item.type()) {
                    case ARRAY:
                        schema = new ArraySchema();
                        if (item.parser() instanceof CollectionParser) {
                            CollectionParser<?, ?,?> parser = ((CollectionParser<?, ?,?>) item.parser());
                            schema.description("Separator for items is \"" + parser.delimiter() + "\"");
                        }
                        break;
                    case STRING:
                        StringSchema stringSchema = new StringSchema();
                        schema = stringSchema;
                        if (item.parser() != null) {
                            if (item.parser() instanceof NonEmptyValidator) {
                                parameter.allowEmptyValue(false);
                                break;
                            }

                            if (item.parser() instanceof StringLengthValidator) {
                                Field field = StringLengthValidator.class.getDeclaredField("expectedLength");
                                field.setAccessible(true);
                                stringSchema.maxLength(field.getInt(item.parser()));
                                break;
                            }

                            if (item.parser() instanceof EnumParser) {
                                EnumParser<?> parser = ((EnumParser<?>) item.parser());
                                Field field = parser.getClass().getDeclaredField("e");
                                field.setAccessible(true);
                                Class<?> clazz = (Class<?>) field.get(parser);

                                stringSchema.setEnum(
                                    Arrays.stream(clazz.getEnumConstants())
                                        .map(String::valueOf)
                                        .map((p) -> p.toLowerCase(Locale.ENGLISH))
                                        .collect(Collectors.toList()));
                                break;
                            }

                            if (item.parser() instanceof LocaleParser) {
                                schema.description("locale").example("ru");
                                break;
                            }
                        }
                        break;
                    case NUMBER:
                        schema = new NumberSchema();
                        break;
                    case BOOLEAN:
                        schema = new BooleanSchema();
                        break;
                    case INTEGER:
                        IntegerSchema integerSchema = new IntegerSchema();
                        schema = integerSchema;
                        if (item.parser() != null) {
                            if (item.parser() instanceof PositiveLongValidator
                                    || item.parser() instanceof PositiveIntegerValidator)
                            {
                                integerSchema.setMinimum(new BigDecimal(1));
                                schema.description("positive value");
                                break;
                            }
                        }
                        break;
                    default:
                        schema = new ObjectSchema();
                        break;
                }
                schema.setDefault(item.defaultValue());
                parameter.setSchema(schema);
                for (int i = 0; i < item.examples().size(); i++) {
                    String exampleStr = item.examples().get(i);
                    Example example = new Example();
                    example.value(exampleStr);
                    parameter.example(exampleStr);
                    //parameter.addExample(String.valueOf(i + 1), example);
                }
                parameters.add(parameter);
            }
            operation.setParameters(parameters);
            ApiResponses responses = new ApiResponses();
            for (Map.Entry<Integer, String> entry: path.responses().entrySet()) {
                ApiResponse apiResponse = new ApiResponse();
                Content content = new Content();
                MediaType mediaType = new MediaType();
                mediaType.example(entry.getValue());
                mediaType.setSchema(new ObjectSchema());
                content.addMediaType("application/json", mediaType);
                apiResponse.setContent(content);
                responses.addApiResponse(entry.getKey().toString(), apiResponse);
                if (entry.getKey() == HttpStatus.SC_OK) {
                    responses.setDefault(apiResponse);
                }
            }

            operation.setResponses(responses);
            PathItem item = new PathItem();
            item.operation(PathItem.HttpMethod.valueOf(path.method()), operation);
            api.path(path.path(), item);
        }

        try (Utf8JsonWriter writer =
                JsonType.HUMAN_READABLE.create(
                    new FileOutputStream(swaggerFile.toFile())))
        {
            writeOpenApiObject(api, writer);
        } catch (Exception e) {
            System.err.println("Failed to write openapi to " + swaggerFile);
        }
    }

    private static void writeOpenApiObject(
        final Object obj,
        final JsonWriterBase writer)
        throws Exception
    {
        if (obj instanceof Collection) {
            writer.startArray();
            for (Object item: (Collection<?>) obj) {
                writeOpenApiObject(item, writer);
            }
            writer.endArray();
            return;
        }

        if (obj instanceof Map<?, ?>) {
            writer.startObject();
            for (Map.Entry<?, ?> entry: ((Map<?, ?>) obj).entrySet()) {
                writer.key(String.valueOf(entry.getKey()));
                writeOpenApiObject(entry.getValue(), writer);
            }
            writer.endObject();
            return;
        }

        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            writer.value(obj);
            return;
        }

        writer.startObject();
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field: fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) {
                    continue;
                }
                String name = field.getName();
                if (name.startsWith("_") && name.length() > 1) {
                    name = name.substring(1);
                }
                writer.key(name);
                writeOpenApiObject(value, writer);
            }

            clazz = clazz.getSuperclass();
        }

        writer.endObject();
    }


    public Path resource(final String name) throws Exception {
        URL url = getClass().getResource(name);
        Path path;
        if (url == null) {
            path =
                Paths.get(
                    ru.yandex.devtools.test.Paths.getSandboxResourcesRoot()
                    + '/' + name);
            if (!Files.exists(path)) {
                path = Paths.get(
                    ru.yandex.devtools.test.Paths.getSourcePath(name));
            }
        } else {
            path = Paths.get(url.toURI());
        }
        return path;
    }

    public InputStream resourceStream(final String name) throws Exception {
        return resourceStream(getClass(), name);
    }

    public static InputStream resourceStream(
        final Class<?> clazz,
        final String name)
        throws Exception
    {
        InputStream stream = clazz.getResourceAsStream(name);
        if (stream == null) {
            Path path =
                Paths.get(
                    ru.yandex.devtools.test.Paths.getSandboxResourcesRoot()
                    + '/' + name);
            if (!Files.exists(path)) {
                path =
                    Paths.get(
                        ru.yandex.devtools.test.Paths.getSourcePath(name));
            }
            stream = Files.newInputStream(path);
        }
        return stream;
    }

    public JsonChecker loadResourceAsJsonChecker(final String name)
        throws Exception
    {
        return loadResourceAsJsonChecker(name, currentSchema);
    }

    public JsonChecker loadResourceAsJsonChecker(
        final String name,
        final JsonSchema schema)
        throws Exception
    {
        return new JsonChecker(loadResourceAsString(name), schema);
    }

    public String loadResourceAsString(final String name) throws Exception {
        return loadResourceAsString(getClass(), name);
    }

    public static String loadResourceAsString(
        final Class<?> clazz,
        final String name)
        throws Exception
    {
        try (Reader reader = new InputStreamReader(
                resourceStream(clazz, name),
                StandardCharsets.UTF_8))
        {
            return IOStreamUtils.consume(reader).toString();
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized <T, E extends Exception> T contextResource(
        final String name,
        final GenericSupplier<T, E> supplier)
        throws E
    {
        T resource = (T) context.get(name);
        if (resource == null) {
            resource = supplier.get();
            context.put(name, resource);
        }
        return resource;
    }

    public synchronized void clearContext() {
        context = new HashMap<>();
    }

    public static void clearLoggerSection(final IniConfig config) {
        config.removeKey("file");
        config.removeKey("rotate");
        config.removeKey("buffer");
        config.removeKey("memory-limit");
        config.removeKey("fsync");
    }

    @SuppressWarnings("StringSplitter")
    public static void main(final String... args) throws Exception {
        String name = System.getProperty("sun.java.command").split(" ")[0];
        Class<?> clazz = Class.forName(name);
        Object instance = clazz.getConstructor().newInstance();
        clazz.getMethod(args[0]).invoke(instance);
    }

    public static class TestInfo extends TestWatcher {
        private final PrefixedLogger logger;
        private Description description;

        public TestInfo(final PrefixedLogger logger) {
            this.logger = logger;
        }

        @Override
        protected void starting(final Description description) {
            this.description = description;
        }

        @Override
        protected void failed(
            final Throwable t,
            final Description description)
        {
            logger.log(Level.INFO, "Test failed", t);
        }

        public String getMethodName() {
            return description.getMethodName();
        }
    }

    public static class StatusThread extends Thread {
        private final PrefixedLogger logger;
        private final Path loadavg;
        private final Path entropy;
        private final CPUMonitor cpuMonitor;
        private volatile boolean stop = false;

        StatusThread() {
            super("StatusThread");
            LoggerFileConfigDefaults defaults =
                new LoggerFileConfigDefaults() {
                    @Override
                    public String logFormat() {
                        return "%{date}%{separator}%{thread}"
                            + "%{separator}%{message}";
                    }
                };
            final Map<String, LoggerFileConfigDefaults> fileDefaults =
                Collections.singletonMap(
                    LoggerConfigDefaults.DEFAULT,
                    defaults);
            try {
                logger =
                    new ImmutableLoggerConfig(
                        new LoggerConfigDefaults() {
                            @Override
                            public Map<String, LoggerFileConfigDefaults>
                                files()
                            {
                                return fileDefaults;
                            }
                        })
                        .buildPrefixed(new HandlersManager());
            } catch (ConfigException e) {
                throw new RuntimeException(e);
            }
            loadavg = Paths.get("/proc/loadavg");
            entropy = Paths.get("/proc/sys/kernel/random/entropy_avail");
            CPUMonitor cpuMonitor;
            try {
                cpuMonitor = new CPUMonitor();
            } catch (ReflectiveOperationException e) {
                cpuMonitor = null;
            }
            this.cpuMonitor = cpuMonitor;
            setDaemon(true);
        }

        public void printStatus() throws IOException {
            if (Files.exists(loadavg) && Files.exists(entropy)) {
                double systemCpuLoad;
                if (cpuMonitor == null) {
                    systemCpuLoad = 0d;
                } else {
                    systemCpuLoad = cpuMonitor.getSystemCpuLoad();
                }
                logger.info(
                    "Load average: " + Files.readAllLines(loadavg)
                    + ", system cpu load: " + systemCpuLoad
                    + " %, process cpu usage: "
                    + RusageMonitor.round(
                        RUSAGE_MONITOR.snapshot().totalCPUUsage(),
                        2)
                    + " %, entropy available: " + Files.readAllLines(entropy));
            }
        }

        public void stopStatus() {
            stop = true;
            interrupt();
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    printStatus();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Status failed", e);
                }
                try{
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}

