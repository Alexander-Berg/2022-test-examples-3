package ru.yandex.canvas.controllers;

import com.mongodb.client.result.UpdateResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.service.html5.Html5BatchesService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class Html5BatchControllerRenameBatchNegativeTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private Html5BatchesService batchesService;

    private int nameMaxLen = 255;

    private String batchId = "aaa";
    private String uri = "/html5/batch";
    private String uriBatchId = uri + "/" + batchId;
    private String nameExceedLen = StringUtils.repeat("N", nameMaxLen + 1);

    @Before
    public void stubBatchService() {
        // Overwise if validation fails to deflect malfunction request we will have bogus NPE and broken test
        // beside normal test failed output
        given(batchesService.updateBatchName(any(), anyLong(), any(), any()))
                .willReturn(UpdateResult.acknowledged(1, 1L, null));
    }

    @Test
    public void clientIdRequired() throws Exception {
        mvc.perform(patch(uriBatchId).params(
                new LinkedMultiValueMap<>() {{
                    add("name", "New name");
                }})
        ).andExpect(status().is(400))
                .andExpect(status().reason("Required request parameter 'client_id' for method parameter type Long is not present"));

    }

    @Test
    public void nameRequired() throws Exception {
        mvc.perform(patch(uriBatchId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .param("client_id", "123")
        ).andExpect(status().is(400))
                .andExpect(content().string("must not be empty"));
    }

    @Test
    public void emptyNameFails() throws Exception {
        given(batchesService.updateBatchName(any(), anyLong(), any(), any()))
                .willReturn(UpdateResult.acknowledged(1, 1L, null));
        mvc.perform(patch(uriBatchId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")
                .param("client_id", "123")
        )
                .andExpect(status().is(400))
                .andExpect(content().string("Text length must be between 1 and 255 symbols, must not be empty"));
        // TODO: разобраться почему остальное возвращается как Error message, а из GlobalExceptionHander идет Body
    }

    @Test
    public void nameExceedMaxLengthFails() throws Exception {
        mvc.perform(patch(uriBatchId)
                .content("{\"name\":\"" + nameExceedLen + "\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .param("client_id", "123")
        ).andExpect(status().is(400))
                .andExpect(content().string("Text length must be between 1 and 255 symbols"));
        // TODO: разобраться почему остальное возвращается как Error message, а из GlobalExceptionHander идет Body
    }
}
