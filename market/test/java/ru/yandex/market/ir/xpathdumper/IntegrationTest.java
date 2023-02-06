package ru.yandex.market.ir.xpathdumper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author inenakhov
 */
@Ignore
public class IntegrationTest {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        File configFile = new File("xpathdumper/src/test/resources/start_params.json");
        Params params = objectMapper.readValue(configFile, Params.class);

        System.setProperty("cluster", params.getCluster());
        System.setProperty("yt_token", params.getYtToken());
        System.setProperty("output_table", params.getOutputTable());
        System.setProperty("result_table_file", params.getResultTableFile());
        System.setProperty("scat.robot.username", params.getScatRobotUsername());
        System.setProperty("scat.robot.password", params.getScatRobotPassword());

        Main.main(new String[]{});
    }
}
