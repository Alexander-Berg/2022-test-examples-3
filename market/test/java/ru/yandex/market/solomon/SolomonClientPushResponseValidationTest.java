package ru.yandex.market.solomon;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 08.08.2018
 */
public class SolomonClientPushResponseValidationTest {
    private final SolomonShardId shardId = new SolomonShardId("project1", "service1", "cluster1");

    @Test
    public void threeSuccessfulSensors() {
        String responseBody = "" +
            "timestamp:   2018-08-08T11:06:40.002Z\n" +
            "server fqdn: solomon-kfront-sas-02.search.yandex.net\n" +
            "successful sensors 3~3; statuses: {OK=[null, null]}\n";
        assertEquals(3, SolomonClient.checkPushResponseAndReturnSavedSensorsCount(shardId, 200, responseBody));
    }

    @Test
    public void statusNot2xx() {
        assertThatThrownBy(() -> SolomonClient.checkPushResponseAndReturnSavedSensorsCount(shardId, 500, ""))
            .isInstanceOf(SolomonClientException.class)
            .hasMessageContaining("500");
    }

    @Test
    public void firstNumberOfSensorsIsWrong() {
        String responseBody = "" +
            "timestamp:   2018-08-08T11:06:40.002Z\n" +
            "server fqdn: solomon-kfront-sas-02.search.yandex.net\n" +
            "successful sensors 2~3; statuses: {OK=[null, null]}\n";
        assertThatCode(() -> SolomonClient.checkPushResponseAndReturnSavedSensorsCount(shardId, 200, responseBody))
            .isInstanceOf(SolomonClientException.class)
            .hasMessageContaining(responseBody);
    }

    @Test
    public void secondNumberOfSensorsIsWrong() {
        String responseBody = "" +
            "timestamp:   2018-08-08T11:06:40.002Z\n" +
            "server fqdn: solomon-kfront-sas-02.search.yandex.net\n" +
            "successful sensors 3~2; statuses: {OK=[null, null]}\n";
        assertThatCode(() -> SolomonClient.checkPushResponseAndReturnSavedSensorsCount(shardId, 200, responseBody))
            .isInstanceOf(SolomonClientException.class)
            .hasMessageContaining(responseBody);
    }

    @Test
    public void wrongResponseFromSolomon() {
        String responseBody = "Solomon API is great =/ \n";
        assertThatCode(() -> SolomonClient.checkPushResponseAndReturnSavedSensorsCount(shardId, 200, responseBody))
            .isInstanceOf(SolomonClientException.class)
            .hasMessageContaining(responseBody);
    }

    @Test
    public void tooLargeNumberOfSensors() {
        String responseBody = "" +
            "timestamp:   2018-08-08T11:06:40.002Z\n" +
            "server fqdn: solomon-kfront-sas-02.search.yandex.net\n" +
            "successful sensors 12345678900987654321~3; statuses: {OK=[null, null]}\n";
        assertThatCode(() -> SolomonClient.checkPushResponseAndReturnSavedSensorsCount(shardId, 200, responseBody))
            .isInstanceOf(SolomonClientException.class)
            .hasMessageContaining(responseBody)
            .hasCauseInstanceOf(NumberFormatException.class);
    }
}
