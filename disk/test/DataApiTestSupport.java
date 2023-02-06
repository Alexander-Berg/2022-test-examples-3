package ru.yandex.chemodan.app.dataapi.test;

import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.data.field.DataField;
import ru.yandex.chemodan.app.dataapi.api.deltas.Delta;
import ru.yandex.chemodan.app.dataapi.api.deltas.RecordChange;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.generic.DeletionSettings;
import ru.yandex.chemodan.app.dataapi.core.generic.TypeLocation;
import ru.yandex.chemodan.app.dataapi.core.generic.TypeSettings;
import ru.yandex.chemodan.app.dataapi.core.generic.filter.Order;
import ru.yandex.chemodan.boot.ChemodanPropertiesLoadStrategy;
import ru.yandex.chemodan.test.TestHelper;
import ru.yandex.commune.bazinga.test.BazingaTaskManagerStub;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;
import ru.yandex.misc.property.load.PropertiesLoader;
import ru.yandex.misc.property.load.strategy.PropertiesBuilder;
import ru.yandex.misc.version.SimpleAppName;

import static ru.yandex.chemodan.app.dataapi.core.dao.test.ActivateDataApiEmbeddedPg.DATAAPI_EMBEDDED_PG;
import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

/**
 * @author tolmalev
 */
@ContextConfiguration(classes = FullContextTestsContextConfiguration.class)
@TestExecutionListeners(value = DependencyInjectionTestExecutionListener.class)
@ActiveProfiles({DATAAPI_EMBEDDED_PG, EMBEDDED_PG, "dataapi"})
public class DataApiTestSupport {
    protected static final String TEST_VALUE_NAME = "testValueName";

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected BazingaTaskManagerStub bazingaStub;

    @Autowired
    protected UserInitializer userInitializer;

    protected Instant now = Instant.now();

    static {
        TestHelper.initialize();
        initProperties();
    }

    @Before
    public void baseBefore() {
        bazingaStub.tasksWithParams.clear();
    }

    protected DataApiUserId createRandomCleanUser() {
        return userInitializer.createRandomCleanUser();
    }

    protected DataApiUserId createRandomCleanUserInDefaultShard() {
        return userInitializer.createRandomCleanUserInDefaultShard();
    }

    protected void registerDependency(Object dependency) {
        ConfigurableListableBeanFactory beanFactory =
                (ConfigurableListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        beanFactory.registerSingleton(dependency.getClass().getName(), dependency);
    }

    protected <T> T createBean(Class<T> beanClass) {
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        @SuppressWarnings("unchecked")
        T bean = (T) factory.createBean(beanClass, AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR, true);
        return bean;
    }

    public static void initProperties() {
        PropertiesLoader.initialize(DataApiTestSupport::initProperties);
    }

    private static void initProperties(PropertiesBuilder props, EnvironmentType environment) {
        new ChemodanPropertiesLoadStrategy(new SimpleAppName("dataapi", "dataapi"), true).load(props, environment);

        props.set("app.environment.name", environment.getValue());
        // load special functional tests specific ones (the main goal of separate properties - to have separate db)

        props.setDefault("app", "tests");
        props.setDefault("app.name", "tests");
        props.setDefault("service.name", "tests");
        props.setDefault("haveDumpInYt", "true");

        props.copyCutPrefix("dataapi.");
        props.copyCutPrefix("service_dataapi_all.");
        //props.dump();
    }

    protected void timeStop() {
        DateTimeUtils.setCurrentMillisProvider(now::getMillis);
    }

    protected void timeStop(Instant newNow) {
        now = newNow;
        DateTimeUtils.setCurrentMillisProvider(now::getMillis);
    }

    protected void timeStart() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    protected static TypeSettings getTypeSettings() {
        ClassPathResourceInputStreamSource iss = new ClassPathResourceInputStreamSource(
                DataApiTestSupport.class, "SimpleSchema.json");
        String schema = iss.readText();

        return getTypeSettings("my/type/Name", schema, new TypeLocation(Option.of("generic-app-home"), "colId-test", "dbId-test"));
    }

    protected static TypeSettings getTypeSettings(String typeName, String jsonSchema, TypeLocation location) {
        return new TypeSettings(
                jsonSchema,
                typeName,
                "key",
                true,
                true,
                Cf.list(Order.empty()),
                location,
                Option.of(new DeletionSettings("date", Duration.standardDays(1))),
                false);
    }

    protected MapF<String, DataField> createRequiredGenericObjectData(String keyValue, String requiredPropValue) {
        MapF<String, DataField> data = Cf.hashMap();
        data.put("key", DataField.string(keyValue));
        data.put("requiredProp", DataField.string(requiredPropValue));

        return data;
    }

    protected static Delta makeSimpleDelta(int testValue) {
        return makeSimpleDelta(testValue, "colId");
    }

    protected static Delta makeSimpleDelta(int testValue, String collectionId) {
        RecordChange recordChange = RecordChange.set(
                collectionId, "recId", Cf.map(TEST_VALUE_NAME, DataField.integer(testValue)));
        return new Delta(recordChange);
    }

    protected static Delta makeInsertDelta(int testValue, String collectionId) {
        RecordChange recordChange = RecordChange.insert(
                collectionId, "recId_" + testValue, Cf.map(TEST_VALUE_NAME, DataField.integer(testValue)));
        return new Delta(recordChange);
    }
}
