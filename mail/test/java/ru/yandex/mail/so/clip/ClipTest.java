package ru.yandex.mail.so.clip;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class ClipTest extends TestBase {
    public ClipTest() {
        super(false, 0L);
        System.setProperty("ADDITIONAL_CONFIG", "empty.conf");
    }

    private void testTabMove(
        final String username,
        final String changeFile,
        final String tabPfFile,
        final String soResponseFile,
        final int blackboxAccessCount,
        final int filterSearchAccessCount)
        throws Exception
    {
        try (ClipCluster cluster = new ClipCluster(this)) {
            String blackboxUri =
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=subscription.suid.2&sid=smtp&uid=5598601";
            cluster.blackbox().add(
                blackboxUri,
                loadResourceAsString(username + "-userinfo.json"));
            String blackboxFullUri =
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=subscription.suid.2,userinfo.reg_date.uid,"
                + "userinfo.country.uid&emails=getdefault&aliases=8,13"
                + "&attributes=13,36,1017,1031&sid=smtp&uid=5598601";
            cluster.blackbox().add(
                blackboxFullUri,
                loadResourceAsString(username + "-full-userinfo.json"));
            String filterSearchUri =
                "/filter_search?full_folders_and_labels=1"
                + "&uid=5598601&mids=178455135234592577";
            cluster.filterSearch().add(
                filterSearchUri,
                loadResourceAsString("move1-fs.json"));
            cluster.lenulca().add(
                "/get/320.mail:5598601.E6275498:205149643334330448318509673180"
                + "?raw",
                loadResourceAsString("move1.eml"));
            cluster.start();

            HttpPost post =
                new HttpPost(
                    cluster.clip().host()
                    + "/notify?mdb=pg&pgshard=3039&operation-id=380447677"
                    + "&operation-date=1644911748.361637&uid=5598601"
                    + "&change-type=move&changed-size=1&salo-worker=pg3039:12"
                    + "&transfer-timestamp=1644911748390");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString(changeFile),
                    ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response =
                    cluster.client().execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    cluster.client().execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/sequential/search?service=change_log"
                            + "&prefix=5598601&text=url:tabpf_*"
                            + "&get=url,tabpf_last_tab")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString(tabPfFile)),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Do not check data for users without mail subscription
            if (filterSearchAccessCount > 0) {
                // Let's check that tab pf was saved and visible by SO2
                cluster.so2Cluster().check(
                    "/antispam?session_id=aIZo3O77Ey-Os7i02gq"
                    + "&format=protobuf-json",
                    loadResourceAsString("move1-so-request.json"),
                    loadResourceAsString(soResponseFile));
                Assert.assertEquals(
                    1,
                    cluster.blackbox().accessCount(blackboxFullUri));
            }

            Assert.assertEquals(
                blackboxAccessCount,
                cluster.blackbox().accessCount(blackboxUri));
            Assert.assertEquals(
                filterSearchAccessCount,
                cluster.filterSearch().accessCount(filterSearchUri));
        }
    }

    @Test
    public void testTabMove() throws Exception {
        testTabMove(
            "analizer",
            "move1.json",
            "move1-tabpf.json",
            "move1-so-response.json",
            1,
            1);
    }

    @Test
    public void testTabMoveFromDeleted() throws Exception {
        testTabMove(
            "analizer",
            "move1-from-deleted.json",
            "move1-tabpf.json",
            "move1-so-response.json",
            1,
            1);
    }

    @Test
    public void testTabMoveToUserFolder() throws Exception {
        testTabMove(
            "analizer",
            "move1-to-user-folder.json",
            "empty-search-result.json",
            "move1-so-no-tabpf-response.json",
            0,
            0);
    }

    @Test
    public void testTabMoveManyToUserFolder() throws Exception {
        testTabMove(
            "analizer",
            "move1-move-many-to-user-folder.json",
            "empty-search-result.json",
            "move1-so-no-tabpf-response.json",
            0,
            0);
    }

    @Test
    public void testTabMoveBetweenUserFolders() throws Exception {
        testTabMove(
            "analizer",
            "move1-move-many-between-user-folders.json",
            "empty-search-result.json",
            "move1-so-no-tabpf-response.json",
            0,
            0);
    }

    @Test
    public void testTabMoveUserNotFound() throws Exception {
        testTabMove(
            "user-not-found",
            "move1.json",
            "empty-search-result.json",
            "move1-so-no-tabpf-response.json",
            1,
            0);
    }

    @Test
    public void testTabMoveUserWithoutSuid() throws Exception {
        testTabMove(
            "user-without-suid",
            "move1.json",
            "empty-search-result.json",
            "move1-so-no-tabpf-response.json",
            1,
            0);
    }

    @Test
    public void testBatching() throws Exception {
        try (ClipCluster cluster = new ClipCluster(this)) {
            String blackboxUri =
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=subscription.suid.2&sid=smtp&uid=5598601";
            cluster.blackbox().add(
                blackboxUri,
                loadResourceAsString("analizer-userinfo.json"));
            cluster.filterSearch().add(
                "/filter_search?full_folders_and_labels=1&uid=5598601"
                + "&mids=178455135234592547&mids=178455135234592588",
                loadResourceAsString("move2-fs1.json"));
            cluster.filterSearch().add(
                "/filter_search?full_folders_and_labels=1&uid=5598601"
                + "&mids=178455135234592607",
                loadResourceAsString("move2-fs2.json"));
            cluster.lenulca().add(
                "/get/320.mail:5598601.E5986287:43182668173819529821893913619"
                + "?raw",
                loadResourceAsString("move2-1.eml"));
            cluster.lenulca().add(
                "/get/320.mail:5598601.E6576543:2158765762198456402665423991701"
                + "?raw",
                loadResourceAsString("move2-2.eml"));
            cluster.lenulca().add(
                "/get/320.mail:5598601.E6721466:2252193840158940213224307145729"
                + "?raw",
                loadResourceAsString("move2-3.eml"));
            cluster.start();

            HttpPost post =
                new HttpPost(
                    cluster.clip().host()
                    + "/notify?mdb=pg&pgshard=3039&operation-id=380447677"
                    + "&operation-date=1644911748.361637&uid=5598601"
                    + "&change-type=move&changed-size=1&salo-worker=pg3039:12"
                    + "&transfer-timestamp=1644911748390");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("move2.json"),
                    ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response =
                    cluster.client().execute(post))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    cluster.client().execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/sequential/search?service=change_log"
                            + "&prefix=5598601&text=url:tabpf_*"
                            + "&get=url,tabpf_last_tab&sort=url")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(loadResourceAsString("move2-tabpf.json")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // Request will be splitted into 2 batches processed separately
            Assert.assertEquals(
                2,
                cluster.blackbox().accessCount(blackboxUri));
        }
    }

    @Test
    public void testTikaiteErrors() throws Exception {
        try (ClipCluster cluster = new ClipCluster(this)) {
            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=subscription.suid.2&sid=smtp&uid=5598601",
                loadResourceAsString("analizer-userinfo.json"));
            cluster.filterSearch().add(
                "/filter_search?full_folders_and_labels=1"
                + "&uid=5598601&mids=178455135234592577",
                loadResourceAsString("move1-fs.json"));
            cluster.lenulca().add(
                "/get/320.mail:5598601.E6275498:205149643334330448318509673180"
                + "?raw",
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
            cluster.start();

            HttpPost post =
                new HttpPost(
                    cluster.clip().host()
                    + "/notify?mdb=pg&pgshard=3039&operation-id=380447677"
                    + "&operation-date=1644911748.361637&uid=5598601"
                    + "&change-type=move&changed-size=1&salo-worker=pg3039:12"
                    + "&transfer-timestamp=1644911748390");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("move1.json"),
                    ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response =
                    cluster.client().execute(post))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    response);
            }

            try (CloseableHttpResponse response =
                    cluster.client().execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/sequential/search?service=change_log"
                            + "&prefix=5598601&text=url:tabpf_*"
                            + "&get=url,tabpf_last_tab&sort=url")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        loadResourceAsString("empty-search-result.json")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testAlerts() throws Exception {
        try (ClipCluster cluster = new ClipCluster(this)) {
            cluster.start();

            try (CloseableHttpResponse response =
                    cluster.client().execute(
                        new HttpGet(
                            cluster.clip().host()
                            + "/generate-alerts-config")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new StringChecker(
                        loadResourceAsString("alerts-config.ini")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

