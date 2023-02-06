package ru.yandex.market.crm.platform.reader.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.Iterables;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.BeanFactory;

import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.exceptions.InvalidConfigException;
import ru.yandex.market.crm.platform.commons.CustomOptions;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.config.InboxSourceConfig;
import ru.yandex.market.crm.platform.config.LBInstallationMode;
import ru.yandex.market.crm.platform.config.LogBrokerSource;
import ru.yandex.market.crm.platform.config.Model;
import ru.yandex.market.crm.platform.config.SourceConfig;
import ru.yandex.market.crm.platform.config.YtSourceConfig;
import ru.yandex.market.crm.platform.services.config.ConfigurationInitializerImpl;
import ru.yandex.market.crm.platform.config.impl.Configurations;
import ru.yandex.market.crm.platform.config.raw.RawFactConfig;
import ru.yandex.market.crm.platform.mappers.ModelMapper;
import ru.yandex.market.crm.platform.models.TestCartEvent;
import ru.yandex.market.crm.platform.reader.test.AbstractServiceTest;
import ru.yandex.market.crm.platform.yt.YtClusters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationInitializerImplTest extends AbstractServiceTest {

    /**
     * Используется в {@link ConfigurationInitializerImplTest#testReadWithMapper()}
     */
    public static class MapperStub implements ModelMapper<TestCartEvent> {

        @Override
        public List<TestCartEvent> apply(byte[] content) {
            UserIds.Builder userIds = UserIds.newBuilder()
                .setYandexuid("4545");
            TestCartEvent event = TestCartEvent.newBuilder()
                .setUserIds(userIds)
                .setModelId(96)
                .build();
            return Collections.singletonList(event);
        }
    }

    private static void assertModel(Model model) throws InvalidProtocolBufferException {
        assertNotNull(model);

        Parser<? extends Message> parser = model.getParser();
        assertNotNull(parser);

        TestCartEvent event = (TestCartEvent) parser.parseFrom(
            TestCartEvent.newBuilder()
                .setUserIds(
                    UserIds.newBuilder()
                        .setYandexuid("111")
                )
                .setModelId(222)
                .build()
                .toByteArray()
        );

        assertEquals("111", event.getUserIds().getYandexuid());
        assertEquals(222, event.getModelId());

        Descriptor descriptor = model.getDescriptor();
        assertNotNull(descriptor);

        assertEquals(3, descriptor.getFields().size());
        assertEquals("userIds", descriptor.getFields().get(0).getName());

        assertEquals("timestamp", descriptor.getFields().get(1).getName());
        assertTrue(descriptor.getFields().get(1).getOptions().getExtension(CustomOptions.time));

        assertEquals("modelId", descriptor.getFields().get(2).getName());
        assertTrue(descriptor.getFields().get(2).getOptions().getExtension(CustomOptions.id));
    }

    @Inject
    private BeanFactory beanFactory;

    @Inject
    private EnvironmentResolver environmentResolver;

    @Inject
    private YtClusters ytClusters;

    private ConfigurationInitializerImpl initializer;

    @Before
    public void setUp() {
        initializer = new ConfigurationInitializerImpl(beanFactory, environmentResolver, ytClusters);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testReadMinimumConfig() throws IOException {
        FactConfig config = parse("minimal-config.yaml");

        assertNotNull(config);
        assertEquals("TestCartEvent", config.getId());
        assertModel(config.getModel());

        SourceConfig source = Iterables.get(config.getSources(), 0);
        assertNotNull(source);
        assertEquals(InboxSourceConfig.class, source.getClass());
    }

    @Test
    public void testReadSimpleConfig() throws IOException {
        FactConfig config = parse("simple-config.yaml");

        assertNotNull(config);

        SourceConfig source = Iterables.get(config.getSources(), 0);
        assertNotNull(source);
        assertEquals(LogBrokerSource.class, source.getClass());

        LogBrokerSource topic = (LogBrokerSource) source;
        assertEquals("market-carter", topic.getIdent());
        assertEquals("carter-cart-log", topic.getLogType());
        assertEquals(LBInstallationMode.LOGBROKER, topic.getInstallation());
        assertNull(topic.getMapper());

        assertModel(config.getModel());
    }

    @Test
    public void testReadWithMapper() throws IOException {
        FactConfig config = parse("config-with-mapper.yaml");

        assertNotNull(config);

        LogBrokerSource source = (LogBrokerSource) Iterables.get(config.getSources(), 0);
        assertNotNull(source.getMapper());

        TestCartEvent event = (TestCartEvent) source.getMapper()
            .apply(new byte[0]).get(0);

        assertNotNull(event);
        assertEquals("4545", event.getUserIds().getYandexuid());
        assertEquals(96, event.getModelId());
    }

    @Test(expected = InvalidConfigException.class)
    public void testErrorOnConfigWithoutId() throws IOException {
        parse("config-without-id.yaml");
    }

    @Test(expected = InvalidConfigException.class)
    public void testErrorOnModelClassNotFound() throws IOException {
        parse("config-with-no-protobuf-model-desc.yaml");
    }

    @Test(expected = InvalidConfigException.class)
    public void testErrorOnMapperClassNotFound() throws IOException {
        parse("config-with-invalid-mapper.yaml");
    }

    @Test
    public void testErrorOnModelWithoutUserId() throws IOException {
        expectValidationException("Invalid model 'WithoutUserId'. Required uid or group or fact id.");

        parse("config-with-invalid-model-1.yaml");
    }

    @Test
    public void testErrorOnModelWithUserIdWithWrongType() throws IOException {
        expectValidationException("Invalid model 'WithUserIdWithWrongType'. Field with option" +
            " 'crm.platform.commons.uid' has wrong type. Found: uint32.");
        parse("config-with-invalid-model-2.yaml");
    }

    @Test
    public void testErrorOnModelWithMultipleEventTimeOptions() throws IOException {
        expectValidationException("Invalid model 'WithMultipleEventTimeFields'. " +
            "Model should have only one field annotated as 'crm.platform.commons.time'");
        parse("config-with-invalid-model-3.yaml");
    }

    @Test
    public void testReadConfigWithComplicatedTTL() throws IOException {
        FactConfig config = parse("config-with-full-ttl.yaml");
        var clusterStorages = config.getClusterStorages();

        var expected = Duration.ZERO
                .plus(365, ChronoUnit.DAYS)
                .plus(4 * 30, ChronoUnit.DAYS)
                .plus(2, ChronoUnit.DAYS)
                .plus(3, ChronoUnit.HOURS)
                .plus(15, ChronoUnit.MINUTES)
                .plus(16, ChronoUnit.SECONDS);

        var senecaSas = clusterStorages.get("seneca-sas");
        assertNotNull(senecaSas);
        assertEquals(expected, senecaSas.getTtl());

        var senecaMan = clusterStorages.get("seneca-man");
        assertNotNull(senecaMan);
        assertEquals(expected, senecaMan.getTtl());

        var senecaVla = clusterStorages.get("seneca-vla");
        assertNotNull(senecaVla);
        assertEquals(expected, senecaVla.getTtl());
    }

    @Test
    public void testReadConfigWithSimpleTTL() throws IOException {
        FactConfig config = parse("config-with-simple-ttl.yaml");

        var storage = config.getClusterStorages().get("seneca-sas");
        assertNotNull(storage);
        assertEquals(Duration.ofDays(7), storage.getTtl());
    }

    @Test
    public void testConfigWithYtSource() throws IOException {
        FactConfig config = parse("config-yt-source-correct.yaml");
        SourceConfig source = config.getSources().iterator().next();

        assertTrue(source instanceof YtSourceConfig);
        YtSourceConfig ytSource = (YtSourceConfig) source;

        assertEquals("//home/path/example/table", ytSource.getPath());
        assertEquals("YtFactMapper", ytSource.getMapper());
        assertEquals(YtSourceConfig.NewTableStrategy.FIRST, ytSource.getStrategy());
    }

    @Test
    public void testReadConfigWithInstallation() throws IOException {
        FactConfig config = parse("config-with-installation.yaml");
        SourceConfig source = config.getSources().iterator().next();

        assertNotNull(source);
        assertEquals(LogBrokerSource.class, source.getClass());

        LogBrokerSource topic = (LogBrokerSource) source;
        assertEquals(LBInstallationMode.LBKX, topic.getInstallation());
    }

    /**
     * В случае если ни один storage не сконфигурирован, используются дефолтные настройки хранения,
     * распространенные на все доступные кластера.
     */
    @Test
    public void testUseDefaultSettingsIfStoragesIsNotConfigured() throws IOException {
        var config = parse("config-with-installation.yaml");
        var clusters = config.getClusterStorages().keySet();
        assertEquals(Set.of("hahn", "arnold", "seneca-sas", "seneca-vla", "seneca-man"), clusters);
    }

    @Test
    public void testUseAllClustersIfClusterFieldNotSpecified() throws IOException {
        var config = parse("config-with-storage-without-cluster.yaml");
        var storages = config.getClusterStorages();
        assertEquals(Set.of("hahn", "arnold", "seneca-sas", "seneca-vla", "seneca-man"), storages.keySet());

        var storage = storages.get("hahn");
        assertEquals(Duration.ofDays(7), storage.getTtl());
    }

    @Test
    public void testIfEmptyStoragesSpecifiedThereWillBeNoStorage() throws IOException {
        var config = parse("config-with-empty-storages.yaml");
        assertTrue(config.getClusterStorages().isEmpty());
    }

    private void expectValidationException(String message) {
        exception.expect(InvalidConfigException.class);
        exception.expectMessage(message);
    }

    private static RawFactConfig readRawConfig(String name) throws IOException {
        InputStream is = ConfigurationInitializerImplTest.class.getResourceAsStream(name);
        return Configurations.MAPPER.readValue(is, RawFactConfig.class);
    }

    private FactConfig parse(String name) throws IOException {
        return initializer.parse(name, readRawConfig(name));
    }
}
