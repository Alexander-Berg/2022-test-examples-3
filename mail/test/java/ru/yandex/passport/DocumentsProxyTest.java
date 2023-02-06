package ru.yandex.passport;


import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.test.util.TestBase;

public class DocumentsProxyTest extends TestBase {
    @Test
    public void testAdd() throws Exception {
        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this)) {
            String uid = "227356512";
            HttpPost request = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=0");
            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            HttpAssert.assertStatusCode(200, request);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this)) {
            String uid = "227356512";

            HttpPost request = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=0");
            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request.setEntity(new StringEntity("{\"id\":\"0\",\"doc_type\":\"NATIONAL_ID\"}"));
            HttpAssert.assertStatusCode(200, request);


            HttpPost request2 = new HttpPost(
                    cluster.proxy().host().toString() + "/document/update?user_id=" + uid + "&request_service=0");
            request2.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request2.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            HttpAssert.assertStatusCode(400, request2);

            HttpPost request3 = new HttpPost(
                    cluster.proxy().host().toString() + "/document/update?user_id=" + uid + "&request_service=0");
            request3.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request3.setEntity(new StringEntity("{\"id\":\"0\",\"doc_type\":\"NATIONAL_ID\", \"service\":\"passport\"}"));
            HttpAssert.assertStatusCode(200, request3);
        }
    }

    @Test
    public void testImageUpload() throws Exception {
        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this)) {
            String uid = "227356512";
            cluster.avatars().add("/put-id_doc/*",
                    "{\n" +
                            "                \"imagename\": \"imagename\",\n" +
                            "                \"group-id\": \"group-id\",\n" +
                            "                \"sizes\": {\n" +
                            "                    \"orig\":{\"path\":\"path\",\"width\":0,\"height\":0},\n" +
                            "                    \"optimize\":{\"path\":\"path\",\"width\":0,\"height\":0}\n" +
                            "                }\n" +
                            "            }"
            );
            HttpPost request = new HttpPost(
                    cluster.proxy().host().toString() + "/document/image/upload" +
                            "?user_id=" + uid +
                            "&doc_type=national_id" +
                            "&request_service=0");
            request.setEntity(new StringEntity(""));
            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertStatusCode(200, request);
        }
    }

    @Test
    public void testImageDiskCopy() throws Exception {
        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this)) {
            String uid = "227356512";
            cluster.disk().add(
                    "/api/async/passport/disk/document?&uid=227356512&path=/disk/tmp",
                    ""
            );
            cluster.avatars().add("/put-id_doc/*",
                    "{\n" +
                            "                \"imagename\": \"imagename\",\n" +
                            "                \"group-id\": \"group-id\",\n" +
                            "                \"sizes\": {\n" +
                            "                    \"orig\":{\"path\":\"path\",\"width\":0,\"height\":0},\n" +
                            "                    \"optimize\":{\"path\":\"path\",\"width\":0,\"height\":0}\n" +
                            "                }\n" +
                            "            }"
            );
            HttpGet request = new HttpGet(
                    cluster.proxy().host().toString() + "/document/image/disk/copy" +
                            "?user_id=" + uid +
                            "&doc_type=national_id" +
                            "&path=disk:/tmp" +
                            "&request_service=0");
            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertStatusCode(200, request);
        }
    }

    @Test
    public void testGet() throws Exception {
        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String uid = "227356512";
            HttpPost request = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=0");
            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            JsonObject object = TypesafeValueContentHandler.parse(CharsetUtils.content(client.execute(request).getEntity()));
            String docId = object.asMap().get("id").asString();


            HttpGet request2 = new HttpGet(
                    cluster.proxy().host().toString() + "/document/get?id=" + docId + "&user_id=" + uid + "&request_service=0");
            HttpAssert.assertStatusCode(200, request2);

            HttpGet request3 = new HttpGet(
                    cluster.proxy().host().toString() + "/document/get?id=0&user_id=" + uid + "&request_service=0");
            HttpAssert.assertStatusCode(404, request3);
        }
    }

    @Test
    public void testListWithoutAgreement() throws Exception {
        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String uid = "227356512";
            cluster.blackbox().add(
                    "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&attributes=219&sid=2&uid=227356512",
                    "{\"users\":[{\"id\":\"" + uid
                            + "\",\"uid\":{\"value\":\"" + uid
                            + "\",\"lite\":false,\"hosted\":false},\"login\":\"" + "eshemchik"
                            + "\",\"have_password\":true,\"have_hint\":true,\"karma\":{"
                            + "\"value\":0},\"karma_status\":{\"value\":6000},\"phones\": []," +
                            "\"address-list\":[],"
                            + "\"dbfields\":{},\"attributes\":{\"219\":\"false\"}" +
                            "}]}"
            );

            HttpPost request = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=2033365");
            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            HttpAssert.assertStatusCode(200, request);
            HttpAssert.assertCheckerResponse(
                    client,
                    new HttpGet(cluster.proxy().host().toString() + "/document/list?user_id=" + uid + "&request_service=2033365"),
                    response -> {
                        try {
                            if (TypesafeValueContentHandler.parse(response).asMap().get("documents").asList().size() == 1) {
                                return null;
                            } else {
                                return "Documents count != 1";
                            }
                        } catch (JsonException e) {
                            return e.getMessage();
                        }
                    }
            );

            HttpPost request2 = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=0");
            request2.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request2.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            HttpAssert.assertStatusCode(200, request2);
            HttpAssert.assertCheckerResponse(
                    client,
                    new HttpGet(cluster.proxy().host().toString() + "/document/list?user_id=" + uid + "&request_service=2033365"),
                    response -> {
                        try {
                            if (TypesafeValueContentHandler.parse(response).asMap().get("documents").asList().size() == 1) {
                                return null;
                            } else {
                                return "Documents count != 1";
                            }
                        } catch (JsonException e) {
                            return e.getMessage();
                        }
                    }
            );
        }
    }

    @Test
    public void testListWithAgreement() throws Exception {
        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String uid = "227356512";
            cluster.blackbox().add(
                    "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&attributes=219&sid=2&uid=227356512",
                    "{\"users\":[{\"id\":\"" + uid
                            + "\",\"uid\":{\"value\":\"" + uid
                            + "\",\"lite\":false,\"hosted\":false},\"login\":\"" + "eshemchik"
                            + "\",\"have_password\":true,\"have_hint\":true,\"karma\":{"
                            + "\"value\":0},\"karma_status\":{\"value\":6000},\"phones\": []," +
                            "\"address-list\":[],"
                            + "\"dbfields\":{},\"attributes\":{\"219\":\"true\"}" +
                            "}]}"
            );

            HttpPost request = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=0");
            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            HttpAssert.assertStatusCode(200, request);
            HttpAssert.assertCheckerResponse(
                    client,
                    new HttpGet(cluster.proxy().host().toString() + "/document/list?user_id=" + uid + "&request_service=0"),
                    response -> {
                        try {
                            if (TypesafeValueContentHandler.parse(response).asMap().get("documents").asList().size() == 1) {
                                return null;
                            } else {
                                return "Documents count != 1";
                            }
                        } catch (JsonException e) {
                            return e.getMessage();
                        }
                    }
            );

            HttpPost request2 = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=2033365");
            request2.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request2.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            HttpAssert.assertStatusCode(200, request2);
            HttpAssert.assertCheckerResponse(
                    client,
                    new HttpGet(cluster.proxy().host().toString() + "/document/list?user_id=" + uid + "&request_service=0"),
                    response -> {
                        try {
                            if (TypesafeValueContentHandler.parse(response).asMap().get("documents").asList().size() == 2) {
                                return null;
                            } else {
                                return "Documents count != 2";
                            }
                        } catch (JsonException e) {
                            return e.getMessage();
                        }
                    }
            );
        }
    }

    @Test
    public void testDelete() throws Exception {
        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String uid = "227356512";
            HttpPost request = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=0");
            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            JsonObject object = TypesafeValueContentHandler.parse(CharsetUtils.content(client.execute(request).getEntity()));
            String docId = object.asMap().get("id").asString();

            cluster.blackbox().add(
                    "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&attributes=219&sid=2&uid=227356512",
                    "{\"users\":[{\"id\":\"" + uid
                            + "\",\"uid\":{\"value\":\"" + uid
                            + "\",\"lite\":false,\"hosted\":false},\"login\":\"" + "eshemchik"
                            + "\",\"have_password\":true,\"have_hint\":true,\"karma\":{"
                            + "\"value\":0},\"karma_status\":{\"value\":6000},\"phones\": []," +
                            "\"address-list\":[],"
                            + "\"dbfields\":{},\"attributes\":{\"219\":\"true\"}" +
                            "}]}"
            );

            HttpAssert.assertCheckerResponse(
                    client,
                    new HttpGet(cluster.proxy().host().toString() + "/document/list?user_id=" + uid + "&request_service=0"),
                    response -> {
                        try {
                            if (TypesafeValueContentHandler.parse(response).asMap().get("documents").asList().size() == 1) {
                                return null;
                            } else {
                                return "Documents count != 1";
                            }
                        } catch (JsonException e) {
                            return e.getMessage();
                        }
                    }
            );

            HttpGet request2 = new HttpGet(
                    cluster.proxy().host().toString() + "/document/delete?id=" + docId + "&user_id=" + uid + "&request_service=0");
            HttpAssert.assertStatusCode(200, request2);

            HttpAssert.assertCheckerResponse(
                    client,
                    new HttpGet(cluster.proxy().host().toString() + "/document/list?user_id=" + uid + "&request_service=0"),
                    response -> {
                        try {
                            if (TypesafeValueContentHandler.parse(response).asMap().get("documents").asList().size() == 0) {
                                return null;
                            } else {
                                return "Documents count != 0";
                            }
                        } catch (JsonException e) {
                            return e.getMessage();
                        }
                    }
            );
        }
    }

    @Test
    public void testServices() throws Exception {
        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String uid = "227356512";
            HttpAssert.assertJsonResponse(
                    client,
                    cluster.proxy().host().toString() + "/document/services?user_id=" + uid + "&request_service=0",
                    "{\"services\":[]}"
            );

            HttpPost request = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=0");
            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            HttpAssert.assertStatusCode(200, request);
            HttpAssert.assertJsonResponse(
                    client,
                    cluster.proxy().host().toString() + "/document/services?user_id=" + uid + "&request_service=0",
                    "{\"services\":[{\"doc_type\":\"national_id\",\"service_name\":\"test\"}]}"
            );

            HttpPost request2 = new HttpPost(
                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=2033365");
            request2.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
            request2.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
            HttpAssert.assertStatusCode(200, request2);
            HttpAssert.assertJsonResponse(
                    client,
                    cluster.proxy().host().toString() + "/document/services?user_id=" + uid + "&request_service=2033365",
                    "{\"services\":[" +
                            "{\"doc_type\":\"national_id\",\"service_name\":\"test\"}," +
                            "{\"doc_type\":\"national_id\",\"service_name\":\"market\"}" +
                            "]}"
            );
        }
    }

//    @Test
//    public void testVersions() throws Exception {
//        try (DocumentsProxyCluster cluster = new DocumentsProxyCluster(this);
//             CloseableHttpClient client = HttpClients.createDefault())
//        {
//            String uid = "227356512";
//            cluster.blackbox().add(
//                    "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&attributes=219&sid=2&uid=227356512",
//                    "{\"users\":[{\"id\":\"" + uid
//                            + "\",\"uid\":{\"value\":\"" + uid
//                            + "\",\"lite\":false,\"hosted\":false},\"login\":\"" + "eshemchik"
//                            + "\",\"have_password\":true,\"have_hint\":true,\"karma\":{"
//                            + "\"value\":0},\"karma_status\":{\"value\":6000},\"phones\": []," +
//                            "\"address-list\":[],"
//                            + "\"dbfields\":{},\"attributes\":{\"219\":\"true\"}" +
//                            "}]}"
//            );
//
//            HttpPost request = new HttpPost(
//                    cluster.proxy().host().toString() + "/document/add?user_id=" + uid + "&request_service=0");
//            request.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
//            request.setEntity(new StringEntity("{\"doc_type\":\"NATIONAL_ID\"}"));
//            JsonObject object = TypesafeValueContentHandler.parse(CharsetUtils.content(client.execute(request).getEntity()));
//            String docId = object.asMap().get("id").asString();
//            HttpPost request2 = new HttpPost(
//                    cluster.proxy().host().toString() + "/document/update?user_id=" + uid + "&request_service=0");
//            request2.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, DocumentsProxyCluster.TICKET_PASSP_PASSP);
//            request2.setEntity(new StringEntity("{\"id\":\"" + docId + "\",\"doc_type\":\"NATIONAL_ID\"}"));
//            HttpAssert.assertStatusCode(200, request2);
//
//            HttpAssert.assertCheckerResponse(
//                    client,
//                    new HttpGet(cluster.proxy().host().toString() + "/document/list?user_id=" + uid + "&request_service=0"),
//                    response -> {
//                        try {
//                            JsonList docs = TypesafeValueContentHandler.parse(response).asMap().get("documents").asList();
//                            if (docs.size() != 1) {
//                                return "Documents count != 1: " + docs.size();
//                            } else if (docs.get(0).asMap().get("version").asLong() != 1L) {
//                                return "Wrong version returned: " + docs.get(0).asMap().get("version").asLong();
//                            } else {
//                                return null;
//                            }
//                        } catch (JsonException e) {
//                            return e.getMessage();
//                        }
//                    }
//            );
//        }
//    }
}
