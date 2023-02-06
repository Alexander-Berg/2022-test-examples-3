package ru.yandex.chemodan.uploader.office;

import org.junit.Test;

import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class MpfsInitialStoreInfoTest {

    @Test
    public void parseOfficeStoreResponse() {
        String result = ClassLoaderUtils.loadText(this.getClass(), "initial_store_info.json");
        MpfsInitialStoreInfo info = MpfsInitialStoreInfo.PS.getParser().parseJson(result);

        Assert.equals(128280859L, info.uid);
        Assert.equals(10733221536L, info.maxFileSize);
        Assert.equals(
                "http://waser.dev.yandex.net/service/kladun_callback?uid=128280859&oid=93fd5183e1d8e71b9ae527da6561c49a27fa20bc48bb4e9f997c7a7196540b9a",
                info.callback);
    }
}
