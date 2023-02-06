package ru.yandex.passport;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.BasicGenericConsumer;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.parser.JsonParser;
import ru.yandex.json.parser.StackContentHandler;
import ru.yandex.passport.address.AddressId;
import ru.yandex.passport.address.AddressService;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AddressCrudTest extends TestBase {

    @Test
    public void testCrudAddressWithDistrict() throws Exception {
        try (AddressProxyCluster cluster = new AddressProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.homeWorkDataSync().add("/v2/227356512/personality/profile/addresses*", "{\"items\":[]}");
            cluster.deliveryDataSync().add("/v1/227356512/personality/profile/market/delivery_addresses*", "{\"items\":[]}");
            cluster.geocoder().add(
                "/yandsearch*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("geocoder_save_district.response"))));

            HttpPost create = new HttpPost(
                cluster.proxy().host().toString() + "/address/create?user_id=227356512&user_type=uid");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            create.setEntity(
                new StringEntity(
                    "{\n" +
                        "  \"building\": 38,\n" +
                        "  \"country\": \"Россия\",\n" +
                        "  \"locality\": \"Москва\",\n" +
                        "  \"district\": \"микрорайон 1 мая\",\n" +
                        "  \"locale\": \"en\"\n" +
                        "}\n"));

            String expAddress =
                "\"id\":\"<any value>" +
                    "\",\"owner_service\":\"passport\",\"type\":\"address\"," +
                    "\"subtype\":\"address\",\"building\":\"38\",\"comment\":null," +
                    "\"country\":\"Россия\"," +
                    "\"creation_time\":\"<any value>\",\"format_version\":null,\"geocoder_description\":null," +
                    "\"district\":\"микрорайон 1 Мая\",\"entrance\":null,\"floor\":null,\"intercom\":null," +
                    "\"city\":\"Балашиха\",\"locality\":\"Балашиха\"," +
                    "\"address_line\":\"<any value>\"," +
                    "\"address_type\":\"address\",\"brand_name\":null,\"comment_courier\":null," +
                    "\"geocoder_exact\":null," +
                    "\"geocoder_name\":null,\"geocoder_object_type\":null,\"label\":null,\"last_touched_time\":null," +
                    "\"locale\":\"en\",\"modification_time\":\"<any value>\",\"name\":null,\"org_id\":null," +
                    "\"phone_id\":null,\"uri\":null,\"version\":0,\"is_portal_uid\": false, \"draft\": false," +
                    "\"location\":{\"latitude\":55.80213309816211,\"longitude\":37.84706496568502}," +
                    "\"regionId\":10716,\"room\":null,\"street\":null,\"zip\":\"\",\"region\": \"Москва и Московская " +
                    "область\"";
            HttpAssert.assertJsonResponse(
                client,
                create,
                "{\"status\":\"ok\"," + expAddress + "}");

            HttpGet get = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/list?user_id=227356512&user_type=uid&locale=en");
            get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"status\":\"ok\", \"addresses\": [{" + expAddress
                    + ",\"user_id\":\"227356512\",\"user_type\":\"uid\"}],"
                    + "\"more\":false}");
        }
    }

    @Test
    public void testCrudAddress() throws Exception {
        try (AddressProxyCluster cluster = new AddressProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.homeWorkDataSync().add("/v2/227356512/personality/profile/addresses*", "{\"items\":[]}");
            cluster.deliveryDataSync().add("/v1/227356512/personality/profile/market/delivery_addresses*", "{\"items\":[]}");
            cluster.geocoder().add(
                "/yandsearch*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("geocoder.response"))));

            HttpPost create = new HttpPost(
                cluster.proxy().host().toString() + "/address/create?user_id=227356512&user_type=uid");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            create.setEntity(
                new StringEntity(
                    "{\n" +
                        "  \"building\": 16,\n" +
                        "  \"country\": \"Россия\",\n" +
                        "  \"locality\": \"Москва\",\n" +
                        "  \"street\": \"Льва Толстого\",\n" +
                        "  \"locale\": \"en\"\n" +
                        "}\n"));

            String expAddress =
                "\"id\":\"<any value>" +
                    "\",\"owner_service\":\"passport\",\"type\":\"address\"," +
                    "\"subtype\":\"address\",\"building\":\"16\",\"comment\":null," +
                    "\"country\":\"Киргизия\"," +
                    "\"district\":null,\"entrance\":null,\"floor\":null,\"intercom\":null," +
                    "\"city\":\"Бишкек\",\"locality\":\"Бишкек\"," +
                    //"\"address_line\":\"Киргизия, Бишкек, улица Льва Толстого, 16\"," +
                    "\"address_line\":\"<any value>\"," +
                    "\"address_type\":\"address\",\"brand_name\":null,\"comment_courier\":null," +
                    "\"creation_time\":\"<any value>\",\"format_version\":null,\"geocoder_description\":null," +
                    "\"geocoder_exact\":null," +
                    "\"geocoder_name\":null,\"geocoder_object_type\":null,\"label\":null,\"last_touched_time\":null," +
                    "\"locale\":\"en\",\"modification_time\":\"<any value>\",\"name\":null,\"org_id\":null," +
                    "\"phone_id\":null,\"uri\":null,\"version\":0,\"is_portal_uid\": false, \"draft\": false," +
                    "\"location\":{\"latitude\":42.86227172910146,\"longitude\":74.60624317284278}," +
                    "\"regionId\":10309,\"room\":null,\"street\":\"улица Льва Толстого\",\"zip\":\"\",\"region\": null";
            HttpAssert.assertJsonResponse(
                client,
                create,
                "{\"status\":\"ok\"," + expAddress + "}");

            HttpGet get = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/list?user_id=227356512&user_type=uid&locale=en");
            get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"status\":\"ok\", \"addresses\": [{" + expAddress
                    + ",\"user_id\":\"227356512\",\"user_type\":\"uid\"}],"
                    + "\"more\":false}");
        }
    }

    @Test
    public void testId() throws Exception {
        String hexedId = AddressId.makeOuterId( "uid", 227356512L, AddressService.PASSPORT, UUID.randomUUID().toString(), false);
        AddressId addressId = AddressId.parseOuterId(hexedId);
        Assert.assertEquals(addressId.userId(), 227356512L);
        Assert.assertEquals(addressId.userType(), "uid");
        Assert.assertEquals(addressId.ownerService().serviceName(), "passport");
    }

    @Test
    public void testTaxi() throws Exception {
        try (AddressProxyCluster cluster = new AddressProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.geocoder().add(
                "/yandsearch*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("geocoder.response"))));

            HttpPost create = new HttpPost(
                cluster.proxy().host().toString()
                    + "/address/create?user_type=uid&user_id=4080171168&id=6abdbea4f93848f9a83529bb4f30665d&draft=true");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            create.setEntity(
                new StringEntity(
                    "{\"user_type\":\"uid\",\"user_id\":\"4080171168\",\"draft\":true," +
                        "\"id\":\"6abdbea4f93848f9a83529bb4f30665d\"}"));

            String expAddressWithComment = "\"id\":\"6abdbea4f93848f9a83529bb4f30665d\"," +
                "\"owner_service\":\"taxi\",\"type\":\"address\",\"subtype\":\"address\"," +
                "\"building\":null,\"comment\":\"comment 1\",\"country\":null,\"district\":null," +
                "\"entrance\":\"2\",\"floor\":null,\"intercom\":null,\"city\":null," +
                "\"locality\":null,\"location\":{\"latitude\":74.60624317284278,\"longitude\":42" +
                ".86227172910146},\"regionId\":null,\"room\":null,\"street\":null,\"zip\":null," +
                "\"address_type\":\"address\",\"last_touched_time\":null,\"address_line\":null," +
                "\"locale\":\"ru\",\"version\":0,\"format_version\":null,\"name\":null,\"region\":null," +
                "\"uri\":null,\"label\":null,\"brand_name\":null,\"phone_id\":null," +
                "\"geocoder_name\":null,\"geocoder_description\":null,\"is_portal_uid\": false, \"draft\": false," +
                "\"geocoder_object_type\":null,\"geocoder_exact\":null,\"comment_courier\":null," +
                "\"org_id\":null,\"modification_time\":\"<any value>\"," +
                "\"creation_time\":\"<any value>\"";
            HttpAssert.assertJsonResponse(
                client,
                create,
                "{\"status\":\"ok\", \"id\":\"6abdbea4f93848f9a83529bb4f30665d\", \"version\":0}");

            HttpPost update = new HttpPost(
                cluster.proxy().host().toString()
                    + "/address/update?user_type=uid&user_id=4080171168");
            update.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            update.setEntity(
                new StringEntity(
                    "{\"addresses\":[{\"user_type\":\"uid\",\"user_id\":\"4080171168\",\"draft\":true," +
                        "\"comment\":\"comment 1\",\"entrance\":\"2\"," +
                        "\"location\":{\"latitude\":74.60624317284278,\"longitude\":42.86227172910146}," +
                        "\"id\":\"6abdbea4f93848f9a83529bb4f30665d\"}]}"));

            HttpAssert.assertJsonResponse(
                client,
                update,
                "{\"status\":\"ok\", \"addresses\":[{" + expAddressWithComment + "}]}");

            create = new HttpPost(
                cluster.proxy().host().toString()
                    + "/address/create?user_type=uid&user_id=4080171168&id=home&draft=true");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            create.setEntity(
                new StringEntity(
                    "{\"user_type\":\"uid\",\"user_id\":\"4080171168\",\"draft\":true," +
                        "\"id\":\"home\"}"));

            HttpAssert.assertJsonResponse(
                client,
                create,
                "{\"status\":\"ok\", \"id\":\"home\", \"version\":0}");

            update = new HttpPost(
                cluster.proxy().host().toString()
                    + "/address/update?user_type=uid&user_id=4080171168");
            update.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            update.setEntity(
                new StringEntity(
                    "{\"addresses\":[{\"user_type\":\"uid\",\"user_id\":\"4080171168\",\"draft\":true," +
                        "\"location\":{\"latitude\":74.60624317284278,\"longitude\":42.86227172910146}," +
                        "\"id\":\"home\"}]}"));

            String expAddressHome = "\"id\":\"home\"," +
                "\"owner_service\":\"taxi\",\"type\":\"address\",\"subtype\":\"address\"," +
                "\"building\":null,\"comment\":null,\"country\":null,\"district\":null," +
                "\"entrance\":null,\"floor\":null,\"intercom\":null,\"city\":null," +
                "\"locality\":null,\"location\":{\"latitude\":74.60624317284278,\"longitude\":42.86227172910146}," +
                "\"regionId\":null,\"room\":null,\"street\":null,\"zip\":null," +
                "\"address_type\":\"address\",\"last_touched_time\":null,\"address_line\":null," +
                "\"locale\":\"ru\",\"version\":0,\"format_version\":null,\"name\":null,\"region\":null," +
                "\"uri\":null,\"label\":null,\"brand_name\":null,\"phone_id\":null," +
                "\"geocoder_name\":null,\"geocoder_description\":null,\"is_portal_uid\": false, \"draft\": false," +
                "\"geocoder_object_type\":null,\"geocoder_exact\":null,\"comment_courier\":null," +
                "\"org_id\":null,\"modification_time\":\"<any value>\"," +
                "\"creation_time\":\"<any value>\"";
            HttpAssert.assertJsonResponse(
                client,
                update,
                "{\"status\":\"ok\", \"addresses\":[{" + expAddressHome + "}]}");
        }
    }

    @Ignore
    @Test
    public void testTaxiMigration2() throws Exception {
//        Field field = Paths.class.getDeclaredField("sourceRoot");
//        field.setAccessible(true);
//        field.set(null, "/home/vonidu/Projects/PS/repo/arc2/arcadia");
//        field = Paths.class.getDeclaredField("sandboxResourcesRoot");
//        field.setAccessible(true);
//        field.set(null, "/home/vonidu/Downloads");
//
//        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
//        builder.appendPattern("uuuu-MM-dd'T'HH:mm:ss");
//        builder.appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true);
//        builder.appendOffset("+HHmm", "+0000");
//        OffsetDateTime.parse("2021-12-17T03:54:47.608+0300", builder.toFormatter());
//        OffsetDateTime.parse("2021-12-17T03:54:47+0300", builder.toFormatter());
        //System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZ").format(ZonedDateTime.now()));
        //Instant.parse("2021-12-17T03:54:47.608+0300").atOffset(ZoneOffset.UTC);
        //ZonedDateTime.parse("2021-12-14T07:56:09.242+0300", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZZZ"));
        //ZonedDateTime.parse("2021-12-17T03:54:47.608+0300", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss.SSSX"));
        try (AddressProxyCluster cluster = new AddressProxyCluster(this)) {
            HttpPost upsert = new HttpPost(
                cluster.proxy().host().toString()
                    + "/address/update?user_type=uid&insert=true");
            upsert.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            upsert.setEntity(
                new StringEntity(
                    Files.readString(new File("/home/vonidu/Downloads/taxi.json").toPath())));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, upsert);
        }
    }

    @Test
    public void testTaxiMigration() throws Exception {
        try (AddressProxyCluster cluster = new AddressProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.geocoder().add(
                "/yandsearch*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("geocoder.response"))));

            HttpPost upsert = new HttpPost(
                cluster.proxy().host().toString()
                    + "/address/update?user_type=uid&user_id=4080171168&insert=true");
            upsert.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            upsert.setEntity(
                new StringEntity(
                    "{\"addresses\":[{\"user_type\":\"uid\",\"user_id\":\"4080171168\"," +
                        "\"entrance\":\"10\", \"street\":\"Tolstogo\"," +
                        "\"location\":{\"latitude\":74.60624317284278,\"longitude\":42.86227172910146}," +
                        "\"id\":\"6abdbea4f93848f9a83529bb4f30665d\"}]}"));
            // migration call - update?insert=true
            String expAddressUpsert1 =
                "\"id\":\"6abdbea4f93848f9a83529bb4f30665d\"," +
                    "\"owner_service\":\"taxi\",\"type\":\"address\",\"subtype\":\"address\"," +
                    "\"building\":null,\"comment\":null,\"country\":null,\"district\":null," +
                    "\"entrance\":\"10\",\"floor\":null,\"intercom\":null,\"city\":null," +
                    "\"locality\":null,\"location\":{\"latitude\":74.60624317284278,\"longitude\":42" +
                    ".86227172910146},\"regionId\":null,\"room\":null,\"street\":\"Tolstogo\",\"zip\":null," +
                    "\"address_type\":\"address\",\"last_touched_time\":null,\"address_line\":null," +
                    "\"locale\":\"ru\",\"version\":0,\"format_version\":null,\"name\":null,\"region\":null," +
                    "\"uri\":null,\"label\":null,\"brand_name\":null,\"phone_id\":null," +
                    "\"geocoder_name\":null,\"geocoder_description\":null,\"is_portal_uid\": false, \"draft\": false," +
                    "\"geocoder_object_type\":null,\"geocoder_exact\":null,\"comment_courier\":null," +
                    "\"org_id\":null,\"modification_time\":\"<any value>\"," +
                    "\"creation_time\":\"<any value>\"";
            HttpAssert.assertJsonResponse(
                client,
                upsert,
                "{\"status\":\"ok\", \"addresses\":[{" + expAddressUpsert1 + "}]}");


            String expAddressUpsert2 =
                "\"id\":\"6abdbea4f93848f9a83529bb4f30665d\"," +
                    "\"owner_service\":\"taxi\",\"type\":\"address\",\"subtype\":\"address\"," +
                    "\"building\":\"5\",\"comment\":null,\"country\":null,\"district\":null," +
                    "\"entrance\":\"1\",\"floor\":null,\"intercom\":null,\"city\":null," +
                    "\"locality\":null,\"location\":{\"latitude\":74.60624317284278,\"longitude\":42" +
                    ".86227172910146},\"regionId\":null,\"room\":null,\"street\":null,\"zip\":null," +
                    "\"address_type\":\"address\",\"last_touched_time\":null,\"address_line\":null," +
                    "\"locale\":\"ru\",\"version\":0,\"format_version\":null,\"name\":null,\"region\":null," +
                    "\"uri\":null,\"label\":null,\"brand_name\":null,\"phone_id\":null," +
                    "\"geocoder_name\":null,\"geocoder_description\":null,\"is_portal_uid\": false, \"draft\": false," +
                    "\"geocoder_object_type\":null,\"geocoder_exact\":null,\"comment_courier\":null," +
                    "\"org_id\":null,\"modification_time\":\"<any value>\"," +
                    "\"creation_time\":\"<any value>\"";
            upsert = new HttpPost(
                cluster.proxy().host().toString()
                    + "/address/update?user_type=uid&user_id=4080171168&insert=true");
            upsert.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            upsert.setEntity(
                new StringEntity(
                    "{\"addresses\":[{\"user_type\":\"uid\",\"user_id\":\"4080171168\"," +
                        "\"entrance\":\"1\", \"building\": \"5\"," +
                        "\"location\":{\"latitude\":74.60624317284278,\"longitude\":42.86227172910146}," +
                        "\"id\":\"6abdbea4f93848f9a83529bb4f30665d\"}]}"));
            HttpAssert.assertJsonResponse(
                client,
                upsert,
                "{\"status\":\"ok\", \"addresses\":[{" + expAddressUpsert2 + "}]}");
        }
    }

    @Test
    public void testHiddenByPay() throws Exception {
        try (AddressProxyCluster cluster = new AddressProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.homeWorkDataSync().add("/v2/227356512/personality/profile/addresses*", "{\"items\":[]}");
            cluster.deliveryDataSync().add("/v1/227356512/personality/profile/market/delivery_addresses*", "{\"items\":[]}");
            cluster.geocoder().add(
                "/yandsearch*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("geocoder.response"))));

            HttpPost create = new HttpPost(
                cluster.proxy().host().toString() + "/address/create?user_id=227356512&user_type=uid");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            create.setEntity(
                new StringEntity(
                    "{\n" +
                        "  \"building\": 16,\n" +
                        "  \"country\": \"Россия\",\n" +
                        "  \"locality\": \"Москва\",\n" +
                        "  \"street\": \"Льва Толстого\",\n" +
                        "  \"locale\": \"ru\"\n" +
                        "}\n"));

            String addressId;
            try (CloseableHttpResponse response = client.execute(create)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr = CharsetUtils.toString(response.getEntity());
                JsonObject result = TypesafeValueContentHandler.parse(responseStr);
                addressId = result.asMap().getString("id");
            }

            HttpGet list = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/list?user_id=227356512&user_type=uid");

            list.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PAY_PASSP);
            try (CloseableHttpResponse response = client.execute(list)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr = CharsetUtils.toString(response.getEntity());
                JsonList addreses = TypesafeValueContentHandler.parse(responseStr).asMap().getList("addresses");
                Assert.assertEquals(1, addreses.size());
                Assert.assertEquals(addreses.get(0).asMap().getString("id"), addressId);
            }

            HttpGet delete = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/delete?user_id=227356512&user_type=uid&id=" + addressId);
            delete.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PAY_PASSP);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, delete);

            try (CloseableHttpResponse response = client.execute(list)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr = CharsetUtils.toString(response.getEntity());
                JsonList addreses = TypesafeValueContentHandler.parse(responseStr).asMap().getList("addresses");
                Assert.assertEquals(0, addreses.size());
            }
        }
    }

    private static String geocoderRequest(String text, String lang) {
        return String.format("/yandsearch?origin=market-loyalty&tvm=1&text=%s&lang=%s&ms=pb&type=geo", text, lang);
    }

    private static String geocoderRequest(String text, String lang, double latitude, double longitude)  {
        return geocoderRequest(text, lang) + "&ll=" + latitude + "," + longitude;
    }

    @Test
    public void testGetAddressConversion() throws Exception {
        try (AddressProxyCluster cluster = new AddressProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.deliveryDataSync().add("/v2/4080171168/personality/profile/addresses", "{\"items\":[]}");
            // Москва, Льва Толстого, 16
            cluster.geocoder().add(
                geocoderRequest(
                    "%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C+%D0%9B%D1%8C%D0%B2%D0%B0+" +
                        "%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2C+16",
                    "ru-RU"
                ),
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("convert_address_ru_geocoder.response"))));

            cluster.geocoder().add(
                geocoderRequest(
                    "%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C+%D0%9B%D1%8C%D0%B2%D0%B0+" +
                        "%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2C+16",
                    "ru-RU",
                    37.587093,
                    55.733974
                ),
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("convert_address_ru_geocoder.response"))));

            // Россия, 213, Москва, улица Льва Толстого, 16
            cluster.geocoder().add(
                geocoderRequest(
                    "119021%2C+%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C+213%2C" +
                        "+%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C+%D1%83%D0%BB%D0%B8%D1%86%D0%B0" +
                        "+%D0%9B%D1%8C%D0%B2%D0%B0+%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2C+16",
                    "en-US"
                ),
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("convert_address_en_geocoder.response"))));

            cluster.geocoder().add(
                geocoderRequest(
                    "119021%2C+%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C+213%2C" +
                        "+%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C+%D1%83%D0%BB%D0%B8%D1%86%D0%B0" +
                        "+%D0%9B%D1%8C%D0%B2%D0%B0+%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2C+16",
                    "en-US",
                    37.587093,
                    55.733974
                ),
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("convert_address_en_geocoder.response"))));


            // create ru address

            HttpPost create = new HttpPost(
                cluster.proxy().host().toString()
                    + "/address/create?user_type=uid&user_id=4080171168");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            create.setEntity(
                new StringEntity(
                    "{\"user_type\":\"uid\",\"user_id\":\"4080171168\"," +
                        "\"address_line\":\"Москва, Льва Толстого, 16\"}",
                    Charset.defaultCharset()));

            String expAddress = "\"id\":\"<any value>\"," +
                "\"owner_service\":\"passport\",\"type\":\"address\",\"subtype\":\"address\"," +
                "\"building\":\"16\",\"comment\":null,\"country\":\"Россия\",\"district\":null," +
                "\"entrance\":null,\"floor\":null,\"intercom\":null,\"city\":\"Москва\"," +
                "\"locality\":\"Москва\",\"location\":{\"latitude\":55.73397404565889,\"longitude\":37.587092522460836}," +
                "\"regionId\":213,\"room\":null,\"street\":\"улица Льва Толстого\",\"zip\":\"119021\"," +
                "\"address_type\":\"address\",\"last_touched_time\":null," +
                "\"locale\":\"ru\",\"version\":0,\"format_version\":null,\"name\":null," +
                "\"uri\":null,\"label\":null,\"brand_name\":null,\"phone_id\":null," +
                "\"geocoder_name\":null,\"geocoder_description\":null,\"is_portal_uid\": false, \"draft\": false," +
                "\"geocoder_object_type\":null,\"geocoder_exact\":null,\"comment_courier\":null," +
                "\"org_id\":null,\"modification_time\":\"<any value>\",\"region\":\"Москва и Московская область\"," +
                "\"creation_time\":\"<any value>\"";

            String id;

            try (CloseableHttpResponse response = client.execute(create)) {
                String responseStr =
                    CharsetUtils.toString(response.getEntity());

                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(new JsonChecker(
                    "{\"status\":\"ok\", \"version\":0," + expAddress + ",\"address_line\":\"Россия, Москва, улица Льва Толстого, 16\"}"),
                    responseStr);

                JsonObject responseObj = new SimpleJsonParser().parse(responseStr);
                id = responseObj.asMap().get("id").asStringOrNull();
            }


            // get ru address
            // TODO fix this kostyl
            expAddress += ",\"address_line\":null";
            HttpGet get = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/get?user_id=4080171168&user_type=uid&locale=ru&id=" + id);
            get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"status\":\"ok\"," + expAddress + "}");


            // get en address without convert

            HttpGet getEn = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/get?user_id=4080171168&user_type=uid&locale=en&id=" + id);
            getEn.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, client, getEn);


            // get en address with convert
            // TODO fix region name other locale
            String expAddressEn = "\"id\":\"<any value>\"," +
                "\"owner_service\":\"passport\",\"type\":\"address\",\"subtype\":\"address\"," +
                "\"building\":\"16\",\"comment\":null,\"country\":\"Russia\",\"district\":null," +
                "\"entrance\":null,\"floor\":null,\"intercom\":null,\"city\":\"Moscow\"," +
                "\"locality\":\"Moscow\",\"location\":{\"latitude\":55.73397404565889,\"longitude\":37.587092522460836}," +
                "\"regionId\":213,\"room\":null,\"street\":\"Lva Tolstogo Street\",\"zip\":\"119021\"," +
                "\"address_type\":\"address\",\"last_touched_time\":null," +
                "\"locale\":\"en\",\"version\":0,\"format_version\":null,\"name\":null," +
                "\"uri\":null,\"label\":null,\"brand_name\":null,\"phone_id\":null," +
                "\"geocoder_name\":null,\"geocoder_description\":null,\"is_portal_uid\": false, \"draft\": false," +
                "\"geocoder_object_type\":null,\"geocoder_exact\":null,\"comment_courier\":null," +
                "\"org_id\":null,\"modification_time\":\"<any value>\",\"address_line\": null," +
                "\"creation_time\":\"<any value>\", \"region\": \"Москва и Московская область\"";

            HttpGet getEnConvert = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/get?user_id=4080171168&user_type=uid&locale=en&convert=true&id=" + id);
            getEnConvert.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);

            String enId;

            try (CloseableHttpResponse response = client.execute(getEnConvert)) {
                String responseStr =
                    CharsetUtils.toString(response.getEntity());

                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(new JsonChecker(
                    "{\"status\":\"ok\"," + expAddressEn + "}"),
                    responseStr);

                JsonObject responseObj = new SimpleJsonParser().parse(responseStr);
                enId = responseObj.asMap().get("id").asStringOrNull();
            }

            HttpGet getEn2 = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/get?user_id=4080171168&user_type=uid&locale=en&id=" + enId);
            getEn2.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                getEn2,
                "{\"status\":\"ok\"," + expAddressEn + "}");


            // update, en address will be deleted

            cluster.geocoder().add(
                "/yandsearch?origin=market-loyalty&tvm=1" +
                    "&text=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C+%D0%9B%D1%8C%D0%B2%D0%B0+" +
                    "%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2C+16" +
                    "&lang=ru-RU&ms=pb&type=geo",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("convert_address_ru_geocoder.response"))));

            HttpPost update = new HttpPost(
                cluster.proxy().host().toString()
                    + "/address/update?user_type=uid&user_id=4080171168&id=" + id);
            update.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            update.setEntity(
                new StringEntity(
                    "{\"id\":\"" + id + "\",\"user_type\":\"uid\",\"user_id\":\"4080171168\"," +
                        "\"address_line\":\"Москва, Льва Толстого, 16\"}",
                    Charset.defaultCharset()));

            HttpAssert.assertJsonResponse(
                client,
                update,
                "{\"status\":\"ok\"," + expAddress + "}");

            HttpGet getEn3 = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/get?user_id=4080171168&user_type=uid&locale=en&id=" + enId);
            getEn3.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, client, getEn3);
        }
    }

    @Test
    public void testListAddressConversion() throws Exception {
        try (AddressProxyCluster cluster = new AddressProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.geocoder().add(
                geocoderRequest(
                    "%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C+%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C+%D0%9B%D1%8C%D0%B2%D0%B0" +
                    "+%D0%A2%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2C+16", "en-US"),
                new StaticHttpResource( // Россия Москва Льва Толстого 16
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("convert_address_en_geocoder.response"))));

            cluster.geocoder().add(
                geocoderRequest("%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C+%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C" +
                    "+%D0%A2%D0%B2%D0%B5%D1%80%D1%81%D0%BA%D0%B0%D1%8F%2C+1", "en-US"),
                new StaticHttpResource( // Россия Москва Тверская 1
                    HttpStatus.SC_OK,
                    new InputStreamEntity(this.getClass().getResourceAsStream("convert_address_en_geocoder2.response"))));

            // create few addresses

            String createUri = cluster.proxy().host().toString()
                + "/address/create?user_type=uid&user_id=4080171168&id=";
            String updateUri = cluster.proxy().host().toString()
                + "/address/update?user_type=uid&user_id=4080171168&id=";
            String entity = "{\"user_type\":\"uid\",\"user_id\":\"4080171168\"}";
            String expected = "{\"status\":\"ok\", \"version\":0,\"id\":\"";

            // work
            HttpPost create = new HttpPost(createUri + "work");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            create.setEntity(new StringEntity(entity, Charset.defaultCharset()));
            HttpAssert.assertJsonResponse(client, create, expected + "work\"}");


            HttpPost update = new HttpPost(updateUri + "work");
            update.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            update.setEntity(
                new StringEntity(
                    "{\"id\":\"work\",\"user_type\":\"uid\",\"user_id\":\"4080171168\"," +
                        "\"country\":\"Россия\",\"city\":\"Москва\",\"street\":\"Льва Толстого\"," +
                        "\"building\":\"16\"}",
                    Charset.defaultCharset()));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, update);

            // home
            create = new HttpPost(createUri + "home");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            create.setEntity(new StringEntity(entity, Charset.defaultCharset()));
            HttpAssert.assertJsonResponse(client, create, expected + "home\"}");


            update = new HttpPost(updateUri + "home");
            update.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            update.setEntity(
                new StringEntity(
                    "{\"id\":\"home\",\"user_type\":\"uid\",\"user_id\":\"4080171168\"," +
                        "\"country\":\"Россия\",\"city\":\"Москва\",\"street\":\"Тверская\"," +
                        "\"building\":\"1\"}",
                    Charset.defaultCharset()));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, update);

            // not valid for geocoder
            create = new HttpPost(createUri + "123");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            create.setEntity(new StringEntity(entity, Charset.defaultCharset()));
            HttpAssert.assertJsonResponse(client, create, expected + "123\"}");


            update = new HttpPost(updateUri + "123");
            update.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            update.setEntity(
                new StringEntity(
                    "{\"id\":\"123\",\"user_type\":\"uid\",\"user_id\":\"4080171168\"," +
                        "\"country\":\"Россия\",\"city\":\"Москва\",\"street\":\"Ленина\"}",
                    Charset.defaultCharset()));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, update);


            // list ru address

            String baseAddress = "\"owner_service\":\"taxi\",\"type\":\"address\",\"subtype\":\"address\"," +
                "\"comment\":null,\"district\":null," +
                "\"entrance\":null,\"floor\":null,\"intercom\":null," +
                "\"location\":null,\"room\":null," +
                "\"address_type\":\"address\",\"last_touched_time\":null," +
                "\"version\":0,\"format_version\":null,\"name\":null," +
                "\"uri\":null,\"label\":null,\"brand_name\":null,\"phone_id\":null," +
                "\"geocoder_name\":null,\"geocoder_description\":null," +
                "\"geocoder_object_type\":null,\"geocoder_exact\":null,\"comment_courier\":null," +
                "\"org_id\":null,\"modification_time\":\"<any value>\",\"is_portal_uid\": false, \"draft\": false," +
                "\"creation_time\":\"<any value>\",\"address_line\":\"<any value>\",\"id\":\"<any value>\"," +
                "\"user_type\":\"uid\",\"user_id\":\"4080171168\"";

            String ruAddress = baseAddress
                + ",\"country\":\"Россия\",\"city\":\"Москва\",\"locality\":\"Москва\",\"locale\":\"ru\"" +
                ",\"regionId\":null,\"zip\":null,\"region\":null";

            HttpGet get = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/list?user_id=4080171168&user_type=uid");
            get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"more\":false,\"status\":\"ok\",\"addresses\":[{"
                    + ruAddress +  ",\"street\":\"Ленина\",\"building\":null"
                    + "},{"
                    + ruAddress +  ",\"street\":\"Льва Толстого\",\"building\":\"16\""
                    + "},{"
                    + ruAddress +  ",\"street\":\"Тверская\",\"building\":\"1\""
                    + "}]}");


            // list en address without convert

            get = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/list?user_id=4080171168&user_type=uid&locale=en");
            get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"more\":false,\"status\":\"ok\",\"addresses\":[]}");


            // list en address with convert

            //TODO fix region name in other locale
            String enAddress = baseAddress
                + ",\"country\":\"Russia\",\"city\":\"Moscow\",\"locality\":\"Moscow\",\"locale\":\"en\",\"regionId\":213"
                + ",\"region\":\"Москва и Московская область\"";

            get = new HttpGet(
                cluster.proxy().host().toString()
                    + "/address/list?user_id=4080171168&user_type=uid&locale=en&convert");
            get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_TAXI_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"more\":false,\"status\":\"ok\",\"addresses\":[{"
                    + enAddress +  ",\"street\":\"Lva Tolstogo Street\",\"building\":\"16\",\"zip\":\"119021\""
                    + "},{"
                    + enAddress +  ",\"street\":\"Tverskaya Street\",\"building\":\"1\",\"zip\":\"125009\""
                    + "}]}");
        }
    }

    @Ignore
    @Test
    public void testRegionId() throws Exception {
        Field field = Paths.class.getDeclaredField("sourceRoot");
        field.setAccessible(true);
        field.set(null, "/home/vonidu/Projects/PS/repo/arc2/arcadia");
        field = Paths.class.getDeclaredField("sandboxResourcesRoot");
        field.setAccessible(true);
        field.set(null, "/home/vonidu/Downloads");
        int regionId = 117428;
        //int regionId = 213;
        RegionService regionService = RegionServiceWrapper.regionService;
        @SuppressWarnings("unchecked")
        RegionTree<? extends Region> regionTree = regionService.getRegionTree();
        for (int id: regionTree.getPathToRoot(regionId)) {
            System.out.println("RegionId " + id + " name " + regionTree.getRegion(id));
        }
        String name = regionTree.getPathToRoot(regionId)
            .stream()
            .map(regionTree::getRegion)
            .filter(Objects::nonNull)
            .filter(region -> region.getType() == RegionType.SUBJECT_FEDERATION)
            .findFirst()
            .map(Region::getName).orElse(null);
        System.err.println(name);
    }

    private static final class SimpleJsonParser {
        private final JsonParser parser;
        private final BasicGenericConsumer<JsonObject, JsonException> consumer;

        SimpleJsonParser() {
            consumer = new BasicGenericConsumer<>();
            parser =
                new JsonParser(new StackContentHandler(
                    new TypesafeValueContentHandler(consumer)));
        }

        public JsonObject parse(final String json) throws JsonException {
            parser.parse(json);
            return consumer.get();
        }
    }
}
