package ru.yandex.market.clickphite.solomon;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.clickphite.solomon.dto.SolomonPushRequestBody;
import ru.yandex.market.clickphite.solomon.dto.SolomonSensor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 18.07.2018
 */
@Ignore
public class SolomonClientManualTest {
    /**
     * Можно запустить и проверять что рисуется вот этот график:
     * https://solomon-prestable.yandex-team.ru/?project=market&cluster=test_cluster&service=test_service&graph=auto&b=1h&e=
     * Там должно быть два сенсора, у одного одно значение каждые 10 секунд, у другого два значения каждые 10 секунд.
     * Примерно вот так: https://jing.yandex-team.ru/files/alkedr/2018.07.18_16.30.46.619888288.png
     */
    @Test
    public void infiniteRandomSensor() throws InterruptedException {
        SolomonClient solomonClient = new SolomonClient(
            "http://solomon.yandex.net/",
//            "http://solomon-prestable.yandex.net/",
            "AQAD-qJSJwR8AAAIY8sVKI6eGk3Tkw0dbl0jHiQ",
            60
        );
        Random random = new Random();

        while (true) {
            long timestamp = Instant.now().getEpochSecond();
            try {
                SolomonShardId shardId = new SolomonShardId(
                    "market-infra-test",
                    "test_service",
                    "test_cluster"
                );
                SolomonPushRequestBody sensors = new SolomonPushRequestBody(
                    null,
                    Collections.emptyMap(),
                    Arrays.asList(
                        new SolomonSensor(
                            Collections.singletonMap("host", "tests1"),
                            SolomonSensor.Kind.DGAUGE,
                            random.nextInt(1000),
                            timestamp,
                            null,
                            null
                        ),

                        new SolomonSensor(
                            Collections.singletonMap("host", "tests2"),
                            SolomonSensor.Kind.DGAUGE,
                            random.nextInt(1000),
                            timestamp,
                            Arrays.asList(
                                new SolomonSensor.TimestampValuePair(timestamp - 3, random.nextInt(1000)),
                                new SolomonSensor.TimestampValuePair(timestamp, random.nextInt(1000))
                            ),
                            null
                        )
                    )
                );
                try {
                    solomonClient.push("contextId", shardId, sensors);
                } catch (SolomonShardNotFoundException e) {
                    solomonClient.createServiceAndClusterAndShard(shardId);
                    solomonClient.push("contextId", shardId, sensors);
                }
            } catch (SolomonClientException e) {
                e.printStackTrace();
            }

            System.out.println("Sleeping");
            Thread.sleep(10_000);
        }
    }
}
