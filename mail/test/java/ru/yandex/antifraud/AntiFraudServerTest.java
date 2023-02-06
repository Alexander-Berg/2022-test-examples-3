package ru.yandex.antifraud;

import java.nio.file.Files;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.antifraud.currency.Amount;
import ru.yandex.antifraud.data.Field;
import ru.yandex.antifraud.data.ScoringData;
import ru.yandex.antifraud.invariants.TransactionStatus;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AntiFraudServerTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (Cluster cluster = new Cluster(this, resource("channels.conf").toString());
             CloseableHttpClient client = Configs.createDefaultClient()) {

            cluster.passport().add("/2/account/448130096/password_options/?consumer=SoFraud&action=change_password",
                    new ExpectingHttpItem(
                            new StringChecker("antifraud_external_id=5eb59fcadff13b6faf122757&comment=change_password" +
                                    "+lomanovvasiliy&max_change_frequency_in_days=5&is_changing_required=1&admin_name" +
                                    "=luckybug&notify_by_sms=false&show_2fa_promo=true"),
                            "{\"status\": \"ok\"}"));

            cluster.passport().add("/2/account/448130096/password_options/?antifraud_external_id" +
                            "=5eb59fcadff13b6faf122757&consumer=SoFraud&action=logout",
                    new ExpectingHttpItem(
                            new StringChecker("admin_name=luckybug&comment=logout+luckybug" +
                                    "&global_logout=1"),
                            "{\"status\": \"ok\"}"));

            cluster.passport().add("/1/bundle/auth/password/challenge/send_email/" +
                            "?consumer=SoFraud" +
                            "&antifraud_external_id=5eb59fcadff13b6faf122757",
                    new ExpectingHttpItem(
                            new StringChecker("is_challenged=false&uid=448130096"),
                            "{\"email_sent\": true}"));

            cluster.dedupliactedPassport().add("/1/bundle/push/send/am/" +
                            "?antifraud_external_id=5eb59fcadff13b6faf122757" +
                            "&consumer=SoFraud",
                    new ExpectingHttpItem(
                            new StringChecker("uid=1120000000036393&" +
                                    "subtitle=some+subtitle&" +
                                    "title=some+title&" +
                                    "require_web_auth=true&" +
                                    "event_name=some_event_name&" +
                                    "push_service=push_name&" +
                                    "body=some+body&" +
                                    "webview_url=localhost"),
                            "{\"status\": \"ok\"}"));

            cluster.cache().add("/put?key=some-key&ttl=3h",
                    new ExpectingHttpItem(
                            new JsonChecker("{\"key\": \"value\"}"),
                            "123"));

            cluster.cache().add("/get?key=some-key",
                    "{\"key\": \"value\"}");

            cluster.cache().add("/get?key=another-key",
                    HttpStatus.SC_NOT_FOUND,
                    "Not found");

            cluster.passport().add("/1/bundle/challenge/standalone/create_track/?consumer=SoFraud",
                    new ExpectingHttpItem(
                            new StringChecker("retpath=https%3A%2F%2Fyandex" +
                                    ".ru&uid=448130096&card_id_for_3ds=ad7c4c7dc1a39d4e4536fd07"),
                            "{\"status\": \"ok\",\"track_id\": \"abcdefghabcdefgh\"}"));

            cluster.getStorageSave().add("/update?service=so_fraud_data&prefix=1443&fraud-request-type=MAIN",
                    new ExpectingHttpItem(
                            new JsonChecker(
                                    Files.readString(resource("storage-create-request-MAIN.json"))),
                            "200"),
                    new ExpectingHttpItem(
                            new JsonChecker(
                                    Files.readString(resource("storage-create-request-on-save.json"))),
                            "200"));

            cluster.getStorageSave().add("/update?service=so_fraud_data&prefix=0&fraud-request-type=COUNTERS,AGGRS",
                    new ExpectingHttpItem(
                            new JsonChecker(
                                    Files.readString(resource("storage-create-request-1.json"))),
                            "200"));

            cluster.getStorageSave().add("/update?service=so_fraud_data&prefix=1&fraud-request-type=FAST_LIST",
                    new ExpectingHttpItem(
                            new JsonChecker(
                                    Files.readString(resource("storage-create-lists-request-1.json"))),
                            "200"),
                    new ExpectingHttpItem(
                            new JsonChecker(
                                    Files.readString(resource("storage-create-lists-request-1.json"))),
                            "200"));

            cluster.getStorageSave().add("/update?service=so_fraud_login_id&prefix=0&fraud-request-type" +
                            "=VERIFICATION_LEVEL",
                    new ExpectingHttpItem(
                            new JsonChecker(
                                    Files.readString(resource("storage-create-verification-level-request.json"))),
                            "200"));

            final String pharmaUri = "/v1/factors_by_number";
            cluster.pharma().add(pharmaUri,
                    new ExpectingHttpItem(
                            new StringChecker("phone_number=37494006699"),
                            Files.readString(
                                    resource("pharma.json"))));

            cluster.getStorageAggregation().add(
                    "/sequential/search?prefix=1443&service=so_fraud_data&json-type=dollar&IO_PRIO=0&early-interrupt" +
                            "=1&get=id,uid,card_id,ip,order_region_name,channel,sub_channel,channel_uri,terminal_id," +
                            "currency,amount,taxi_car_number,taxi_driver_license,card_isoa2,device_id,yid," +
                            "payment_method,card,txn_timestamp,txn_extid,order_id,user_phone,payed_by,payment_type," +
                            "service_payment_type,payment_mode,login_id,afs_verification_level,hbf_id,rub_amount," +
                            "mail,user_agent,order_tarif,type,transaction_type,txn_afs_action,txn_afs_tags," +
                            "txn_afs_reason," +
                            "txn_afs_queue,txn_status,txn_is_authed,txn_status_timestamp,user_context&text=" +
                            "UID_daily_id:(%22448130096_beru_payment_COMMON_PAYMENT_1593475200000%22" +
                            "+%22448130096_beru_payment_BINDING_1593475200000%22)" +
                            "&extid=5eb59fcadff13b6faf122757&fraud-request" +
                            "-type=MAIN&length=100",
                    Files.readString(resource("storage-aggregation-response.json")));

            cluster.getStorageAggregation().add(
                    "/sequential/search?prefix=1442&service=so_fraud_data&json-type=dollar&IO_PRIO=0&early-interrupt" +
                            "=1&get=id,uid,card_id,ip,order_region_name,channel,sub_channel,channel_uri,terminal_id," +
                            "currency,amount,taxi_car_number,taxi_driver_license,card_isoa2,device_id,yid," +
                            "payment_method,card,txn_timestamp,txn_extid,order_id,user_phone,payed_by,payment_type," +
                            "service_payment_type,payment_mode,login_id,afs_verification_level,hbf_id,rub_amount," +
                            "mail,user_agent,order_tarif,type,transaction_type,txn_afs_action,txn_afs_tags," +
                            "txn_afs_reason," +
                            "txn_afs_queue,txn_status,txn_is_authed,txn_status_timestamp,user_context&text=" +
                            "UID_daily_id:(%22448130096_beru_payment_COMMON_PAYMENT_1593388800000%22" +
                            "+%22448130096_beru_payment_BINDING_1593388800000%22)" +
                            "&extid" +
                            "=5eb59fcadff13b6faf122757&fraud-request-type=MAIN&length=100",
                    Files.readString(resource("storage-aggregation-empty-response.json")));

            cluster.getStorageAggregation().add(
                    "/sequential/search?prefix=1443&service=so_fraud_data&json-type=dollar&IO_PRIO=0&early-interrupt" +
                            "=1&get=id,uid,card_id,ip,order_region_name,channel,sub_channel,channel_uri,terminal_id," +
                            "currency,amount,taxi_car_number,taxi_driver_license,card_isoa2,device_id,yid," +
                            "payment_method,card,txn_timestamp,txn_extid,order_id,user_phone,payed_by,payment_type," +
                            "service_payment_type,payment_mode,login_id,afs_verification_level,hbf_id,rub_amount," +
                            "mail,user_agent,order_tarif,type,transaction_type,txn_afs_action,txn_afs_tags," +
                            "txn_afs_reason," +
                            "txn_afs_queue,txn_status,txn_is_authed,txn_status_timestamp,user_context&text=" +
                            "CARD_ID_daily_id:(%22ad7c4c7dc1a39d4e4536fd07_beru_payment_COMMON_PAYMENT_1593475200000" +
                            "%22+%22ad7c4c7dc1a39d4e4536fd07_beru_payment_BINDING_1593475200000%22)" +
                            "&extid=5eb59fcadff13b6faf122757&fraud-request" +
                            "-type=MAIN&length=100",
                    Files.readString(resource("storage-aggregation-empty-response.json")));

            cluster.getStorageAggregation().add(
                    "/sequential/search?prefix=1442&service=so_fraud_data&json-type=dollar&IO_PRIO=0&early-interrupt" +
                            "=1&get=id,uid,card_id,ip,order_region_name,channel,sub_channel,channel_uri,terminal_id," +
                            "currency,amount,taxi_car_number,taxi_driver_license,card_isoa2,device_id,yid," +
                            "payment_method,card,txn_timestamp,txn_extid,order_id,user_phone,payed_by,payment_type," +
                            "service_payment_type,payment_mode,login_id,afs_verification_level,hbf_id,rub_amount," +
                            "mail,user_agent,order_tarif,type,transaction_type,txn_afs_action,txn_afs_tags," +
                            "txn_afs_reason," +
                            "txn_afs_queue,txn_status,txn_is_authed,txn_status_timestamp,user_context&text=" +
                            "CARD_ID_daily_id:(%22ad7c4c7dc1a39d4e4536fd07_beru_payment_COMMON_PAYMENT_1593388800000" +
                            "%22+%22ad7c4c7dc1a39d4e4536fd07_beru_payment_BINDING_1593388800000%22)" +
                            "&extid" +
                            "=5eb59fcadff13b6faf122757&fraud-request-type=MAIN&length=100",
                    Files.readString(resource("storage-aggregation-empty-response.json")));

            cluster.getStorageAggregation().add(
                    "/sequential/search?prefix=1443&service=so_fraud_data&json-type=dollar&IO_PRIO=0&early-interrupt" +
                            "=1&get=id,uid,card_id,ip,order_region_name,channel,sub_channel,channel_uri,terminal_id," +
                            "currency,amount,taxi_car_number,taxi_driver_license,card_isoa2,device_id,yid," +
                            "payment_method,card,txn_timestamp,txn_extid,order_id,user_phone,payed_by,payment_type," +
                            "service_payment_type,payment_mode,login_id,afs_verification_level,hbf_id,rub_amount," +
                            "mail,user_agent,order_tarif,type,transaction_type,txn_afs_action,txn_afs_tags," +
                            "txn_afs_reason," +
                            "txn_afs_queue,txn_status,txn_is_authed,txn_status_timestamp,user_context&text=" +
                            "IP_daily_id:(%2237.252.83.39_beru_payment_COMMON_PAYMENT_1593475200000%22+%2237.252.83" +
                            ".39_beru_payment_BINDING_1593475200000%22)" +
                            "&extid=5eb59fcadff13b6faf122757&fraud-request" +
                            "-type=MAIN&length=100",
                    Files.readString(resource("storage-aggregation-empty-response.json")));

            cluster.getStorageAggregation().add(
                    "/sequential/search?prefix=1442&service=so_fraud_data&json-type=dollar&IO_PRIO=0&early-interrupt" +
                            "=1&get=id,uid,card_id,ip,order_region_name,channel,sub_channel,channel_uri,terminal_id," +
                            "currency,amount,taxi_car_number,taxi_driver_license,card_isoa2,device_id,yid," +
                            "payment_method,card,txn_timestamp,txn_extid,order_id,user_phone,payed_by,payment_type," +
                            "service_payment_type,payment_mode,login_id,afs_verification_level,hbf_id,rub_amount," +
                            "mail,user_agent,order_tarif,type,transaction_type,txn_afs_action,txn_afs_tags," +
                            "txn_afs_reason," +
                            "txn_afs_queue,txn_status,txn_is_authed,txn_status_timestamp,user_context&text=" +
                            "IP_daily_id:(%2237.252.83.39_beru_payment_COMMON_PAYMENT_1593388800000%22+%2237.252.83" +
                            ".39_beru_payment_BINDING_1593388800000%22)" +
                            "&extid" +
                            "=5eb59fcadff13b6faf122757&fraud-request-type=MAIN&length=100",
                    Files.readString(resource("storage-aggregation-empty-response.json")));

            cluster.getStorageAggregation().add(
                    "/sequential/search?prefix=1443&service=so_fraud_data&json-type=dollar&IO_PRIO=0&early-interrupt" +
                            "=1&get=id,uid,card_id,ip,order_region_name,channel,sub_channel,channel_uri,terminal_id," +
                            "currency,amount,taxi_car_number,taxi_driver_license,card_isoa2,device_id,yid," +
                            "payment_method,card,txn_timestamp,txn_extid,order_id,user_phone,payed_by,payment_type," +
                            "service_payment_type,payment_mode,login_id,afs_verification_level,hbf_id,rub_amount," +
                            "mail,user_agent,order_tarif,type,transaction_type,txn_afs_action,txn_afs_tags," +
                            "txn_afs_reason," +
                            "txn_afs_queue,txn_status,txn_is_authed,txn_status_timestamp,user_context&text=" +
                            "USER_PHONE_daily_id:(%2237494006699_beru_payment_COMMON_PAYMENT_1593475200000%22" +
                            "+%2237494006699_beru_payment_BINDING_1593475200000%22)" +
                            "&extid=5eb59fcadff13b6faf122757&fraud-request" +
                            "-type=MAIN&length=100",
                    Files.readString(resource("storage-aggregation-empty-response.json")));

            cluster.getStorageAggregation().add(
                    "/sequential/search?prefix=1442&service=so_fraud_data&json-type=dollar&IO_PRIO=0&early-interrupt" +
                            "=1&get=id,uid,card_id,ip,order_region_name,channel,sub_channel,channel_uri,terminal_id," +
                            "currency,amount,taxi_car_number,taxi_driver_license,card_isoa2,device_id,yid," +
                            "payment_method,card,txn_timestamp,txn_extid,order_id,user_phone,payed_by,payment_type," +
                            "service_payment_type,payment_mode,login_id,afs_verification_level,hbf_id,rub_amount," +
                            "mail,user_agent,order_tarif,type,transaction_type,txn_afs_action,txn_afs_tags," +
                            "txn_afs_reason," +
                            "txn_afs_queue,txn_status,txn_is_authed,txn_status_timestamp,user_context&text=" +
                            "USER_PHONE_daily_id:(%2237494006699_beru_payment_COMMON_PAYMENT_1593388800000%22" +
                            "+%2237494006699_beru_payment_BINDING_1593388800000%22)" +
                            "&extid" +
                            "=5eb59fcadff13b6faf122757&fraud-request-type=MAIN&length=100",
                    Files.readString(resource("storage-aggregation-empty-response.json")));

            final String searchCollapsedAggrsUri = "/sequential/search?prefix=1&service=so_fraud_data&json-type" +
                    "=dollar&IO_PRIO=0&early-interrupt=1&get=*&text=" +
                    "(key_value:\"uid_448130096\" AND txn_day:[1588352181134 TO 1593449781134]) AND " +
                    "channel_uri:\"beru_payment\" AND type:\"COLLAPSED_AGGRS\"" +
                    "&extid=5eb59fcadff13b6faf122757" +
                    "&ranges-over-boolean=1" +
                    "&fraud-request-type=COLLAPSED_AGGRS&length=59";
            cluster.getStorageAggregation().add(searchCollapsedAggrsUri,
                    Files.readString(resource("collapsed_aggrs.json")));

            final String fastListsPreparedSoFraudPassportQuery = "/sequential/search?prefix=1&service=so_fraud_data" +
                    "&json-type=dollar&IO_PRIO=0&early-interrupt=1&get=*&text=id:" +
                    "(\"txn_beru_payment_BLACK_UIDS_1120000000036393\" " +
                    "\"txn_beru_payment_PREPARED_FAST_LIST_1120000000036393\" " +
                    "\"txn_beru_payment_PREPARED_FAST_LIST_123\" \"txn_beru_payment_PREPARED_FAST_LIST_123\")" +
                    "&fraud-request-type=FAST_LIST&length=4";
            cluster.getStorageAggregation().add(fastListsPreparedSoFraudPassportQuery,
                    Files.readString(resource("prepared-fast-lists-response.json")));

            final String searchVerificationLevelsQuery = "/sequential/search?prefix=0&service=so_fraud_login_id&json" +
                    "-type" +
                    "=dollar&IO_PRIO=0&early-interrupt=1&get=*&text=login_id:\"qwertyui\" " +
                    "uid:\"448130096\"" +
                    "&postfilter=type == VERIFICATION_LEVEL" +
                    "&fraud-request-type=VERIFICATION_LEVEL" +
                    "&length=100";
            cluster.getStorageAggregation().add(searchVerificationLevelsQuery,
                    Files.readString(resource("storage-verification-response.json")));

            final String searchPreparedCountersUri = "/sequential/search?prefix=0&service=so_fraud_data&json-type" +
                    "=dollar" +
                    "&IO_PRIO=0&early-interrupt=1&get=*&text=id:(\"txn_beru_payment_user_phone_37494006699_counters\"" +
                    " \"txn_beru_payment_ip_37.252.83.39_counters\" " +
                    "\"txn_beru_payment_card_id_ad7c4c7dc1a39d4e4536fd07_counters\" " +
                    "\"txn_beru_payment_uid_448130096_counters\" \"txn_beru_payment_some_key_value_counters\" " +
                    "\"txn_beru_payment_user_agent_Mozilla\\/5.0\\ \\(Macintosh;\\ Intel\\ Mac\\ OS\\ X\\ 10_15_7\\)" +
                    "\\ AppleWebKit\\/537.36\\ \\(KHTML,\\ like\\ Gecko\\)\\ Chrome\\/90.0.4430.41\\ YaBrowser\\/21.5" +
                    ".0.751\\ Yowser\\/2.5\\ Safari\\/537.36_counters\")" +
                    "&fraud-request-type=COUNTERS&length=100";
            cluster.getStorageAggregation().add(searchPreparedCountersUri,
                    Files.readString(resource("storage-prepared-counters-response.json")));

            cluster.getRbl().add("/check?info=geobase&service=antifraud&ip=37.252.83.39",
                    Files.readString(resource("rbl-response.json")));

            cluster.trust().add("/legacy/wallet-balance?uid=448130096",
                    Files.readString(resource("trust-response.json")));

            cluster.blackbox().add(
                    "/blackbox/?phone_attributes=102,104,106,108&method=userinfo&getphones=bound&aliases=1,2,3,5,6,7," +
                            "8,9,10,11,12,13,15,16,17,18,19,20,21,22&uid=448130096&attributes=1,31,107,110,132,200," +
                            "1003,1015&userip=127.0.0.1&format=json&sid=2",
                    new ExpectingHeaderHttpItem(
                            new StaticHttpItem(
                                    Files.readString(
                                            resource("some-bb-response.json"))),
                            YandexHeaders.X_YA_SERVICE_TICKET,
                            Cluster.BLACKBOX_TVM_TICKET));

            cluster.familypayCluster().prepareDatabase();

            cluster.start();

            {
                HttpPost post = new HttpPost(cluster.server().host() + "/scoring");
                post.setEntity(new StringEntity(Files.readString(resource("cybertonica-request-so-only-by-us.json")),
                        ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new JsonChecker(Files.readString(resource("our-response.json"))),
                            CharsetUtils.toString(response.getEntity()));
                }
            }
            {
                final HttpGet post = new HttpGet(cluster.server().host() + "/reload_rules");

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                }
            }

            {
                HttpPost post = new HttpPost(cluster.server().host() + "/save_transaction");
                post.setEntity(new StringEntity(Files.readString(resource("update-request.json")),
                        ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                            new StringChecker("OK"),
                            CharsetUtils.toString(response.getEntity()));
                }
            }
            Assert.assertEquals(
                    1,
                    cluster.pharma().accessCount(pharmaUri));
            Assert.assertEquals(
                    1,
                    cluster.getStorageAggregation().accessCount(fastListsPreparedSoFraudPassportQuery));
            Assert.assertEquals(
                    1,
                    cluster.getStorageAggregation().accessCount(searchPreparedCountersUri));
            Assert.assertEquals(
                    1,
                    cluster.getStorageAggregation().accessCount(searchCollapsedAggrsUri));
        }
    }

    @Test
    public void testUpdateRequestParsing() throws Exception {
        {
            final JsonObject data =
                    TypesafeValueContentHandler.parse(Files.readString(resource("update-request.json")));
            final ScoringData request = new ScoringData(data.asMap(), Amount::amount);

            Assert.assertEquals("Successful payment", request.getValue(Field.COMMENT).value());
            Assert.assertEquals("success", request.getValue(Field.CODE).value());
            Assert.assertEquals(0, request.getValue(Field.AUTHED).longValue());
            Assert.assertEquals(TransactionStatus.OK,
                    TransactionStatus.valueOf(request.getValue(Field.STATUS).value()));
            Assert.assertEquals("5eb59fcadff13b6faf122757", request.getExternalId());
            Assert.assertEquals("eve_acquiring:84b4e875-dd24-414c-a860-1851bfc60716",
                    request.getValue(Field.TX_ID).value());
            Assert.assertEquals("beru", request.getChannel());
        }
    }

    @Test
    public void testLogBrokerConsumer() throws Exception {
        try (Cluster cluster = new Cluster(this, resource("channels.conf").toString());
             CloseableHttpClient client = Configs.createDefaultClient()) {

            final String updateUri = "/update?" +
                    "service=so_fraud_data&" +
                    "prefix=1613&" +
                    "fraud-request-type=MAIN";

            cluster.getStorageSave().add(updateUri,
                    new ExpectingHttpItem(
                            new JsonChecker(
                                    Files.readString(resource("lb-update-data.json"))),
                            "123"));

            cluster.start();

            final String uri = "/passport-logbroke-it";
            final HttpPost post = new HttpPost(cluster.server().host() + uri);

            final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setContentType(ContentType.parse("multipart/mixed"));
            builder.addTextBody("part1", Files.readString(resource("passport-lb-data.txt")));
            post.setEntity(builder.build());

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                        new JsonChecker(Files.readString(resource("lb-response.json"))),
                        CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testLists() throws Exception {
        try (Cluster cluster = new Cluster(this, resource("channels.conf").toString());
             CloseableHttpClient client = Configs.createDefaultClient()) {

            final String updateUri = "/update?" +
                    "service=so_fraud_data" +
                    "&prefix=1" +
                    "&fraud-request-type=FAST_LIST";

            cluster.getStorageSave().add(updateUri,
                    new ExpectingHttpItem(
                            new JsonChecker(
                                    Files.readString(resource("list-storage-request.json"))),
                            "123"));

            final String deleteUri = "/delete?prefix=1&service=so_fraud_data" +
                    "&text=id:\"txn_taxi_test_12345678\"&fraud-request-type=FAST_LIST";
            cluster.getStorageSave().add(deleteUri, new ExpectingHeaderHttpItem(
                    new StaticHttpItem("123")));

            cluster.start();

            {
                HttpPost post = new HttpPost(cluster.server().host() + "/update_list");
                post.setEntity(new StringEntity(Files.readString(resource("list-request.json")),
                        ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals("123", CharsetUtils.toString(response.getEntity()));
                }
                Assert.assertEquals(
                        1,
                        cluster.getStorageSave().accessCount(updateUri));
            }

            {
                HttpPost post = new HttpPost(cluster.server().host() + "/delete_list");
                post.setEntity(new StringEntity(Files.readString(resource("list-delete-request.json")),
                        ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = client.execute(post)) {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    Assert.assertEquals("123", CharsetUtils.toString(response.getEntity()));
                }
                Assert.assertEquals(
                        1,
                        cluster.getStorageSave().accessCount(deleteUri));
            }
        }
    }
}

