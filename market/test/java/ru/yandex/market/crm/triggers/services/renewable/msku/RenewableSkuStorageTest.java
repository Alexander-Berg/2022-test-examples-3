package ru.yandex.market.crm.triggers.services.renewable.msku;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class RenewableSkuStorageTest {
    @Test
    public void loadFileTest() throws IOException {
        final String pathToFile = getClass().getClassLoader()
                .getResource("renewable_mskus_20190801-150737.pb.bin")
                .getPath();
        final RenewableSkuStorage storage = new RenewableSkuStorage(pathToFile, false);

        Assert.assertEquals(168L, storage.get("100407442826").getRepurchasePeriod());
        Assert.assertEquals(60L, storage.get("36146556").getRepurchasePeriod());
    }

    @Test(expected = FileNotFoundException.class)
    public void loadFileNegativeTest() throws IOException {
        new RenewableSkuStorage("totally/wrong/path",false);
    }
}
