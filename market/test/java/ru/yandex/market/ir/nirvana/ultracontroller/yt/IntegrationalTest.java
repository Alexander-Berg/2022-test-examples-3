package ru.yandex.market.ir.nirvana.ultracontroller.yt;

import org.junit.Ignore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author inenakhov
 */
@Ignore
public class IntegrationalTest {
    private IntegrationalTest() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("continueAfterRestart", "true");
        String ytToken = Files.readAllLines(Paths.get(System.getProperty("user.home"),"/.yt/token")).get(0);

        System.setProperty("config_file_path", "ultracontroller-nirvana/src/test/resources/uc_config_sku.json");
        System.setProperty("sourceTable", "ultracontroller-nirvana/src/test/resources/mrtable.json");
        System.setProperty("outTablesFolder", "ultracontroller-nirvana/src/test/resources/mrdir.json");
        System.setProperty("yt_token", ytToken);
        System.setProperty("resultTable", "ultracontroller-nirvana/src/test/resources/result_table.json");

        Main.main(new String[]{});
    }
}
