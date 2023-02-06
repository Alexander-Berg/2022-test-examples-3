package ru.yandex.market.freqparser;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.yandex.market.freqparser.protos.RenewableMskuProto;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;

/**
 * Unit test for the App.
 */
public class AppTest {

    @Before
    public void init() {
        System.setProperty("app.testmode", "true");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noPathTest() throws Exception {
        String[] args = new String[0];
        App.main(args);
    }

    @Test
    public void positiveTest() throws Exception {
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
        String tempFolderPath = temporaryFolder.getRoot().getAbsolutePath();

        String resourcesPath = getClass().getClassLoader().getResource("").getPath();

        System.setProperty("app.input", resourcesPath);
        System.setProperty("app.output", tempFolderPath);

        System.setProperty("app.user", "test");
        System.setProperty("app.token", "tooooken");

        String[] args = new String[0];
        App.main(args);

        File dir = temporaryFolder.getRoot();

        FileFilter fileFilter = new WildcardFileFilter(Collections.singletonList("*.bin"), IOCase.INSENSITIVE);
        File[] files = dir.listFiles(fileFilter);

        Assert.assertNotNull(files);
        File file = files[0];

        try (InputStream inputStream = new FileInputStream(file)) {
            RenewableMskuProto.RenewableMskus renewableMskus =
                    RenewableMskuProto.RenewableMskus.parseFrom(inputStream);

            Assert.assertEquals(99, renewableMskus.getItemCount());

            Assert.assertEquals(100326876391L, renewableMskus.getItem(0).getMarketSku());
            Assert.assertEquals(200L, renewableMskus.getItem(0).getRepurchasePeriod());

            Assert.assertEquals(100304632951L, renewableMskus.getItem(98).getMarketSku());
            Assert.assertEquals(200L, renewableMskus.getItem(98).getRepurchasePeriod());
        }

        temporaryFolder.delete();
    }
}
