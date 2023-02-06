package ru.yandex.msearch;

import org.junit.Test;

import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.search.backend.TestMailSearchBackend;
import ru.yandex.test.util.TestBase;

public class PruningTest extends TestBase {
    private void preparePruningIndex(final TestSearchBackend lucene)
        throws Exception
    {
        final int docsCount = 10000;
        String[] docs = new String[docsCount];
        // Make pruning possible, add tons of docs
        for (int i = 0; i < docsCount; ++i) {
            docs[i] =
            "\"fact_uid\":0,\"url\":\"" + (docsCount + i)
            + "\",\"fact_mid\":\"" + (1234400000 + i) + '"';
        }
        lucene.add(docs);
    }

    @Test
    public void testPruningOrdering() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this, false)) {
            preparePruningIndex(lucene);
            lucene.add(
                "\"fact_uid\":0,\"url\":\"1\",\"fact_mid\":\"1234500000\"");
            lucene.add(
                "\"fact_uid\":0,\"url\":\"2\",\"fact_mid\":\"1234500001\"");
            lucene.flush();
            lucene.add(
                "\"fact_uid\":0,\"url\":\"3\",\"fact_mid\":\"1234500002\"");
            lucene.flush();
            lucene.add(
                "\"fact_uid\":0,\"url\":\"4\",\"fact_mid\":\"1234500003\"",
                "\"fact_uid\":0,\"url\":\"5\",\"fact_mid\":\"1234500003\"",
                "\"fact_uid\":0,\"url\":\"6\",\"fact_mid\":\"1234500003\"");
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=fact_mid&text=fact_uid:0"
                + "&length=4&collector=pruning",
                TestSearchBackend.prepareResult(
                    10006,
                    "\"url\":\"<any value>\"",
                    "\"url\":\"<any value>\"",
                    "\"url\":\"<any value>\"",
                    "\"url\":\"3\""));
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=fact_mid&text=fact_uid:0"
                + "&length=5&collector=pruning",
                TestSearchBackend.prepareResult(
                    10006,
                    "\"url\":\"<any value>\"",
                    "\"url\":\"<any value>\"",
                    "\"url\":\"<any value>\"",
                    "\"url\":\"3\"",
                    "\"url\":\"2\""));
        }
    }

    @Test
    public void testPruningOrderingFailFast() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this, false)) {
            preparePruningIndex(lucene);
            lucene.add(
                "\"fact_uid\":0,\"url\":\"2\",\"fact_mid\":\"1234500001\"");
            lucene.flush();
            lucene.add(
                "\"fact_uid\":0,\"url\":\"3\",\"fact_mid\":\"1234500002\"");
            lucene.flush();
            lucene.add(
                "\"fact_uid\":0,\"url\":\"1\",\"fact_mid\":\"1234500000\"");
            lucene.add(
                "\"fact_uid\":0,\"url\":\"4\",\"fact_mid\":\"1234500003\"",
                "\"fact_uid\":0,\"url\":\"5\",\"fact_mid\":\"1234500003\"",
                "\"fact_uid\":0,\"url\":\"6\",\"fact_mid\":\"1234500003\"");
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=fact_mid&text=fact_uid:0"
                + "&length=4&collector=pruning",
                TestSearchBackend.prepareResult(
                    10006,
                    "\"url\":\"<any value>\"",
                    "\"url\":\"<any value>\"",
                    "\"url\":\"<any value>\"",
                    "\"url\":\"3\""));
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=fact_mid&text=fact_uid:0"
                + "&length=5&collector=pruning",
                TestSearchBackend.prepareResult(
                    10006,
                    "\"url\":\"<any value>\"",
                    "\"url\":\"<any value>\"",
                    "\"url\":\"<any value>\"",
                    "\"url\":\"3\"",
                    "\"url\":\"2\""));
        }
    }

    @Test
    public void testPruningOrderingDeletes() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this, false)) {
            preparePruningIndex(lucene);
            lucene.add(
                "\"fact_uid\":0,\"url\":\"2\",\"fact_mid\":\"1234500001\"");
            lucene.flush();
            lucene.add(
                "\"fact_uid\":0,\"url\":\"3\",\"fact_mid\":\"1234500002\"");
            lucene.flush();
            lucene.add(
                "\"fact_uid\":0,\"url\":\"1\",\"fact_mid\":\"1234500000\"");
            lucene.add(
                "\"fact_uid\":0,\"url\":\"4\",\"fact_mid\":\"1234500003\"",
                "\"fact_uid\":0,\"url\":\"5\",\"fact_mid\":\"1234500003\"",
                "\"fact_uid\":0,\"url\":\"6\",\"fact_mid\":\"1234500003\"");
            lucene.delete("\"url\":\"2\"", "\"url\":\"5\"", "\"url\":\"6\"");
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=fact_mid&text=fact_uid:0"
                + "&length=3&collector=pruning",
                TestSearchBackend.prepareResult(
                    10003,
                    "\"url\":\"4\"",
                    "\"url\":\"3\"",
                    "\"url\":\"1\""));
        }
    }

    @Test
    public void testForcedPruneField() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this, false)) {
            preparePruningIndex(lucene);
            lucene.add(
                "\"url\":1,\"received_date\":1234483198,\"mid\":1",
                "\"url\":2,\"received_date\":1234483199,\"mid\":2",
                "\"url\":3,\"received_date\":1234483200,\"mid\":3",
                "\"url\":4,\"received_date\":1234483201,\"mid\":4");
            lucene.flush();
            lucene.add(
                "\"url\":5,\"received_date\":1234483202,\"mid\":5",
                "\"url\":6,\"received_date\":1234483203,\"mid\":6",
                "\"url\":7,\"received_date\":1234483204,\"mid\":7",
                "\"url\":8,\"received_date\":1234483205,\"mid\":8",
                "\"url\":9,\"received_date\":1234483206,\"mid\":9");
            lucene.flush();
            // Test simplest case
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=received_date"
                + "&text=mid_p:*&length=5&collector=pruning(received_day_p)",
                TestSearchBackend.prepareResult(
                    9,
                    "\"url\":\"9\"",
                    "\"url\":\"8\"",
                    "\"url\":\"7\"",
                    "\"url\":\"6\"",
                    "\"url\":\"5\""));
            // Test simplest case with offset
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=received_date&offset=2"
                + "&text=mid_p:*&length=5&collector=pruning(received_day_p)",
                TestSearchBackend.prepareResult(
                    9,
                    "\"url\":\"7\"",
                    "\"url\":\"6\"",
                    "\"url\":\"5\"",
                    "\"url\":\"4\"",
                    "\"url\":\"3\""));
            // Test simplest case reversed
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=received_date&asc"
                + "&text=mid_p:*&length=5&collector=pruning(received_day_p)",
                TestSearchBackend.prepareResult(
                    9,
                    "\"url\":\"1\"",
                    "\"url\":\"2\"",
                    "\"url\":\"3\"",
                    "\"url\":\"4\"",
                    "\"url\":\"5\""));
            // Test simplest case reversed with offset
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=received_date&asc&offset=1"
                + "&text=mid_p:*&length=5&collector=pruning(received_day_p)",
                TestSearchBackend.prepareResult(
                    9,
                    "\"url\":\"2\"",
                    "\"url\":\"3\"",
                    "\"url\":\"4\"",
                    "\"url\":\"5\"",
                    "\"url\":\"6\""));
            // Test user id field. TODO: Fair check that passthrough took place
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=received_date&asc&offset=1"
                + "&text=mid_p:*&length=3&collector=pruning(received_day_p)"
                + "&user-id-field=__prefix&user-id-term=0",
                TestSearchBackend.prepareResult(
                    9,
                    "\"url\":\"2\"",
                    "\"url\":\"3\"",
                    "\"url\":\"4\""));
            // Check that both segments will be properly scanned
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=received_date"
                + "&text=mid_p:*&length=3&collector=pruning"
                + "&user-id-field=__prefix&user-id-term=0",
                TestSearchBackend.prepareResult(
                    9,
                    "\"url\":\"9\"",
                    "\"url\":\"8\"",
                    "\"url\":\"7\""));
        }
    }

    @Test
    public void testPruningGrouping() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this, false)) {
            preparePruningIndex(lucene);
            lucene.add(
                "\"url\":1,\"received_date\":1234483198,\"mid\":1,"
                + "\"thread_id\":1,\"fid\":1",
                "\"url\":2,\"received_date\":1234483199,\"mid\":2,"
                + "\"thread_id\":2",
                "\"url\":3,\"received_date\":1234483200,\"mid\":3,"
                + "\"thread_id\":3",
                "\"url\":4,\"received_date\":1234483201,\"mid\":4,"
                + "\"thread_id\":1,\"fid\":1",
                "\"url\":5,\"received_date\":1234483202,\"mid\":5,"
                + "\"thread_id\":2",
                "\"url\":6,\"received_date\":1234483203,\"mid\":6,"
                + "\"thread_id\":3",
                "\"url\":7,\"received_date\":1234483204,\"mid\":7,"
                + "\"thread_id\":1,\"fid\":1",
                "\"url\":8,\"received_date\":1234483205,\"mid\":8,"
                + "\"thread_id\":2",
                "\"url\":9,\"received_date\":1234483206,\"mid\":9,"
                + "\"thread_id\":3");
            lucene.flush();
            // Test both simple pruning and pruning with forced prune field
            for (String collector: new String[] {"", "(received_day_p)"}) {
                System.out.println("Collector modificator: " + collector);
                // Check standard pruning
                lucene.checkSearch(
                    "/search?prefix=0&get=url&sort=received_date"
                    + "&text=mid_p:*&length=2&group=thread_id"
                    + "&collector=pruning" + collector,
                    TestSearchBackend.prepareResult(
                        3,
                        "\"url\":\"9\",\"merged_docs\":"
                        + "[{\"url\":\"3\"},{\"url\":\"6\"}]",
                        "\"url\":\"8\",\"merged_docs\":"
                        + "[{\"url\":\"2\"},{\"url\":\"5\"}]"));
                // Check pruning in ascending order
                lucene.checkSearch(
                    "/search?prefix=0&get=url&sort=received_date&asc"
                    + "&text=mid_p:*&length=2&group=thread_id&collector="
                    + "pruning" + collector,
                    TestSearchBackend.prepareResult(
                        3,
                        "\"url\":\"1\",\"merged_docs\":"
                        + "[{\"url\":\"4\"},{\"url\":\"7\"}]",
                        "\"url\":\"2\",\"merged_docs\":"
                        + "[{\"url\":\"5\"},{\"url\":\"8\"}]"));
                // Check grouping for unprefixed field
                lucene.checkSearch(
                    "/search?prefix=0&get=url&sort=received_date"
                    + "&text=url:(1+2+3+4)&length=3&group=url"
                    + "&collector=pruning" + collector,
                    TestSearchBackend.prepareResult(
                        4,
                        "\"url\":\"4\"",
                        "\"url\":\"3\"",
                        "\"url\":\"2\""));
            }
            // Check pruning with unprefixed field and null groups for some
            // documents
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&asc&text=mid_p:*"
                + "&group=fid&length=3&collector=pruning",
                TestSearchBackend.prepareResult(
                    -1,
                    "\"url\":\"1\","
                    + "\"merged_docs\":[{\"url\":\"4\"},{\"url\":\"7\"}]",
                    "\"url\":\"2\"",
                    "\"url\":\"3\""));
        }
    }

    @Test
    public void testPruningGroupingForcedGroupField() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this, false)) {
            preparePruningIndex(lucene);
            //use msg_id as keyword field
            lucene.add(
                "\"url\":1,\"received_date\":1234483198,\"mid\":1,"
                + "\"hdr_to\":1,\"fid\":1,\"hdr_to_keyword\":1",
                "\"url\":2,\"received_date\":1234483199,\"mid\":2,"
                + "\"hdr_to\":2,\"hdr_to_keyword\":2",
                "\"url\":3,\"received_date\":1234483200,\"mid\":3,"
                + "\"hdr_to\":3,\"hdr_to_keyword\":3",
                "\"url\":4,\"received_date\":1234483201,\"mid\":4,"
                + "\"hdr_to\":1,\"fid\":1,\"hdr_to_keyword\":1",
                "\"url\":5,\"received_date\":1234483202,\"mid\":5,"
                + "\"hdr_to\":2,\"hdr_to_keyword\":2",
                "\"url\":6,\"received_date\":1234483203,\"mid\":6,"
                + "\"hdr_to\":3,\"hdr_to_keyword\":3",
                "\"url\":7,\"received_date\":1234483204,\"mid\":7,"
                + "\"hdr_to\":1,\"fid\":1,\"hdr_to_keyword\":1",
                "\"url\":8,\"received_date\":1234483205,\"mid\":8,"
                + "\"hdr_to\":2,\"hdr_to_keyword\":2",
                "\"url\":9,\"received_date\":1234483206,\"mid\":9,"
                + "\"hdr_to\":3,\"hdr_to_keyword\":3");
            lucene.flush();
            // Test both simple pruning and pruning with forced prune field
            for (String group:
                new String[] {
                    "&collector=pruning&pruning-group-field=hdr_to_keyword"
                        + "&force-pruning-group-field"})
            {
                System.out.println("Collector forced group field: " + group);
                // Check standard pruning
                lucene.checkSearch(
                    "/search?prefix=0&get=url&sort=received_date"
                    + "&text=mid_p:*&length=2&group=hdr_to"
                    + group,
                    TestSearchBackend.prepareResult(
                        3,
                        "\"url\":\"9\",\"merged_docs\":"
                        + "[{\"url\":\"3\"},{\"url\":\"6\"}]",
                        "\"url\":\"8\",\"merged_docs\":"
                        + "[{\"url\":\"2\"},{\"url\":\"5\"}]"));
                // Check pruning in ascending order
                lucene.checkSearch(
                    "/search?prefix=0&get=url&sort=received_date&asc"
                    + "&text=mid_p:*&length=2&group=hdr_to"
                    + group,
                    TestSearchBackend.prepareResult(
                        3,
                        "\"url\":\"1\",\"merged_docs\":"
                        + "[{\"url\":\"4\"},{\"url\":\"7\"}]",
                        "\"url\":\"2\",\"merged_docs\":"
                        + "[{\"url\":\"5\"},{\"url\":\"8\"}]"));
            }
            // Check pruning with unprefixed field and null groups for some
            // documents
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&asc&text=mid_p:*"
                + "&group=fid&length=3&collector=pruning",
                TestSearchBackend.prepareResult(
                    -1,
                    "\"url\":\"1\","
                    + "\"merged_docs\":[{\"url\":\"4\"},{\"url\":\"7\"}]",
                    "\"url\":\"2\"",
                    "\"url\":\"3\""));
        }
    }

    @Test
    public void testPruningPrefixlessField() throws Exception {
        try (TestSearchBackend lucene = new TestMailSearchBackend(this)) {
            lucene.add("\"url\":\"1\"", "\"url\":\"2\"", "\"url\":\"3\"");
            lucene.flush();
            lucene.checkSearch(
                "/search?prefix=0&get=url&sort=url&text=url:*&length=2",
                TestSearchBackend.prepareResult(
                    3,
                    "\"url\":\"3\"",
                    "\"url\":\"2\""));
        }
    }
}
