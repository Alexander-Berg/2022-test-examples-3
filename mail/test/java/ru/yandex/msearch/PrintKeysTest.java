package ru.yandex.msearch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.search.prefix.Prefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.util.filesystem.DeletingFileVisitor;
import ru.yandex.util.string.StringUtils;

public class PrintKeysTest extends TestBase {
    @Test
    public void testPrintKeys() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"body_text\":\"мир\"",
                "\"url\":\"2\",\"body_text\":\"миру мир\"",
                "\"url\":\"3\",\"body_text\":\"мир дверь мяч\"");
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(
                    lucene.indexerUri() + "/delete?text=url:3&prefix=0"));
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&print-freqs&hr",
                "{\"0#мир\":{\"freq\":1},\"0#мир\\\"\":{\"freq\":1},"
                + "\"0#мира\":{\"freq\":1},\"0#миру\\\"\":{\"freq\":1}}");
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&print-freqs"
                + "&max-freq=0&hr",
                "{\"0#мир\":{\"freq\":2},\"0#мир\\\"\":{\"freq\":2},"
                + "\"0#мира\":{\"freq\":1},\"0#миру\\\"\":{\"freq\":1}}");
            // check that skip-deleted is compatible with print-docs
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&print-docs"
                + "&max-freq=0",
                new StringChecker(
                    "0#мир ( 2 ) : 0 1\n"
                    + "0#мир\" ( 2 ) : 0 1\n"
                    + "0#мира ( 1 ) : 1\n"
                    + "0#миру\" ( 1 ) : 1\n"));
            // Due to ultimate awesomeness of various Terms implementations,
            // there is no reverseIterator() support for FastCommitCodec terms
            // and PerFieldMergingIndexReader$MergingTerms, so we have to use
            // some fashionable crutches to read reversed keys from segments
            // flushed to disk
            lucene.flush();
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&reverse",
                new StringChecker("0#миру\"\n0#мира\n0#мир\"\n0#мир\n"));
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&reverse&prefix=0%23",
                new StringChecker("0#миру\"\n0#мира\n0#мир\"\n0#мир\n"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 128; ++i) {
                sb.append('я');
            }
            String longToken = new String(sb);
            // Check word lemmer not familiar with
            lucene.add(
                "\"url\":\"4\",\"body_text\":\"100500 яяавафывамси "
                + longToken + '"');
            // Tokens starting with digit won't be lemmatized
            // Wery long tokens won't be lemmatized and will be truncated by
            // filter to 66 characters
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted",
                new StringChecker(
                    "0#100500\n"
                    + "0#мир\n"
                    + "0#мир\"\n"
                    + "0#мира\n"
                    + "0#миру\"\n"
                    + "0#яяавафывамси\n"
                    + "0#яяавафывамси\"\n"
                    + "0#" + longToken.substring(0, 64) + '\n'));
        }
    }

    @Test
    public void testPrintKeysPrefixShard() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            final Prefix prefix0 = new LongPrefix(0);
            final Prefix prefix1 = new LongPrefix(1);
            lucene.add(
                prefix0,
                "\"url\":\"1\",\"body_text\":\"мир\"",
                "\"url\":\"2\",\"body_text\":\"миру мир\"");
            lucene.add(
                prefix1,
                "\"url\":\"3\",\"body_text\":\"мир дверь мяч\"");
            //user=0
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&print-freqs&hr"
                    + "&user=0",
                "{\"0#мир\":{\"freq\":1},\"0#мир\\\"\":{\"freq\":1},"
                + "\"0#мира\":{\"freq\":1},\"0#миру\\\"\":{\"freq\":1}}");
            //user=1
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&print-freqs&hr"
                    + "&user=1",
                "{\"1#дверь\":{\"freq\":1},\"1#дверь\\\"\":{\"freq\":1},"
                + "\"1#мир\":{\"freq\":1},\"1#мир\\\"\":{\"freq\":1},"
                + "\"1#мяч\":{\"freq\":1},\"1#мяч\\\"\":{\"freq\":1}}");
            //shard=1
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&print-freqs&hr"
                    + "&shard=0",
                "{\"0#мир\":{\"freq\":1},\"0#мир\\\"\":{\"freq\":1},"
                + "\"0#мира\":{\"freq\":1},\"0#миру\\\"\":{\"freq\":1}}");
            //shard=1
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&print-freqs&hr"
                    + "&shard=1",
                "{\"1#дверь\":{\"freq\":1},\"1#дверь\\\"\":{\"freq\":1},"
                + "\"1#мир\":{\"freq\":1},\"1#мир\\\"\":{\"freq\":1},"
                + "\"1#мяч\":{\"freq\":1},\"1#мяч\\\"\":{\"freq\":1}}");
            //all users
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&print-freqs&hr",
                "{\"0#мир\":{\"freq\":1},\"0#мир\\\"\":{\"freq\":1},"
                + "\"0#мира\":{\"freq\":1},\"0#миру\\\"\":{\"freq\":1},"
                + "\"1#дверь\":{\"freq\":1},\"1#дверь\\\"\":{\"freq\":1},"
                + "\"1#мир\":{\"freq\":1},\"1#мир\\\"\":{\"freq\":1},"
                + "\"1#мяч\":{\"freq\":1},\"1#мяч\\\"\":{\"freq\":1}}");

            // Due to ultimate awesomeness of various Terms implementations,
            // there is no reverseIterator() support for FastCommitCodec terms
            // and PerFieldMergingIndexReader$MergingTerms, so we have to use
            // some fashionable crutches to read reversed keys from segments
            // flushed to disk
            lucene.flush();
            //user=0
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&reverse&user=0",
                new StringChecker("0#миру\"\n0#мира\n0#мир\"\n0#мир\n"));
            //user=1
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&reverse&user=1",
                new StringChecker(
                    "1#мяч\"\n1#мяч\n1#мир\"\n1#мир\n1#дверь\"\n1#дверь\n"));
            //shard=0
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&reverse&shard=0",
                new StringChecker("0#миру\"\n0#мира\n0#мир\"\n0#мир\n"));
            //shard=1
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&reverse&shard=1",
                new StringChecker(
                    "1#мяч\"\n1#мяч\n1#мир\"\n1#мир\n1#дверь\"\n1#дверь\n"));
            //all
            lucene.checkSearch(
                "/printkeys?field=body_text&skip-deleted&reverse",
                new StringChecker(
                    "1#мяч\"\n1#мяч\n1#мир\"\n1#мир\n1#дверь\"\n1#дверь\n"
                    + "0#миру\"\n0#мира\n0#мир\"\n0#мир\n"));
        }
    }

    private void testPrintKeysBlocks(
        final int indexDivisor,
        final String fieldCodec)
        throws Exception
    {
        Path path = Files.createTempDirectory(testName.getMethodName());
        IniConfig config =
            new IniConfig(
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail")));
        config.put("index_divisor", Integer.toString(indexDivisor));
        config.put("default-field-codec", fieldCodec);
        try (TestSearchBackend lucene = new TestSearchBackend(
                path,
                false,
                TestSearchBackend.patchConfig(path, false, config)))
        {
            int batchSize = 1000;
            int batches = 100;
            // received_date has padding:10, so received dates dictionary will
            // have keys from 0#0000050000 to 0#0000149999
            int firstDocId = batchSize * batches >> 1;
            String[] docs = new String[batchSize];
            String[] receivedDates = new String[batchSize * batches];
            for (int i = 0; i < batches; ++i) {
                for (int j = 0; j < batchSize; ++j) {
                    int docId = i * batchSize + j;
                    String receivedDate = Integer.toString(docId + firstDocId);
                    if (receivedDate.length() == 5) {
                        receivedDates[docId] = "0#00000" + receivedDate;
                    } else {
                        // receivedDate.length() == 6
                        receivedDates[docId] = "0#0000" + receivedDate;
                    }
                    docs[j] =
                        "\"url\":\"" + docId
                        + "\",\"received_date\":\"" + receivedDate + '"';
                }
                lucene.add(docs);
            }
            lucene.flush();

            // Check that whe hadn't missed anything in simplest case
            lucene.checkSearch(
                "/printkeys?field=received_date",
                new StringChecker(
                    StringUtils.join(receivedDates, '\n', "", "\n")));

            // Check that reverse listing works in simplest case
            List<String> dates = Arrays.asList(receivedDates);
            List<String> reversed = new ArrayList<>(dates);
            Collections.reverse(reversed);
            lucene.checkSearch(
                "/printkeys?field=received_date&reverse",
                new StringChecker(StringUtils.join(reversed, '\n', "", "\n")));

            // Check that prefix requests works as expected
            // Let's select keys from 0#0000120000 to 0#0000129999
            List<String> middle = dates.subList(70000, 80000);
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000012",
                new StringChecker(StringUtils.join(middle, '\n', "", "\n")));
            // Check TermsCache correctness
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000012",
                new StringChecker(StringUtils.join(middle, '\n', "", "\n")));

            // Same for reversed order
            List<String> middleReversed = new ArrayList<>(middle);
            Collections.reverse(middleReversed);
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000012&reverse",
                new StringChecker(
                    StringUtils.join(middleReversed, '\n', "", "\n")));

            // Check corner cases

            // Check for prefix being prefix of first key
            List<String> start = dates.subList(0, 10000);
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000005",
                new StringChecker(StringUtils.join(start, '\n', "", "\n")));
            // Check TermsCache correctness
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000005",
                new StringChecker(StringUtils.join(start, '\n', "", "\n")));

            // Same for reversed
            List<String> startReversed = new ArrayList<>(start);
            Collections.reverse(startReversed);
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000005&reverse",
                new StringChecker(
                    StringUtils.join(startReversed, '\n', "", "\n")));

            // Check for prefix being prefix of last key
            List<String> end = dates.subList(
                batchSize * batches - 10000,
                batchSize * batches);
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000014",
                new StringChecker(StringUtils.join(end, '\n', "", "\n")));
            // Check TermsCache correctness
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000014",
                new StringChecker(StringUtils.join(end, '\n', "", "\n")));

            // Same for reversed
            List<String> endReversed = new ArrayList<>(end);
            Collections.reverse(endReversed);
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000014&reverse",
                new StringChecker(
                    StringUtils.join(endReversed, '\n', "", "\n")));

            // Check prefix being less than the first key
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000003",
                new StringChecker(""));
            // Check TermsCache correctness
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000003",
                new StringChecker(""));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%23000003&reverse",
                new StringChecker(""));

            // Check prefix being bigger than the last key
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230003",
                new StringChecker(""));
            // Check TermsCache correctness
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230003",
                new StringChecker(""));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230003&reverse",
                new StringChecker(""));

            // Check exact match with prefix
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000050000",
                new StringChecker("0#0000050000\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000050000&reverse",
                new StringChecker("0#0000050000\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000050001",
                new StringChecker("0#0000050001\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000050001&reverse",
                new StringChecker("0#0000050001\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000149998",
                new StringChecker("0#0000149998\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000149998&reverse",
                new StringChecker("0#0000149998\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000149999",
                new StringChecker("0#0000149999\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000149999&reverse",
                new StringChecker("0#0000149999\n"));
            // Check TermsCache correctness
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000050000",
                new StringChecker("0#0000050000\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000050001",
                new StringChecker("0#0000050001\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000149998",
                new StringChecker("0#0000149998\n"));
            lucene.checkSearch(
                "/printkeys?field=received_date&prefix=0%230000149999",
                new StringChecker("0#0000149999\n"));
        }
    }


    @Test
    public void testPrintKeysBlocks1Yandex() throws Exception {
        testPrintKeysBlocks(1, "Yandex");
    }

    @Test
    public void testPrintKeysBlocks2Yandex() throws Exception {
        testPrintKeysBlocks(2, "Yandex");
    }

    @Test
    public void testPrintKeysBlocks3Yandex() throws Exception {
        testPrintKeysBlocks(3, "Yandex");
    }

    @Test
    public void testPrintKeysBlocks1Yandex2() throws Exception {
        testPrintKeysBlocks(1, "Yandex2");
    }

    @Test
    public void testPrintKeysBlocks2Yandex2() throws Exception {
        testPrintKeysBlocks(2, "Yandex2");
    }

    @Test
    public void testPrintKeysBlocks3Yandex2() throws Exception {
        testPrintKeysBlocks(3, "Yandex2");
    }


    @Test
    public void testPrintKeysBlocks1Yandex2LZ4() throws Exception {
        testPrintKeysBlocks(1, "Yandex2_lz4");
    }

    @Test
    public void testPrintKeysBlocks2Yandex2LZ4() throws Exception {
        testPrintKeysBlocks(2, "Yandex2_lz4");
    }

    @Test
    public void testPrintKeysBlocks3Yandex2LZ4() throws Exception {
        testPrintKeysBlocks(3, "Yandex2_lz4");
    }

    @Test
    public void testPrintKeysBlocks1Yandex2Brotli() throws Exception {
        testPrintKeysBlocks(1, "Yandex2_brotli");
    }

    @Test
    public void testPrintKeysBlocks2Yandex2Brotli() throws Exception {
        testPrintKeysBlocks(2, "Yandex2_brotli");
    }

    @Test
    public void testPrintKeysBlocks3Yandex2Brotli() throws Exception {
        testPrintKeysBlocks(3, "Yandex2_brotli");
    }

    @Test
    public void testPrintKeysOldDaemon() throws Exception {
        File root = Files.createTempDirectory("testPrintKeys").toFile();
        try {
            try (Daemon daemon =
                    new Daemon(
                        SearchBackendTestBase.config(
                            root,
                            "\n[field.inversed]"
                            + "\ntokenizer = keyword"
                            + "\nfilters = maf:-1|aaz:9999999999999|padding:13"
                            + "\nprefixed = true"
                            + "\nanalyze = true"
                            + "\nattribute = true"));
                CloseableHttpClient client = HttpClients.createDefault())
            {
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":\"1\",\"text\":\"привет миры\"},{"
                    + "\"keyword\":\"2\",\"text\":\"привет опять\","
                    + "\"attribute\":\"attr\",\"inversed\":1234567890123}]}",
                    StandardCharsets.UTF_8));
                HttpResponse response = client.execute(post);
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                post.setEntity(new StringEntity("{\"prefix\":2,\"docs\":[{"
                    + "\"keyword\":\"3\",\"text\":\"привет\"}]}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                post.setEntity(new StringEntity("{\"prefix\":1001,\"docs\":[{"
                    + "\"keyword\":\"4\",\"text\":\"мир\"}]}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/printkeys?field=keyword"));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String text = CharsetUtils.toString(response.getEntity());
                Assert.assertEquals("1#1\n1#2\n1001#4\n2#3\n", text);

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/printkeys?field=text&user=1&prefix=1%23&print-pos"));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                text = CharsetUtils.toString(response.getEntity());
                Assert.assertEquals(
                    "1#мир ( 1 ) : 0 { 1 }\n"
                    + "1#мира ( 1 ) : 0 { 1 }\n"
                    + "1#миры\" ( 1 ) : 0 { 1 }\n"
                    + "1#опять ( 1 ) : 1 { 1 }\n"
                    + "1#опять\" ( 1 ) : 1 { 1 }\n"
                    + "1#привет ( 2 ) : 0 { 0 } 1 { 0 }\n"
                    + "1#привет\" ( 2 ) : 0 { 0 } 1 { 0 }\n",
                    text);

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/printkeys?field=text&"
                    + "user=1&prefix=1%23&get=keyword,attribute&print-pos"
                    + "&json-type=normal&length=2&offset=4"));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                text = CharsetUtils.toString(response.getEntity());
                Assert.assertEquals(
                    "{\"1#опять\\\"\":{\"freq\":1,\"docs\":[{\"docId\":1,"
                    + "\"fields\":[\"2\",\"attr\"],\"positions\":[1]}]},"
                    + "\"1#привет\":{\"freq\":2,\"docs\":[{\"docId\":0,"
                    + "\"fields\":[\"1\",null],\"positions\":[0]},{\"docId\":"
                    + "1,\"fields\":[\"2\",\"attr\"],\"positions\":[0]}]}}",
                    text);

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/printkeys?field=text&"
                    + "shard=1&prefix=10&print-docs&length=1&hr"));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                text = CharsetUtils.toString(response.getEntity());
                Assert.assertEquals(
                    "{\n    \"1001#мир\": {\n        \"freq\": 1,\n"
                    + "        \"docs\": [\n            {\n                "
                    + "\"docId\": 2\n            }\n        ]\n    }\n}",
                    text);

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort() + "/printkeys?field=text&"
                    + "prefix=10&get=keyword&length=1"));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                text = CharsetUtils.toString(response.getEntity());
                Assert.assertEquals("1001#мир ( 1 ) : 2 [ 4 ]\n", text);

                //empty field
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?printkeys&field=text1&"
                    + "prefix=zzzz"));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                text = CharsetUtils.toString(response.getEntity());
                Assert.assertEquals("", text);

                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchPort() + "/?printkeys&field=inversed&"
                    + "prefix=1%23"));
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                text = CharsetUtils.toString(response.getEntity());
                Assert.assertEquals("1#8765432109876\n", text);
            }
        } finally {
            Files.walkFileTree(root.toPath(), DeletingFileVisitor.INSTANCE);
        }
    }

    @Test
    public void testQueryFilter() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add(
                "\"url\":\"1\",\"body_text\":\"мир\",\"fid\":\"1\"",
                "\"url\":\"2\",\"body_text\":\"миру мир w0rd\",\"fid\":\"2\"",
                "\"url\":\"3\",\"body_text\":\"мир дверь мяч\"",
                "\"url\":\"4\",\"body_text\":\"двери алабамы\",\"fid\":\"2\"",
                "\"url\":\"5\",\"body_text\":\"мир-мир-мир\",\"fid\":\"2\"");
            // simplest case
            lucene.checkSearch(
                "/printkeys?field=body_text&user=0&skip-deleted"
                + "&text=fid:2&print-docs&max-freq=0",
                new StringChecker(
                    "0#w0rd ( 1 ) : 1\n"
                    + "0#алабама ( 1 ) : 3\n"
                    + "0#алабамы\" ( 1 ) : 3\n"
                    + "0#двери\" ( 1 ) : 3\n"
                    + "0#дверь ( 1 ) : 3\n"
                    + "0#мир ( 2 ) : 1 4\n"
                    + "0#мир\" ( 2 ) : 1 4\n"
                    + "0#мира ( 1 ) : 1\n"
                    + "0#миру\" ( 1 ) : 1\n"));
            // print total freqs
            lucene.checkSearch(
                "/printkeys?field=body_text&user=0&skip-deleted&text=fid:2"
                + "&print-total-freqs&max-freq=0",
                new StringChecker(
                    "0#w0rd ( 1 1 )\n"
                    + "0#алабама ( 1 1 )\n"
                    + "0#алабамы\" ( 1 1 )\n"
                    + "0#двери\" ( 1 1 )\n"
                    + "0#дверь ( 1 1 )\n"
                    + "0#мир ( 2 5 )\n"
                    + "0#мир\" ( 2 4 )\n"
                    + "0#мира ( 1 1 )\n"
                    + "0#миру\" ( 1 1 )\n"));
            // only exact matches
            lucene.checkSearch(
                "/printkeys?field=body_text&user=0&skip-deleted&text=fid:2"
                + "&print-docs&max-freq=0&exact",
                new StringChecker(
                    "0#алабамы\" ( 1 ) : 3\n"
                    + "0#двери\" ( 1 ) : 3\n"
                    + "0#мир\" ( 2 ) : 1 4\n"
                    + "0#миру\" ( 1 ) : 1\n"));
            // only exact matches in json format
            lucene.checkSearch(
                "/printkeys?field=body_text&user=0&text=fid:2"
                + "&print-total-freqs&max-freq=0&exact&json-type=normal",
                new JsonChecker(
                    "{\"0#алабамы\\\"\":{\"freq\":1,\"total-freq\":1},"
                    + "\"0#двери\\\"\":{\"freq\":1,\"total-freq\":1},"
                    + "\"0#мир\\\"\":{\"freq\":2,\"total-freq\":4},"
                    + "\"0#миру\\\"\":{\"freq\":1,\"total-freq\":1}}"));
            // search by field without terms
            lucene.checkSearch(
                "/printkeys?field=body_text&user=0&text=unread:1&print-docs",
                new StringChecker(""));
        }
    }

    @Test
    public void testSparseQueryFilter() throws Exception {
        try (TestSearchBackend lucene = new TestSearchBackend(
                this,
                false,
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail"))))
        {
            lucene.add("\"url\":\"1\",\"body_text\":\"мир\",\"fid\":\"1\"");
            int batchSize = 10000;
            String[] docs = new String[batchSize];
            for (int i = 0; i < batchSize; ++i) {
                docs[i] = "\"url\":\"10000" + i + "\",\"body_text\":\"миры\"";
            }
            lucene.add(docs);
            lucene.add(
                "\"url\":\"2\",\"body_text\":\"дверь мир\",\"fid\":\"1\"");
            for (int j = 3; j < 13; ++j) {
                for (int i = 0; i < batchSize; ++i) {
                    docs[i] =
                        "\"url\":\"" + j + "0000"
                        + i + "\",\"body_text\":\"двери\"";
                }
                lucene.add(docs);
                lucene.add(
                    "\"url\":\"" + j
                    + "\",\"body_text\":\"дом\",\"fid\":\"1\"");
            }
            lucene.checkSearch(
                "/printkeys?field=body_text&user=0&text=fid:1&print-docs"
                + "&max-freq=0",
                new StringChecker(
                    "0#дверь ( 1 ) : 10001\n"
                    + "0#дверь\" ( 1 ) : 10001\n"
                    + "0#дом ( 10 ) : 20002 30003 40004 50005 60006 70007 "
                    + "80008 90009 100010 110011\n"
                    + "0#дом\" ( 10 ) : 20002 30003 40004 50005 60006 70007 "
                    + "80008 90009 100010 110011\n"
                    + "0#мир ( 2 ) : 0 10001\n"
                    + "0#мир\" ( 2 ) : 0 10001\n"));
            lucene.checkSearch(
                "/printkeys?field=body_text&user=0&text=fid:1&print-freqs"
                + "&max-freq=0",
                new StringChecker(
                    "0#дверь ( 1 )\n"
                    + "0#дверь\" ( 1 )\n"
                    + "0#дом ( 10 )\n"
                    + "0#дом\" ( 10 )\n"
                    + "0#мир ( 2 )\n"
                    + "0#мир\" ( 2 )\n"));
        }
    }

    @Test
    public void testNoTerms() throws Exception {
        Path path = Files.createTempDirectory(testName.getMethodName());
        IniConfig config =
            new IniConfig(
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail")));
        config.put("use-fast-commit-codec", "false");
        config.put("default-field-codec", "Yandex2_aesflate");
        try (TestSearchBackend lucene = new TestSearchBackend(
                path,
                false,
                TestSearchBackend.patchConfig(path, false, config)))
        {
            lucene.add(
                "\"url\":\"1\",\"body_text\":\"мир\"",
                "\"url\":\"2\",\"body_text\":\"миру мир\"",
                "\"url\":\"3\",\"body_text\":\"мир дверь мяч\"");
            lucene.flush();
            lucene.add(
                "\"url\":\"4\",\"message_type\":\"1\"",
                "\"url\":\"5\",\"message_type\":\"1\n2\"",
                "\"url\":\"6\",\"message_type\":\"2 3\"");
            lucene.flush();
            lucene.add(
                "\"url\":\"7\",\"body_text\":\"мир\"",
                "\"url\":\"8\",\"body_text\":\"миру мир\"",
                "\"url\":\"9\",\"body_text\":\"мир дверь мяч\"");
            lucene.flush();
            lucene.checkSearch(
                "/printkeys?field=message_type&skip-deleted&print-freqs"
                + "&max-freq=0&user=0",
                new StringChecker("0#1 ( 2 )\n0#2 ( 2 )\n0#3 ( 1 )\n"));
        }
    }
}

