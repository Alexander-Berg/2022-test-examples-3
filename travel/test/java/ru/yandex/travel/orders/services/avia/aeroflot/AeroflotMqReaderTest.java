package ru.yandex.travel.orders.services.avia.aeroflot;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.testing.misc.TestResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AeroflotMqReaderTest {
    private AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
    private AeroflotMqProperties properties = AeroflotMqProperties.builder()
            .readVisibilityTimeout(Duration.ofSeconds(1))
            .build();
    private AeroflotMqReader reader = new AeroflotMqReader(sqs, properties);

    @Test
    public void readOrders() throws Exception {
        PropertyNamingStrategyBase awsNamingCorrector = new PropertyNamingStrategyBase() {
            @Override
            public String translate(String propertyName) {
                if ("Md5OfBody".equalsIgnoreCase(propertyName)) {
                    return "MD5OfBody";
                }
                return PropertyNamingStrategy.UPPER_CAMEL_CASE.nameForField(null, null, propertyName);
            }
        };
        ObjectMapper jsonMapper = new ObjectMapper().setPropertyNamingStrategy(awsNamingCorrector);
        String sampleResponseJson = TestResources.readResource("aeroflot/mq_sync/sqs_msg_sample.json");
        ReceiveMessageResult result = jsonMapper.readValue(sampleResponseJson, ReceiveMessageResult.class);
        when(sqs.receiveMessage((ReceiveMessageRequest) any())).thenReturn(result);

        List<AeroflotMqRawData> parsed = reader.readOrders();
        assertThat(parsed.size()).isEqualTo(1);

        AeroflotMqRawData rawData = parsed.get(0);
        assertThat(rawData.getId()).isEqualTo("477e120d-d9234dfe-e9991a4d-9b7c41cc");
        assertThat(rawData.getData()).isEqualTo("some_json");
        assertThat(rawData.getHandle()).isEqualTo("CgdZQU5PTkVOEAEaI2Q1ZTYxOWYyLWY2MWEwYjFmLTg2NTEyZDBhLTNkZjdiNDg5IJenp6qsLSgA");
        assertThat(rawData.getSendTimestamp()).isEqualTo(Instant.ofEpochMilli(1558080125329L));
    }
}
