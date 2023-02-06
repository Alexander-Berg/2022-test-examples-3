package ru.yandex.chemodan.uploader.office;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class MpfsOfficeStoreResponseInfoTest {

    @Test
    public void parseOfficeStoreResponse() {
        String result =
                "{\n" +
                "    \"store_info\" : {\n" +
                "        \"file-id\": \"7b976076afc959f74bfd049c0248a8905a99dc1d48a216ad87af501dabacb4b5\",\n" +
                "        \"uid\": \"128280859\",\n" +
                "        \"service\": \"disk\",\n" +
                "        \"oid\": \"93fd5183e1d8e71b9ae527da6561c49a27fa20bc48bb4e9f997c7a7196540b9a\",\n" +
                "        \"max-file-size\": 10733221536,\n" +
                "        \"callback\": \"http://waser.dev.yandex.net/service/kladun_callback?uid=128280859&oid=93fd5183e1d8e71b9ae527da6561c49a27fa20bc48bb4e9f997c7a7196540b9a\",\n" +
                "        \"api\": \"0.2\",\n" +
                "        \"path\": \"128280859:/disk/test.docx\"\n" +
                "    },\n" +
                "    \"response_body\": \"\",\n" +
                "    \"response_code\": 200,\n" +
                "    \"response_headers\": {}\n" +
                "}";

        MpfsOfficeStoreResponseInfo response = MpfsOfficeStoreResponseInfo.PS.getParser().parseJson(result);

        Assert.equals(200, response.statusCode);
        Assert.equals("", response.body);
        Assert.equals(Cf.map(), response.headers);
        Assert.some(response.storeInfo);
        MpfsInitialStoreInfo info = response.storeInfo.get();

        Assert.equals(128280859L, info.uid);
        Assert.equals(10733221536L, info.maxFileSize);
        Assert.equals(
                "http://waser.dev.yandex.net/service/kladun_callback?uid=128280859&oid=93fd5183e1d8e71b9ae527da6561c49a27fa20bc48bb4e9f997c7a7196540b9a",
                info.callback);
    }

    @Test
    public void parseOfficeStoreResponseWithConflict() {
        String result =
                "{\n" +
                "    \"response_body\": \"\",\n" +
                "    \"response_code\": 409,\n" +
                "    \"response_headers\": {\n" +
                "        \"X-WOPI-Lock\": \"123\"\n" +
                "    }\n" +
                "}";

        MpfsOfficeStoreResponseInfo response = MpfsOfficeStoreResponseInfo.PS.getParser().parseJson(result);

        Assert.equals(409, response.statusCode);
        Assert.equals("", response.body);
        Assert.equals(Cf.map("X-WOPI-Lock", "123"), response.headers);
        Assert.none(response.storeInfo);
    }
}
