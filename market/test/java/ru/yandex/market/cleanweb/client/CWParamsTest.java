package ru.yandex.market.cleanweb.client;

import java.io.IOException;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CWParamsTest {

    @Test
    public void testWhenRequestFromCacheThenNormalParsing() throws IOException {

        InputStreamReader streamReader = new InputStreamReader(
            getClass().getResourceAsStream("/cwRequestOldFormat.json")
        );
        //При ошибке десериализации будет ошибка
        CWDocumentRequest request = new ObjectMapper().readValue(streamReader, CWDocumentRequest.class);
        assertThat(request.getCwParams().getBody().getAutomationLevels()).isNull();
    }

    @Test
    public void testWhenRequestWithAutoLevelsThenNormalParsing() throws IOException {

        InputStreamReader streamReader = new InputStreamReader(
                getClass().getResourceAsStream("/cwRequestWithAutomationLevels.json")
        );
        //При ошибке десериализации будет ошибка
        CWDocumentRequest request = new ObjectMapper().readValue(streamReader, CWDocumentRequest.class);
        CWParams.Body.AutomationLevels automationLevels = request.getCwParams().getBody().getAutomationLevels();
        assertThat(automationLevels).isNotNull();
        assertThat(automationLevels.getWatermarkImageToloka()).isEqualTo(AutomationLevelType.DEFAULT);
        assertThat(automationLevels.getWatermarkImageTernaryToloka()).isEqualTo(AutomationLevelType.HUMAN_ONLY);
        assertThat(automationLevels.getCpYang()).isEqualTo(AutomationLevelType.DEFAULT);
    }

    @Test
    public void testSerialization() throws IOException {
        CWDocumentRequest request = new CWDocumentRequest(11L, CWParams.forText("key",
                CommonAutomationLevels.DEFAULT_TEXT, "some text"));

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(request);
        assertThat(json).doesNotContain("human_only");
        assertThat(json).doesNotContain("auto_only");
    }

    @Test
    public void testCwResponse() throws IOException {
        InputStreamReader streamReader = new InputStreamReader(
                getClass().getResourceAsStream("/cwResponse.json")
        );

        CWDocumentResponse response = new ObjectMapper().readValue(streamReader, CWDocumentResponse.class);
        assertThat(response.getResult().getExtendedErrors()).containsKey("clean-web");
        assertThat(response.getResult().getExtendedErrors().get("clean-web").getMessage()).isEqualTo("Bad Request");
        assertThat(response.getResult().getExtendedErrors().get("clean-web").getCode()).isEqualTo(400);

        assertThat(response.getResult().getErrors()).containsKey("clean-web");
        assertThat(response.getResult().getErrors().get("clean-web")).isEqualTo("server_error");
    }
}
