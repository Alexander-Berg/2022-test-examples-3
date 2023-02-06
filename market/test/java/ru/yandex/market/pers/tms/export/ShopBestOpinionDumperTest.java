package ru.yandex.market.pers.tms.export;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author artemmz
 *         created on 21.07.16.
 */
public class ShopBestOpinionDumperTest {
    @Autowired
    ShopBestOpinionDumper shopBestOpinionDumper;

    @Test
    @Ignore
    public void testShopBestOpinions() throws IOException {
        final String LOCAL_FILE = "shop-best-opinion.txt";
        ExportProcessor.writeLocalFile(shopBestOpinionDumper, LOCAL_FILE);
        Files.delete(Paths.get(LOCAL_FILE));
    }

}
