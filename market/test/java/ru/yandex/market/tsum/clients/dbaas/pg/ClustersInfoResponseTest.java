package ru.yandex.market.tsum.clients.dbaas.pg;

import com.google.gson.Gson;
import org.junit.Test;

import ru.yandex.market.tsum.clients.dbaas.DbaasApiClient;
import ru.yandex.market.tsum.clients.dbaas.DbaasClusterType;

import static org.junit.Assert.assertEquals;

public class ClustersInfoResponseTest {

    @Test
    public void testDeserialize() {
        String example = "{\n" +
            "    \"clusters\": [\n" +
            "        {\n" +
            "            \"createdAt\": \"2018-06-27T18:00:45.691836+00:00\",\n" +
            "            \"databaseOptions\": {\n" +
            "                \"databases\": [\n" +
            "                    {\n" +
            "                        \"name\": \"market_dtps_test\",\n" +
            "                        \"options\": {\n" +
            "                            \"extensions\": [],\n" +
            "                            \"owner\": \"market_dtps_test\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"pooler\": {\n" +
            "                    \"pool_mode\": \"session\"\n" +
            "                },\n" +
            "                \"postgres\": {\n" +
            "                    \"max_connections\": 200\n" +
            "                },\n" +
            "                \"users\": [\n" +
            "                    {\n" +
            "                        \"name\": \"market_dtps_test\",\n" +
            "                        \"options\": {\n" +
            "                            \"conn_limit\": 130,\n" +
            "                            \"databases\": [\n" +
            "                                \"market_dtps_test\"\n" +
            "                            ]\n" +
            "                        }\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"version\": \"10\"\n" +
            "            },\n" +
            "            \"environment\": \"prestable\",\n" +
            "            \"id\": \"d318a6b8-3f59-4ff7-b95a-be39e79517d7\",\n" +
            "            \"infrastructureOptions\": {\n" +
            "                \"hosts\": [\n" +
            "                    {\n" +
            "                        \"name\": \"iva-m3h0g6ckcpr66a1a.db.yandex.net\",\n" +
            "                        \"options\": {\n" +
            "                            \"geo\": \"iva\",\n" +
            "                            \"type\": \"postgresql\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"name\": \"sas-dzgz3kr2ts4bk3s8.db.yandex.net\",\n" +
            "                        \"options\": {\n" +
            "                            \"geo\": \"sas\",\n" +
            "                            \"type\": \"postgresql\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"name\": \"vla-avn3c17yxklkxsmr.db.yandex.net\",\n" +
            "                        \"options\": {\n" +
            "                            \"geo\": \"vla\",\n" +
            "                            \"type\": \"postgresql\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"instanceType\": \"db1.small\",\n" +
            "                \"volumeSize\": 10737418240\n" +
            "            },\n" +
            "            \"monitoring\": [\n" +
            "                {\n" +
            "                    \"description\": \"Golovan (YaSM) graphs\",\n" +
            "                    \"link\": \"https://yasm.yandex-team" +
            ".ru/template/panel/dbaas_postgres_metrics/cid=d318a6b8-3f59-4ff7-b95a-be39e79517d7;" +
            "dbname=market_dtps_test\",\n" +
            "                    \"name\": \"golovan\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"name\": \"market_dtps_test\",\n" +
            "            \"projectId\": \"183ec711-e889-4f1a-9ac3-0a2b51abfe1f\",\n" +
            "            \"status\": \"RUNNING\",\n" +
            "            \"type\": \"postgresql_cluster\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"nextPageToken\": " +
            "\"MT8yMDE4LTA2LTI3IDIxOjAwOjQ1LjY5MTgzNiswMzowMD9kMzE4YTZiOC0zZjU5LTRmZjctYjk1YS1iZTM5ZTc5NTE3ZDc=\"\n" +
            "}";
        Gson gson = DbaasApiClient.getGson();
        ClustersInfoResponse response = gson.fromJson(example, ClustersInfoResponse.class);

        assertEquals("MT8yMDE4LTA2LTI3IDIxOjAwOjQ1LjY5MTgzNiswMzowMD9kMzE4YTZiOC0zZjU5LTRmZjctYjk1YS1iZTM5ZTc5NTE3ZDc" +
            "=", response.getNextPageToken());
        assertEquals(1, response.getClusters().size());
        assertEquals(DbaasClusterType.POSTGRESQL, response.getClusters().get(0).getType());
        assertEquals("market_dtps_test", response.getClusters().get(0).getName());
        assertEquals("db1.small", response.getClusters().get(0).getInfrastructureOptions().getInstanceType());
    }

}
