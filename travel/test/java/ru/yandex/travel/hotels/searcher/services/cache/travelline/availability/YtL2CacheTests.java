package ru.yandex.travel.hotels.searcher.services.cache.travelline.availability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.travel.commons.yt.BaseYtProperties;
import ru.yandex.travel.commons.yt.ConnectionFactory;
import ru.yandex.travel.commons.yt.YtClusterProperties;
import ru.yandex.travel.hotels.searcher.services.cache.travelline.availability.yt.YtAvailabilityRepository;
import ru.yandex.travel.hotels.searcher.services.cache.travelline.availability.yt.YtInventoryRepository;
import ru.yandex.travel.hotels.searcher.services.cache.travelline.availability.yt.YtTransactionSupplier;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;

@Ignore
public class YtL2CacheTests extends BaseL2CacheTests {
    private static final TableSchema invSchema = new TableSchema.Builder()
            .addKey("HotelId", ColumnValueType.STRING)
            .addValue("Version", ColumnValueType.UINT64)
            .addValue("Inventory", ColumnValueType.STRING)
            .addValue("ActualizationTimestamp", ColumnValueType.UINT64)
            .build();
    private static final TableSchema avSchema = new TableSchema.Builder()
            .addKey("HotelId", ColumnValueType.STRING)
            .addKey("CheckIn", ColumnValueType.UINT64)
            .addKey("CheckOut", ColumnValueType.UINT64)
            .addValue("Response", ColumnValueType.STRING)
            .addValue("Versions", ColumnValueType.STRING)
            .addValue("ActualizationTimestamp", ColumnValueType.UINT64)
            .build();
    static YtClient preparationClient;
    static UUID testId;
    static private String basePath;
    private static ConnectionFactory factory;

    @BeforeClass
    public static void prepareAll() throws IOException {
        String homeDir = System.getProperty("user.home");
        String ytToken = "";
        Path tokenFilePath = Path.of(homeDir, ".yt/token");
        if (Files.exists(tokenFilePath)) {
            ytToken = Files.readString(tokenFilePath).stripTrailing();
        }
        BaseYtProperties properties = new BaseYtProperties();
        properties.getClusters().put("default", new YtClusterProperties());
        properties.getClusters().get("default").setUser(System.getProperty("user.name"));
        properties.getClusters().get("default").setToken(ytToken);
        factory = new ConnectionFactory(properties);
        preparationClient = factory.getClientForCluster("hahn");
        if (!preparationClient.existsNode("//home/travel/testing/unit-tests").join()) {
            preparationClient.createNode(new CreateNode("//home/travel/testing/unit-tests", ObjectType.MapNode)).join();
        }
        testId = UUID.randomUUID();
        basePath = "//home/travel/testing/unit-tests/" + testId.toString();
        preparationClient.createNode(new CreateNode(basePath, ObjectType.MapNode)).join();


        preparationClient.createNode(new CreateNode(basePath + "/inventory", ObjectType.Table, Map.of(
                "dynamic", new YTreeBuilder().value(true).build(),
                "schema", invSchema.toYTree()
        ))).join();
        preparationClient.createNode(new CreateNode(basePath + "/offer_availability", ObjectType.Table, Map.of(
                "dynamic", new YTreeBuilder().value(true).build(),
                "schema", avSchema.toYTree()
        ))).join();
        preparationClient.mountTable(basePath + "/inventory").join();
        preparationClient.mountTable(basePath + "/offer_availability").join();
    }

    @AfterClass
    public static void tearDownAll() {
        preparationClient.removeNode(basePath);
    }

    @Override
    public void prepare() {
        cache = new L2CacheImplementation(new YtInventoryRepository(basePath), new YtAvailabilityRepository(basePath)
                , new YtTransactionSupplier(factory, "hahn"));
    }

    @After
    public void tearDown() {
        cache.transactionally(t ->
                cache.listHotelsActualizedBefore(Instant.now().plusSeconds(10), t).thenCompose(res -> cache.removeInventories(res, t))).join();
        cache.transactionally(t ->
                cache.listAvailabilityKeysForOffersBeforeDate(LocalDate.now().plusYears(1), t).thenCompose(res -> cache.removeAvailabilities(res, t))).join();
    }
}
