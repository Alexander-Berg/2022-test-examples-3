package ru.yandex.market.wms.api.controller;

import java.math.BigDecimal;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.iris.client.api.PushApiClient;
import ru.yandex.market.wms.api.config.IrisClientTestConfiguration;
import ru.yandex.market.wms.api.config.IrisTvmConfiguration;
import ru.yandex.market.wms.api.service.iris.push.IrisPushService;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.PushReferenceItemsResultDto;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(classes = {IntegrationTestConfig.class, IrisTvmConfiguration.class, IrisClientTestConfiguration.class})
public class IrisPushControllerTest extends IntegrationTest {

    @Autowired
    protected PushApiClient pushApiClient;

    @MockBean
    @Autowired
    private IrisPushService irisPushService;

    @Test
    @DatabaseSetup("/iris-push/request/before.xml")
    public void shouldSuccessPushDimensionsToIris() throws Exception {
        mockMvc.perform(post("/iris/push/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("iris-push/request/push-dimensions-request.json")))
                .andExpect(status().isOk());

        ArgumentCaptor<List<PushReferenceItemsResultDto>> dimensionsCaptor = ArgumentCaptor.forClass(List.class);

        verify(irisPushService).pushDimensions(dimensionsCaptor.capture(), eq(145));

        List<PushReferenceItemsResultDto> actualDimensionsList = dimensionsCaptor.getValue();

        assertions.assertThat(actualDimensionsList.size()).isEqualTo(2);
        assertSoftly(assertions -> {
            PushReferenceItemsResultDto item = actualDimensionsList.get(0);

            assertions.assertThat(item.getKorobyte().getWidth()).isEqualTo(1);
            assertions.assertThat(item.getKorobyte().getHeight()).isEqualTo(25);
            assertions.assertThat(item.getKorobyte().getLength()).isEqualTo(1);
            assertions.assertThat(item.getKorobyte().getWeightGross()).isEqualTo(BigDecimal.valueOf(0.01));
            assertions.assertThat(item.getUnitId().getArticle()).isEqualTo("БД277/25");
            assertions.assertThat(item.getUnitId().getVendorId()).isEqualTo(635857L);
        });
    }

    @Test
    public void shouldNotPushToIrisIfListOfDimensionsEmpty() throws Exception {
        mockMvc.perform(post("/iris/push/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("iris-push/request/push-dimensions-request-empty-list.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldNotPushToIrisIfWarehouseIdNotPresent() throws Exception {
        mockMvc.perform(post("/iris/push/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("iris-push/request/push-dimensions-request-empty-list.json")))
                .andExpect(status().isBadRequest());
    }
}
