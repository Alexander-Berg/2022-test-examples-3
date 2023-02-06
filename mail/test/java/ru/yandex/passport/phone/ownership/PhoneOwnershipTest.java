package ru.yandex.passport.phone.ownership;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.passport.phone.ownership.parse.MobileOperator;
import ru.yandex.passport.phone.ownership.parse.PhoneParseResult;
import ru.yandex.passport.phone.ownership.parse.PhoneParser;
import ru.yandex.test.util.TestBase;

import static ru.yandex.http.test.HttpAssert.assertJsonResponse;
import static ru.yandex.http.test.HttpAssert.assertStatusCode;

public class PhoneOwnershipTest extends TestBase {
    private HttpPost startTracking(
        final PhoneOwnershipCluster cluster,
        final String phone,
        final boolean draft)
        throws Exception
    {
        String uri = cluster.proxy().host() + "/phone/tracking/start?";
        if (draft) {
            uri += "&draft=true";
        } else {
            uri += "&draft=false";
        }
        HttpPost post = new HttpPost(uri);
        post.setEntity(new StringEntity("{\"phone\": \"" + phone + "\"}", StandardCharsets.UTF_8));
        return post;
    }

    private HttpPost changeStatus(
        final PhoneOwnershipCluster cluster,
        final String phone)
        throws Exception
    {
        String uri = cluster.proxy().host() + "/phone/change/status";
        HttpPost post = new HttpPost(uri);
        post.setEntity(new StringEntity("{\"phone\": \"" + phone + "\"}", StandardCharsets.UTF_8));
        return post;
    }

    @Test
    public void unsubscribeSkipEdnaTest() throws Exception {
        System.setProperty("SKIP_EDNA", "true");

        PhoneOwnershipCluster cluster = new PhoneOwnershipCluster(this);
        cluster.start();

        HttpPost unsubscribeRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/tracking/stop");

        StringEntity foreignNumberEntity = new StringEntity(
            "{\n" +
                "     \"phone\": \"39260000000\"\n" +
                "}\n"
        );

        StringEntity unknownNumberEntity = new StringEntity(
            "{\n" +
                "     \"phone\": \"79260000000\"\n" +
                "}\n"
        );

        unsubscribeRequest.setEntity(foreignNumberEntity);
        assertStatusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY, unsubscribeRequest);

        unsubscribeRequest.setEntity(unknownNumberEntity);
        assertStatusCode(HttpStatus.SC_OK, unsubscribeRequest);
    }

    @Test
    public void phoneStatusSkipEdnaTest() throws Exception {
        System.setProperty("SKIP_EDNA", "true");

        PhoneOwnershipCluster cluster = new PhoneOwnershipCluster(this);
        cluster.start();

        // nonrussian number, untrackable
        assertStatusCode(
            HttpStatus.SC_UNPROCESSABLE_ENTITY,
            startTracking(cluster, "39260000000", true));
        assertStatusCode(
            HttpStatus.SC_UNPROCESSABLE_ENTITY,
            changeStatus(cluster, "39260000000"));
        // unknown number - not present in db
        assertStatusCode(
            HttpStatus.SC_PAYMENT_REQUIRED,
            changeStatus(cluster, "79300000000"));
        assertStatusCode(
            HttpStatus.SC_OK,
            startTracking(cluster, "79300000000", true));
        Thread.sleep(200);
        // checking that after tracking draft=true number is no saved to db
        assertStatusCode(
            HttpStatus.SC_PAYMENT_REQUIRED,
            changeStatus(cluster, "79300000000"));

        long ts = 1558462880;
        addPhoneToStorage("79300000000", ts, "imsi", false, cluster);
        String expected =
            "{\"status\": \"ok\",\"change_date\":" + TimeUnit.MILLISECONDS.toSeconds(ts) + "}";
        // test after adding phone to db
        CloseableHttpClient client = Configs.createDefaultClient();
        HttpAssert.assertJsonResponse(
            client,
            startTracking(cluster, "79300000000", true),
            expected);
        HttpAssert.assertJsonResponse(
            client,
            changeStatus(cluster, "79300000000"),
            expected);
    }

    @Test
    public void updateTest() throws Exception {
        PhoneOwnershipCluster cluster = new PhoneOwnershipCluster(this);
        cluster.start();
        HttpPost changeStatusRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/change/status");

        changeStatusRequest.setEntity(
            new StringEntity(
                "{\n" +
                    "     \"phone\": \"79260000000\"\n" +
                    "}\n"
            ));

        long ts = 1558462880;
        HttpPost updateRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/manual/update?phone=%2B79260000000&ts=" + ts);
        assertStatusCode(HttpStatus.SC_OK, Configs.createDefaultClient(), updateRequest);

        HttpPost phoneChangeStatusRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/change/status");
        phoneChangeStatusRequest.setEntity(
            new StringEntity(
                "{\n" +
                    "     \"phone\": \"79260000000\"\n" +
                    "}\n"
            ));

        String expected2 = "{\n" +
            "    \"status\": \"ok\",\n" +
            "    \"change_date\":" + TimeUnit.MILLISECONDS.toSeconds(ts) + "\n" +
            "}";

        assertJsonResponse(Configs.createDefaultClient(), phoneChangeStatusRequest, expected2);
    }

    @Ignore
    @Test
    public void phoneStartTrackingDraftFalseTest() throws Exception {
        //System.setProperty("SKIP_EDNA", "false");

        PhoneOwnershipCluster cluster = new PhoneOwnershipCluster(this);
        cluster.start();

        HttpPost startTrackingRequestDraftFalse = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/tracking/start?draft=false&skip-edna=false");
        HttpPost phoneChangeStatusRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/change/status?&skip-edna=false");

        StringEntity notRussianNumberEntity = new StringEntity(
            "{\n" +
                "     \"phone\": \"39260000000\"\n" +
                "}\n"
        );
        startTrackingRequestDraftFalse.setEntity(notRussianNumberEntity);
        assertStatusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY, startTrackingRequestDraftFalse);

        StringEntity unknownNumberEntity = new StringEntity(
            "{\n" +
                "     \"phone\": \"79300000000\"\n" +
                "}\n"
        );

        startTrackingRequestDraftFalse.setEntity(unknownNumberEntity);
        phoneChangeStatusRequest.setEntity(unknownNumberEntity);
        assertStatusCode(HttpStatus.SC_PAYMENT_REQUIRED, startTrackingRequestDraftFalse);

        long ts = 1558462880;
        addPhoneToStorage("79300000000", ts, "imsi", true, cluster);
        cluster.ednaHandler().addPhone("79300000000", "imsi");

        Thread.sleep(1000);

        startTrackingRequestDraftFalse.setEntity(unknownNumberEntity);
        assertStatusCode(HttpStatus.SC_OK, startTrackingRequestDraftFalse);

        HttpResponse response = Configs.createDefaultClient().execute(phoneChangeStatusRequest);
        JsonObject jsonObject = getJsonObjectFromResponse(response);
        long timestamp = jsonObject.get("change_date").getAsLong();
        String status = jsonObject.get("status").getAsString();
        Assert.assertEquals("ok", status);
        Assert.assertEquals(timestamp, TimeUnit.MILLISECONDS.toSeconds(ts));
    }

    @Test
    public void startTrackingDraftTrueTest() throws Exception {
        Field field = Paths.class.getDeclaredField("sourceRoot");
        field.setAccessible(true);
        field.set(null, "/home/vonidu/Projects/PS/repo/arc2/arcadia");
        //System.setProperty("SKIP_EDNA", "false");
        PhoneOwnershipCluster cluster = new PhoneOwnershipCluster(this);
        cluster.start();

        HttpPost startTrackingRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/tracking/start?draft=true&skip-edna=false");

        StringEntity notRussianNumberEntity = new StringEntity(
            "{\n" +
                "     \"phone\": \"39260000000\"\n" +
                "}\n"
        );
        startTrackingRequest.setEntity(notRussianNumberEntity);
        assertStatusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY, startTrackingRequest);

        StringEntity notMonitoredNumber = new StringEntity(
            "{\n" +
                "     \"phone\": \"79260000000\"\n" +
                "}\n"
        );
        startTrackingRequest.setEntity(notMonitoredNumber);
        assertStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR, startTrackingRequest);

        cluster.ednaHandler().addPhone("79260000000", "FD04FDE0CFEE112A9FCFC02587256DEF");

        HttpResponse response = Configs.createDefaultClient().execute(startTrackingRequest);
        JsonObject jsonObject = getJsonObjectFromResponse(response);
        long timestamp = jsonObject.get("change_date").getAsLong();
        String status = jsonObject.get("status").getAsString();
        Assert.assertEquals("ok", status);
        Assert.assertEquals(timestamp, 0);

        cluster.ednaHandler().addPhone("79260000000", "FD04FDE0CFEE112A9FCFC02587256DEE");
        Thread.sleep(1000);

        response = Configs.createDefaultClient().execute(startTrackingRequest);
        jsonObject = getJsonObjectFromResponse(response);
        long timestamp2 = jsonObject.get("change_date").getAsLong();
        status = jsonObject.get("status").getAsString();
        Assert.assertEquals("already_tracking", status);
        Assert.assertEquals(timestamp2, 0);
    }

    @Ignore
    @Test
    public void phoneChangeStatusTest() throws Exception {
        //System.setProperty("SKIP_EDNA", "false");
        PhoneOwnershipCluster cluster = new PhoneOwnershipCluster(this);
        cluster.start();

        HttpPost phoneChangeStatusRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/change/status?&skip-edna=false");

        StringEntity notRussianNumberEntity = new StringEntity(
            "{\n" +
                "     \"phone\": \"39260000000\"\n" +
                "}\n"
        );
        phoneChangeStatusRequest.setEntity(notRussianNumberEntity);
        assertStatusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY, phoneChangeStatusRequest);

        StringEntity notMonitoredNumber = new StringEntity(
            "{\n" +
                "     \"phone\": \"79260000000\"\n" +
                "}\n"
        );

        phoneChangeStatusRequest.setEntity(notMonitoredNumber);
        assertStatusCode(HttpStatus.SC_PAYMENT_REQUIRED, phoneChangeStatusRequest);

        long ts = 1558462880;
        addPhoneToStorage("79260000000", ts, "imsi1", true, cluster);
        cluster.ednaHandler().addPhone("79260000000", "imsi1");

        HttpResponse response = Configs.createDefaultClient().execute(phoneChangeStatusRequest);
        JsonObject jsonObject = getJsonObjectFromResponse(response);
        long timestamp = jsonObject.get("change_date").getAsLong();
        String status = jsonObject.get("status").getAsString();
        Assert.assertEquals("ok", status);
        Assert.assertEquals(timestamp, TimeUnit.MILLISECONDS.toSeconds(ts));

        cluster.ednaHandler().addPhone("79260000000", "imsi2");
        response = Configs.createDefaultClient().execute(phoneChangeStatusRequest);
        jsonObject = getJsonObjectFromResponse(response);
        timestamp = jsonObject.get("change_date").getAsLong();
        status = jsonObject.get("status").getAsString();
        Assert.assertEquals("changed", status);


        HttpPost allowCachedRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/change/status?allow_cached=true&skip-edna=false");
        allowCachedRequest.setEntity(notMonitoredNumber);
        cluster.ednaHandler().addPhone("79260000000", "imsi3");
        Thread.sleep(1000);

        response = Configs.createDefaultClient().execute(allowCachedRequest);
        jsonObject = getJsonObjectFromResponse(response);
        long timestampAllowCached = jsonObject.get("change_date").getAsLong();
        status = jsonObject.get("status").getAsString();
        Assert.assertEquals("ok", status);
        Assert.assertEquals(timestampAllowCached, timestamp);
    }

    @Ignore
    @Test
    public void unsubscribeTest() throws Exception {
        //System.setProperty("SKIP_EDNA", "false");

        PhoneOwnershipCluster cluster = new PhoneOwnershipCluster(this);
        cluster.start();

        StringEntity notMonitoredNumberEntity = new StringEntity(
            "{\n" +
                "     \"phone\": \"79260000000\"\n" +
                "}\n"
        );
        HttpPost phoneStartTrackingRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/tracking/start?draft=true&skip-edna=false");
        phoneStartTrackingRequest.setEntity(notMonitoredNumberEntity);

        HttpPost unsubscribeRequest = new HttpPost("http://localhost:" + cluster.proxy().port() +
            "/phone/tracking/stop");

        StringEntity notRussianNumberEntity = new StringEntity(
            "{\n" +
                "     \"phone\": \"39260000000\"\n" +
                "}\n"
        );
        unsubscribeRequest.setEntity(notRussianNumberEntity);
        assertStatusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY, unsubscribeRequest);

        unsubscribeRequest.setEntity(notMonitoredNumberEntity);

        assertStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR, phoneStartTrackingRequest);
        assertStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR, unsubscribeRequest);

        cluster.ednaHandler().addPhone("79260000000", "imsi");

        assertStatusCode(HttpStatus.SC_OK, phoneStartTrackingRequest);

        String expected3 = "{\n" +
            "    \"status\": \"ok\"\n" +
            "}";
        assertJsonResponse(Configs.createDefaultClient(), unsubscribeRequest, expected3);

        assertStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR, Configs.createDefaultClient(), unsubscribeRequest);

        unsubscribeRequest.setEntity(
            new StringEntity(
                "{\n" +
                    "     \"phone\": \"79260000001\"\n" +
                    "}\n"
            ));

        assertStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR, Configs.createDefaultClient(), unsubscribeRequest);
    }

    @Test
    public void convertToLucene() throws Exception {
        PhoneParseResult result = PhoneParser.RUSSIA.parse(null, "+70000000000");
        System.out.println(result.phone());
        PhoneParseResult result2 = PhoneParser.RUSSIA.parse(null, "+79260000000");
        System.out.println(result2.phone());

        Phonenumber.PhoneNumber phone = PhoneNumberUtil.getInstance().parse("+79261369098", "RU");
        //PhoneNumberToCarrierMapper mapper = PhoneNumberToCarrierMapper.getInstance();
        //String name = mapper.getNameForNumber(phone, new Locale("ru", "RU"));
        //System.out.println("Name " + name);
        MobileOperator operator = new MobileOperator(0, 1, "Акционерное общество \"Крымтелеком\"", "");
        //MobileOperatorMapping mapping = new MobileOperatorMapping();
        //MobileOperator operator = mapping.find(79261369098L);
        System.out.println(operator.normalized());
    }

    private JsonObject getJsonObjectFromResponse(HttpResponse response) throws HttpException, IOException {
        String responseString = CharsetUtils.toString(response.getEntity());
        JsonElement responseJson = JsonParser.parseString(responseString);
        JsonObject jsonObject = responseJson.getAsJsonObject();
        return jsonObject;
    }

    private long getTsFromResponse(HttpResponse response) throws HttpException, IOException {
        JsonObject jsonObject = getJsonObjectFromResponse(response);
        return jsonObject.get("change_date").getAsLong();
    }

    private void addPhoneToStorage(String phoneStr, long ts, String imsi, boolean onMonitoring,
                                   PhoneOwnershipCluster cluster) throws Exception {
        PhoneParseResult parseResult = PhoneParser.RUSSIA.parse(logger, phoneStr);
        long nphone = PhoneOwnershipProxy.normalizePhone(phoneStr);
        PhoneInfo phoneInfo = new PhoneInfo(
            parseResult.phone(),
            nphone,
            phoneStr,
            cluster.proxy().operatorMapping().find(nphone),
            false,
            null,
            ts,
            null,
            false);
        cluster.searchBackend().add("\"id\":\"phone_" + phoneInfo.phoneNum() + "\", \"phone\" : \"" + phoneInfo.fullPhone() + "\", " +
            "\"imsi_change_time\":\"" + ts + "\", \"imsi\":\"" + imsi + "\", \"on_monitoring\":" + onMonitoring + "");
    }
}
