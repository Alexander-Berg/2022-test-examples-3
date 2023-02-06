package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.measurement.client.utils.CubiScanTcpClient;
import ru.yandex.market.wms.servicebus.api.external.measurement.model.Infoscan3D90Response;
import ru.yandex.market.wms.servicebus.core.measurement.request.GetDimensionsRequest;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class MeasureEquipmentControllerTest extends IntegrationTest {

    protected static MockWebServer mockBackEnd;
    private final ObjectMapper mapper = new ObjectMapper();

    @MockBean
    @Autowired
    private CubiScanTcpClient tcpClient;

    @Test
    public void getDimensionsByInfoscan3D90HappyPath() throws Exception {
        var request = mapper.readValue(getFileContent("api/internal/wms/measurement/request.json"),
                GetDimensionsRequest.class);
        mockBackEnd = new MockWebServer();
        mockBackEnd.start(request.properties().port());

        var response = mapper.readValue(
                getFileContent("api/internal/wms/measurement/happy-path/equipment-response.json"),
                Infoscan3D90Response.class
        );
        var responseBody = mapper.writeValueAsString(response);
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(responseBody)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/measure-equipment/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("api/internal/wms/measurement/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/measurement/response.json")))
                .andReturn();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest(5, TimeUnit.SECONDS);

        if (recordedRequest == null) {
            assertions.fail("Request not found");
        }

        HttpUrl expectedUrl = new HttpUrl.Builder()
                .scheme("http")
                .host(request.properties().hostname())
                .port(request.properties().port())
                .addPathSegment(request.properties().path())
                .build();

        String token = request.properties().login() + ":" + request.properties().password();
        String expectedToken = "Basic " + new String(Base64.getMimeEncoder().encode(token.getBytes()));

        assertSoftly(assertions -> {
            assertions.assertThat(recordedRequest).isNotNull();
            assertions.assertThat(recordedRequest.getMethod()).isEqualTo(HttpMethod.GET.name());
            assertions.assertThat(recordedRequest.getRequestUrl()).isEqualTo(expectedUrl);
            assertions.assertThat(recordedRequest.getHeader("Accept"))
                    .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
            assertions.assertThat(recordedRequest.getHeader("Authorization"))
                    .isEqualTo(expectedToken);
        });

        mockBackEnd.shutdown();
    }

    @Test
    public void getDimensionsByCubiScan125HappyPath() throws Exception {
        String answer = new StringBuilder()
                .append((char) 0x02) // STX
                .append("M")
                .append("A")
                .append("C")
                .append("TEST01")
                .append(",")
                .append("L")
                .append((char) 0x00)
                .append((char) 0x00)
                .append("7,7") // Length
                .append(",")
                .append("W")
                .append((char) 0x00)
                .append("20,6") // Width
                .append(",")
                .append("H")
                .append((char) 0x00)
                .append("23,4") // Height
                .append(",")
                .append("E")
                .append(",")
                .append("K")
                .append((char) 0x00)
                .append("10,64") // Weight
                .append(",")
                .append("D")
                .append((char) 0x00)
                .append("20,17")
                .append(",")
                .append("E")
                .append(",")
                .append("F0166")
                .append(",")
                .append("D")
                .append((char) 0x03) // ETX
                .append((char) 0x0D) // Carriage return
                .append((char) 0x0A) // LF
                .toString();

        Mockito.when(tcpClient.sendMeasureCommand(anyString(), anyInt())).thenReturn(answer);

        mockMvc.perform(post("/measure-equipment/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("api/internal/wms/measurement/cubi-scan-125/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("api/internal/wms/measurement/cubi-scan-125/response.json"))
                )
                .andReturn();
    }

    @Test
    public void getDimensionsByCubiScan125ShouldThrowMeasureException() throws Exception {
        String answer = new StringBuilder()
                .append((char) 0x02) // STX
                .append("M")
                .append("N")
                .append("C")
                .append("M")
                .append((char) 0x03) // ETX
                .append((char) 0x0D) // Carriage return
                .append((char) 0x0A) // LF
                .toString();

        Mockito.when(tcpClient.sendMeasureCommand(anyString(), anyInt())).thenReturn(answer);

        mockMvc.perform(post("/measure-equipment/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("api/internal/wms/measurement/cubi-scan-125/request.json")))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void getDimensionsByCubiScan125ShouldThrowCornerSensorException() throws Exception {
        String answer = new StringBuilder()
                .append((char) 0x02) // STX
                .append("M")
                .append("N")
                .append("C")
                .append("C")
                .append((char) 0x03) // ETX
                .append((char) 0x0D) // Carriage return
                .append((char) 0x0A) // LF
                .toString();

        Mockito.when(tcpClient.sendMeasureCommand(anyString(), anyInt())).thenReturn(answer);

        mockMvc.perform(post("/measure-equipment/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("api/internal/wms/measurement/cubi-scan-125/request.json")))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void getDimensionsByCubiScan125ShouldThrowZeroException() throws Exception {
        String answer = new StringBuilder()
                .append((char) 0x02) // STX
                .append("M")
                .append("N")
                .append("C")
                .append("Z")
                .append((char) 0x03) // ETX
                .append((char) 0x0D) // Carriage return
                .append((char) 0x0A) // LF
                .toString();

        Mockito.when(tcpClient.sendMeasureCommand(anyString(), anyInt())).thenReturn(answer);

        mockMvc.perform(post("/measure-equipment/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("api/internal/wms/measurement/cubi-scan-125/request.json")))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }
}
