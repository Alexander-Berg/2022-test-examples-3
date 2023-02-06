package ru.yandex.market.ir.tms.barcode.helper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IterableF;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.function.Function;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.files.YtFiles;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.settings.YqlProperties;

public final class YtHelper {
    private static final String YT_TOKEN = System.getenv("YT_TOKEN");
    private static final String YQL_TOKEN = System.getenv("YQL_TOKEN");
    private static final Yt YT = YtUtils.http("hahn.yt.yandex.net", YT_TOKEN);
    private static final String USERNAME = System.getProperty("user.name");
    private static final JdbcTemplate JDBC_TEMPLATE;
    private static final long FOUR_HOURS_IN_MILLIS = TimeUnit.HOURS.toMillis(4);

    static {
        YqlProperties properties = new YqlProperties();
        properties.setUser(USERNAME);
        properties.setPassword(YQL_TOKEN);
        JDBC_TEMPLATE = new JdbcTemplate(
            new YqlDataSource("jdbc:yql://yql.yandex.net:443/hahn?syntaxVersion=1", properties)
        );
    }

    private YtHelper() {
    }

    public static JdbcTemplate getJdbcTemplate() {
        return JDBC_TEMPLATE;
    }

    public static MapF<String, YTreeNode> readScAttributes() {
        return readAttributes("//home/market/production/ir/sc/stratocaster/recent/mbo_offers_mr");
    }

    public static MapF<String, YTreeNode> readParsedSitesAttributes() {
        return readAttributes("//home/market/production/ir/barcode-storage/parsed-sites/empty/the_table");
    }


    public static MapF<String, YTreeNode> readStorageAttributes() {
        return readAttributes("//home/market/production/ir/barcode-storage/stratocaster/recent/full");
    }

    public static MapF<String, YTreeNode> readBlackListShopsAttributes() {
        return readAttributes("//home/market/production/ir/barcode-loader/stratocaster/recent/black-list-shops");
    }

    public static MapF<String, YTreeNode> readWhiteListCategoriesAttributes() {
        return readAttributes("//home/market/production/ir/barcode-loader/stratocaster/recent/white-list-categories");
    }

    public static MapF<String, YTreeNode> readWhiteListShopsAttributes() {
        return readAttributes("//home/market/production/ir/barcode-loader/stratocaster/recent/white-list-shops");

    }

    public static MapF<String, YTreeNode> readMboCategoriesAttributes() {
        return readAttributes("//home/market/production/mbo/export/recent/categories");
    }

    public static MapF<String, YTreeNode> readMboAllModelsAttributes() {
        return readAttributes("//home/market/production/mbo/export/recent/models/all_models");
    }

    public static MapF<String, YTreeNode> readMstatMbocOffersAttributes() {
        return readAttributes("//home/market/production/mstat/dictionaries/mbo/mboc_offers/latest");
    }

    public static MapF<String, YTreeNode> readMstatSuppliersAttributes() {
        return readAttributes("//home/market/production/mstat/dictionaries/suppliers/latest");
    }

    private static MapF<String, YTreeNode> readAttributes(String tablePath) {
        YPath path = YPath.simple(tablePath).allAttributes();
        YTreeMapNode mapNode = YT.cypress().get(path).mapNode();
        MapF<String, YTreeNode> res = mapNode.asMap();
        new HashSet<>(res.keySet()).stream()
            .filter(x -> !(x.equals("schema") || x.startsWith("_yql_proto_field")))
            .forEach(res::removeTs);
        return res;
    }

    public static <T> ListF<T> read(YPath path, Class<T> clazz) {
        return YT.tables().read(
            path,
            YTableEntryTypes.bender(clazz),
            (Function<IteratorF<T>, ListF<T>>) IteratorF::toList
        );
    }

    public static void write(YPath path, IterableF<YTreeMapNode> nodes) {
        YT.tables().write(path, YTableEntryTypes.YSON, nodes);
    }

    public static YPath prepareTestDir(String className, String testName) {
        String expirationDate = getExpirationDate();

        if (USERNAME == null || USERNAME.isEmpty()) {
            throw new IllegalStateException("empty username " + USERNAME);
        }
        YPath usernameDir = YPath.simple("//home/market/users/" + USERNAME + "/market-ir-yql-tests");
        YT.cypress().create(usernameDir, CypressNodeType.MAP, true, true);
        YT.cypress().set(usernameDir.attribute("expiration_time"), expirationDate);

        YPath ctmDir = usernameDir
            .child(String.valueOf(System.currentTimeMillis()));
        YT.cypress().create(ctmDir, CypressNodeType.MAP, true);
        YT.cypress().set(ctmDir.attribute("expiration_time"), expirationDate);

        YPath currentTestDir = ctmDir.child(className).child(testName);
        YT.cypress().create(currentTestDir, CypressNodeType.MAP, true);

        return currentTestDir;
    }

    private static Cypress mockCypress() {
        Cypress cypress = Mockito.mock(Cypress.class);
        Mockito.doReturn(Cf.list())
            .when(cypress)
            .list(Mockito.any());
        return cypress;
    }

    public static Yt mockYt() {
        Yt mockedYt = Mockito.mock(Yt.class);
        Mockito.doReturn(mockCypress())
            .when(mockedYt)
            .cypress();
        YtFiles mockedFiles = Mockito.mock(YtFiles.class);
        Mockito.doReturn(mockedFiles)
            .when(mockedYt)
            .files();
        return mockedYt;
    }

    @NotNull
    private static String getExpirationDate() {
        Date date = new Date(System.currentTimeMillis() + FOUR_HOURS_IN_MILLIS);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ssXXX").format(date);
    }

    public static <T> void createTableAndWrite(
        YPath path,
        MapF<String, YTreeNode> attributes,
        Class<T> clazz,
        ListF<T> rows
    ) {
        createEmptyTable(path, attributes);
        YT.tables().write(path, YTableEntryTypes.bender(clazz), rows);
    }


    public static void createEmptyTable(YPath path, MapF<String, YTreeNode> attributes) {
        YT.cypress().create(path, CypressNodeType.TABLE, true, false, attributes);
    }
}
