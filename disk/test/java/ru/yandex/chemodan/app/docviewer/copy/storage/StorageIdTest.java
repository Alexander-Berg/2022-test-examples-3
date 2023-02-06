package ru.yandex.chemodan.app.docviewer.copy.storage;

import java.net.URI;

import junit.framework.Assert;
import org.junit.Test;

import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.inside.mulca.MulcaRange;

/**
 * @author metal
 */
public class StorageIdTest {
    @Test
    public void isMdsStidTest() {
        Assert.assertTrue(fromSerializedMulcaId("1000044.tmp.E1554:288803467514948820242487140078").isForwardableToSrw());
        Assert.assertFalse(fromSerializedMulcaId("1000003.yadisk:14367847.3983296384128812767959018411472").isForwardableToSrw());
        Assert.assertFalse(fromSerializedMulcaId("1000003.yadisk:3983296384128812767959018411472").isForwardableToSrw());

        StorageId storageId = StorageId.fromMulcaId(
                MulcaId.fromSerializedString("1000044.tmp.E1554:288803467514948820242487140078/1.1"),
                new MulcaRange(10, 100));
        Assert.assertFalse(storageId.isForwardableToSrw());

        Assert.assertTrue(fromSerializedMdsKey("4231/some_mds_key_value").isForwardableToSrw());
    }

    @Test
    public void getUnistorageId() {
        Assert.assertEquals("1000044.tmp.E1554:288803467514948820242487140078",
                fromSerializedMulcaId("1000044.tmp.E1554:288803467514948820242487140078").getIdForUnistorageServiceRead2());

        Assert.assertEquals("browser-clouddoc/4231/some_mds_key_value",
                fromSerializedMdsKey("4231/some_mds_key_value").getIdForUnistorageServiceRead2());
    }

    private StorageId fromSerializedMulcaId(String mulcaId) {
        return StorageId.fromMulcaId(MulcaId.fromSerializedString(mulcaId));
    }

    private StorageId fromSerializedMdsKey(String mdsKey) {
        return StorageId.fromMdsKey("browser-clouddoc", MdsFileKey.parse(mdsKey));
    }

    @Test
    public void fromHttpUri() throws Exception {
        URI uri = new URI("http://storage-int.mdst.yandex.net/get-browser-tmp/4231/some_mds_key_value");
        Assert.assertEquals(new MdsStorageId(new MdsFileKey(4231, "some_mds_key_value"), "browser-tmp"),
                MdsStorageId.fromHttpUri(uri));
    }
}
