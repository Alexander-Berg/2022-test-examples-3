package ru.yandex.search.mop.server.searchmap;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.search.mop.common.services.ServiceScope;
import ru.yandex.search.mop.server.MopServerCluster;
import ru.yandex.test.util.TestBase;

public class ServerSearchMapTest extends TestBase {
    @Test
    public void testGenerateLabelByHost() throws Exception {
        try (MopServerCluster cluster = new MopServerCluster()) {
            cluster.apply("searchmap.sql");
            cluster.start();
            ServerSearchMap map = cluster.mopServer().searchMapContainer().searchMap();
            checkHostsLabel(map, 0, ServiceScope.MAIL_BP, "host1", "host2", "host3");
            checkHostsLabel(map, 1, ServiceScope.MAIL_BP, "host4", "host5");
            checkHostsLabel(map, 0, ServiceScope.MAIL_CORP, "host6", "host7");
        }
    }

    public void checkHostsLabel(
        final ServerSearchMap map,
        final Integer label,
        final ServiceScope scope,
        final String... hosts)
    {
        for (String host: hosts) {
            Assert.assertEquals(label, map.labelByHost(scope, host));
        }
    }

//    @Test
//    public void uploadToDB()
//        throws IOException, JsonException, ClassNotFoundException, SQLException
//    {
//        SearchMap searchMap;
//
//        // add SIZE(LARGE) TAG(ya:fat) to ya.make
//        // put searchmap file to mop_server/test/resources/ru/yandex/search/mop/server/searchmap/
//
//        // for json searchmap
//        try (BufferedReader reader = new BufferedReader(
//            new InputStreamReader(
//                this.getClass().getResourceAsStream("searchmap.json"),
//                StandardCharsets.UTF_8)))
//        {
//            searchMap = SearchMapJsonParser.INSTANCE.parse(reader);
//        }
//
//        // for text searchmap
//        try (BufferedReader reader = new BufferedReader(
//            new InputStreamReader(
//                this.getClass().getResourceAsStream("searchmap.txt"),
//                StandardCharsets.UTF_8)))
//        {
//            searchMap = SearchMapStringParser.INSTANCE.parse(reader);
//        }
//
//        // upload to DB
//
//        String dbDriver = "org.postgresql.Driver";
//        String dbUrl    = "";
//        String dbUser   = "";
//        String dbPass   = "";
//
//        Class.forName(dbDriver);
//        try (Connection connection =
//                 DriverManager.getConnection(dbUrl, dbUser, dbPass))
//        {
//            SearchMapDBWriter.INSTANCE.write(connection, searchMap);
//        }
//    }
}
