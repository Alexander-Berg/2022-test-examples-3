package ru.yandex.search.district;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class DistrictIndexerTest extends TestBase {
    // CSOFF: MultipleStringLiterals
    // CSOFF: MagicNumber
    @Test
    public void testNoDistrict() throws Exception {
        try (DistrictIndexerCluster cluster =
                 new DistrictIndexerCluster(this)) {
            HttpPost post =
                new HttpPost(cluster.host() + "/api/district/index");
            post.setEntity(new StringEntity("[\n"
                + "    {\n"
                + "        \"change_type\": \"add\",\n"
                + "        \"fields\": {\n"
                + "            \"entity_type\": \"event\",\n"
                + "            \"city_id\": \"1\",\n"
                + "            \"text\":\"Я пришел домой\","
                + "            \"entity_id\": 45214\n"
                + "        }"
                + "    }]", StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            cluster.searchBackend().checkSearch(
                "/search?text=id:*&get=*&prefix=1",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"district_id\": \"-1\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"index_type\": \"city\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"id\": \"city_1_event_-1_45214\",\n"
                    + "\"text\": \"Я пришел домой\"\n"
                    + "}]}"));
        }
    }

    @Test
    public void test() throws Exception {
        try (DistrictIndexerCluster cluster =
                 new DistrictIndexerCluster(this))
        {
            HttpPost post =
                new HttpPost(cluster.host() + "/api/district/index");
            post.setEntity(new StringEntity("[\n"
                + "    {\n"
                + "        \"change_type\": \"add\",\n"
                + "        \"fields\": {\n"
                + "            \"entity_type\": \"event\",\n"
                + "            \"city_id\": \"1\",\n"
                + "            \"text\":\"Я пришел домой\","
                + "            \"district_id\": 1881,\n"
                + "            \"entity_id\": 45214\n"
                + "        }"
                + "    }]", StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            cluster.searchBackend().checkSearch(
                "/search?text=id:event_1881_45214&get=*&prefix=1881",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"district_id\": \"1881\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"index_type\": \"district\",\n"
                    + "\"id\": \"event_1881_45214\",\n"
                    + "\"text\": \"Я пришел домой\"\n"
                    + "}]}"));
            cluster.searchBackend().checkSearch(
                "/search?text=id:city_1_event_1881_45214&get=*&prefix=1",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"district_id\": \"1881\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"index_type\": \"city\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"id\": \"city_1_event_1881_45214\",\n"
                    + "\"text\": \"Я пришел домой\"\n"
                    + "}]}"));

            post.setEntity(new StringEntity("[\n"
                + "    {\n"
                + "        \"change_type\": \"add\",\n"
                + "        \"fields\": {\n"
                + "            \"entity_type\": \"event\",\n"
                + "            \"text\":\"Или поработать\","
                + "            \"district_id\": 1885,\n"
                + "            \"city_id\": \"2\",\n"
                + "            \"districts\": [1882,1883],\n"
                + "            \"entity_id\": 45214\n"
                + "        }"
                + "    }]", StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            cluster.searchBackend().checkSearch(
                "/search?text=id:event_*&get=*&sort=__prefix",
                new JsonChecker("{\"hitsCount\": 3, \"hitsArray\": [{"
                    + "\"district_id\": \"1883\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"city_id\": \"2\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"index_type\": \"district\",\n"
                    + "\"id\": \"event_1883_45214\",\n"
                    + "\"text\": \"Или поработать\""
                    + "},{"
                    + "\"district_id\": \"1882\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"city_id\": \"2\",\n"
                    + "\"index_type\": \"district\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"id\": \"event_1882_45214\",\n"
                    + "\"text\": \"Или поработать\"\n"
                    + "},{"
                    + "\"district_id\": \"1881\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"index_type\": \"district\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"id\": \"event_1881_45214\",\n"
                    + "\"text\": \"Я пришел домой\"\n"
                    + "}]}"));

            post.setEntity(new StringEntity("[\n"
                + "    {\n"
                + "        \"change_type\": \"update\",\n"
                + "        \"fields\": {\n"
                + "            \"entity_type\": \"event\",\n"
                + "            \"city_id\": \"1\",\n"
                + "            \"looks_nice\": \"true\",\n"
                + "            \"text\":\"Я пришел домой\","
                + "            \"district_id\": 1881,\n"
                + "            \"entity_id\": 45214\n"
                + "        }"
                + "    }]", StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            cluster.searchBackend().checkSearch(
                "/search?text=id:city_1_event_1881_45214&get=*&prefix=1",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"district_id\": \"1881\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"index_type\": \"city\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"looks_nice\": \"true\",\n"
                    + "\"id\": \"city_1_event_1881_45214\",\n"
                    + "\"text\": \"Я пришел домой\"\n"
                    + "}]}"));
            //test old post comment
            post.setEntity(new StringEntity("[{\n"
                + "    \"change_type\": \"add\",\n"
                + "    \"fields\": {\n"
                + "        \"event_id\": 328231,\n"
                + "        \"entity_type\": \"comment\",\n"
                + "        \"user_id\": 63038,\n"
                + "        \"dislikes_cnt\": 0,\n"
                + "        \"districts\": [\n"
                + "            -1\n"
                + "        ],\n"
                + "        \"likes_cnt\": 0,\n"
                + "        \"created_at\": 1533541595,\n"
                + "        \"text\": \"А чего не позвали? Только и можете "
                + "хвастаться (((( (шутю)\",\n"
                + "        \"district_id\": -1,\n"
                + "        \"entity_id\": 347678,\n"
                + "        \"city_id\": null\n"
                + "    }\n"
                + "}]", StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            cluster.searchBackend().checkSearch(
                "/search?text=id:comment_0_347678&get=*&sort=__prefix",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"district_id\": \"0\",\n"
                    + "\"entity_id\": \"347678\",\n"
                    + "\"created_at\": \"1533541595\",\n"
                    + "\"event_id\": \"328231\",\n"
                    + "\"likes_cnt\": \"0\",\n"
                    + "\"index_type\": \"district\",\n"
                    + "\"dislikes_cnt\": \"0\",\n"
                    + "\"user_id\": \"63038\",\n"
                    + "\"entity_type\": \"comment\",\n"
                    + "\"id\": \"comment_0_347678\",\n"
                    + "\"text\": \"А чего не позвали? Только и можете "
                    + "хвастаться (((( (шутю)\""
                    + "}]}"));
        }
    }

    @Test
    public void testDelete() throws Exception {
        try (DistrictIndexerCluster cluster =
                 new DistrictIndexerCluster(this))
        {
            HttpPost addPost =
                new HttpPost(cluster.host() + "/api/district/index");
            addPost.setEntity(new StringEntity("[\n"
                + "    {\n"
                + "        \"change_type\": \"add\",\n"
                + "        \"fields\": {\n"
                + "            \"entity_type\": \"event\",\n"
                + "            \"city_id\": \"1\",\n"
                + "            \"title\": \"Тайтл\",\n"
                + "            \"text\":\"Я пришел домой\","
                + "            \"district_id\": 1881,\n"
                + "            \"entity_id\": 45214\n"
                + "        }"
                + "    }]", StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, addPost);

            HttpPost addComment =
                new HttpPost(cluster.host() + "/api/district/index");

            addComment.setEntity(new StringEntity("[\n"
                + "    {\n"
                + "        \"change_type\": \"add\",\n"
                + "        \"fields\": {\n"
                + "            \"entity_type\": \"comment\",\n"
                + "            \"city_id\": \"1\",\n"
                + "            \"event_id\": \"45214\",\n"
                + "            \"text\":\"Комментарий\","
                + "            \"district_id\": 1881,\n"
                + "            \"entity_id\": 55214\n"
                + "        }"
                + "    }]", StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, addComment);

            cluster.searchBackend().checkSearch(
                "/search?text=id:event_1881_45214&get=*&prefix=1881",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"district_id\": \"1881\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"comments_cnt\": \"1\",\n"
                    + "\"title\": \"Тайтл\","
                    + "\"index_type\": \"district\",\n"
                    + "\"id\": \"event_1881_45214\",\n"
                    + "\"text\": \"Я пришел домой\"\n"
                    + "}]}"));

            cluster.searchBackend().checkSearch(
                "/search?text=id:city_1_event_1881_45214&get=*&prefix=1",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"district_id\": \"1881\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"title\": \"Тайтл\",\n"
                    + "\"comments_cnt\": \"1\",\n"
                    + "\"index_type\": \"city\",\n"
                    + "\"id\": \"city_1_event_1881_45214\",\n"
                    + "\"text\": \"Я пришел домой\"\n"
                    + "}]}"));

            cluster.searchBackend().checkSearch(
                "/search?text=id:comment_1881_55214&get=*&prefix=1881",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"district_id\": \"1881\",\n"
                    + "\"entity_id\": \"55214\",\n"
                    + "\"entity_type\": \"comment\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"event_id\": \"45214\",\n"
                    + "\"index_type\": \"district\",\n"
                    + "\"id\": \"comment_1881_55214\",\n"
                    + "\"text\": \"Комментарий\"\n"
                    + "}]}"));

            HttpPost delComment =
                new HttpPost(cluster.host() + "/api/district/index");
            delComment.setEntity(new StringEntity("[\n"
                + "    {\n"
                + "        \"change_type\": \"delete\",\n"
                + "        \"fields\": {\n"
                + "            \"entity_type\": \"comment\",\n"
                + "            \"city_id\": \"1\",\n"
                + "            \"event_id\": \"45214\",\n"
                + "            \"district_id\": 1881,\n"
                + "            \"entity_id\": 55214\n"
                + "        }"
                + "    }]", StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, delComment);

            cluster.searchBackend().checkSearch(
                "/search?text=id:event_1881_45214&get=*&prefix=1881",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"district_id\": \"1881\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"comments_cnt\": \"0\",\n"
                    + "\"title\": \"Тайтл\","
                    + "\"index_type\": \"district\",\n"
                    + "\"id\": \"event_1881_45214\",\n"
                    + "\"text\": \"Я пришел домой\"\n"
                    + "}]}"));

            cluster.searchBackend().checkSearch(
                "/search?text=id:city_1_event_1881_45214&get=*&prefix=1",
                new JsonChecker("{\"hitsCount\": 1, \"hitsArray\": [{"
                    + "\"district_id\": \"1881\",\n"
                    + "\"entity_id\": \"45214\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"city_id\": \"1\",\n"
                    + "\"comments_cnt\": \"0\",\n"
                    + "\"index_type\": \"city\",\n"
                    + "\"title\": \"Тайтл\","
                    + "\"id\": \"city_1_event_1881_45214\",\n"
                    + "\"text\": \"Я пришел домой\"\n"
                    + "}]}"));

            HttpPost delPost =
                new HttpPost(cluster.host() + "/api/district/index");
            delPost.setEntity(new StringEntity("[\n"
                + "    {\n"
                + "        \"change_type\": \"delete\",\n"
                + "        \"fields\": {\n"
                + "            \"entity_type\": \"event\",\n"
                + "            \"city_id\": \"1\",\n"
                + "            \"district_id\": 1881,\n"
                + "            \"entity_id\": 45214\n"
                + "        }"
                + "    }]", StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, delPost);
            cluster.searchBackend().checkSearch(
                "/search?text=id:*&get=*&prefix=1",
                new JsonChecker(
                    "{\"hitsCount\": 0, \"hitsArray\": []}"));

            cluster.searchBackend().checkSearch(
                "/search?text=id:*&get=*&prefix=1881",
                new JsonChecker(
                    "{\"hitsCount\": 0, \"hitsArray\": []}"));
        }
    }

    @Test
    public void testTags() throws Exception {
        String indexBatch = "[{\n"
            + "    \"change_type\": \"add\",\n"
            + "    \"fields\": {\n"
            + "        \"dislikes_cnt\": 0,\n"
            + "        \"districts\": [\n"
            + "            1774\n"
            + "        ],\n"
            + "        \"likes_cnt\": 0,\n"
            + "        \"created_at\": 1558607036,\n"
            + "        \"entity_id\": 766611,\n"
            + "        \"tags\": [\n"
            + "        ],\n"
            + "        \"event_type\": \"news\",\n"
            + "        \"entity_type\": \"event\",\n"
            + "        \"user_id\": 5229218,\n"
            + "        \"views_cnt\": 0,\n"
            + "        \"last_comment_at\": 1558607036,\n"
            + "        \"text\": \"1111111\",\n"
            + "        \"district_id\": 1774,\n"
            + "        \"city_id\": 3\n"
            + "    }\n"
            + "}]";
        try (DistrictIndexerCluster cluster =
                 new DistrictIndexerCluster(this))
        {
            HttpPost post =
                new HttpPost(cluster.host() + "/api/district/index");
            post.setEntity(
                new StringEntity(
                    indexBatch,
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
            cluster.searchBackend().checkSearch(
                "/search?text=id:event_*&get=*&prefix=1774",
                new JsonChecker("{\"hitsArray\": [{"
                    + "\"city_id\": \"3\",\n"
                    + "\"created_at\": \"1558607036\",\n"
                    + "\"dislikes_cnt\": \"0\",\n"
                    + "\"district_id\": \"1774\",\n"
                    + "\"entity_id\": \"766611\",\n"
                    + "\"entity_type\": \"event\",\n"
                    + "\"event_type\": \"news\",\n"
                    + "\"id\": \"event_1774_766611\",\n"
                    + "\"last_comment_at\": \"1558607036\",\n"
                    + "\"likes_cnt\": \"0\",\n"
                    + "\"index_type\": \"district\",\n"
                    + "\"tags\": \"\",\n"
                    + "\"text\": \"1111111\",\n"
                    + "\"user_id\": \"5229218\",\n"
                    + "\"views_cnt\": \"0\"}],\"hitsCount\": 1}"));
        }

        // test old post
    }

    // CSON: MultipleStringLiterals
    // CSON: MagicNumber
}
