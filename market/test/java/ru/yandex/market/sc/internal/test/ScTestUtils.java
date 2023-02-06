package ru.yandex.market.sc.internal.test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.validation.Validation;
import javax.validation.Validator;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.test.TestFactory;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@Slf4j
@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class ScTestUtils {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @SneakyThrows
    public static String fileContent(String fileName) {
        return IOUtils.toString(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(fileName)
                ),
                StandardCharsets.UTF_8
        );
    }

    public ResultActions ffApiSuccessfulCall(MockMvc mockMvc, String token, String requestType, String requestContent) {
        return ffApiSuccessfulCall(
                mockMvc,
                String.format(fileContent("ff_request_template.xml"), token, requestType, requestContent)
        );
    }

    @SneakyThrows
    public ResultActions ffApiSuccessfulCall(MockMvc mockMvc, String body) {
        var resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/fulfillment/query-gateway")
                        .contentType(MediaType.TEXT_XML)
                        .content(body)
        )
                .andExpect(status().isOk())
                .andExpect(xpath("/root/requestState/isError").string("false"));
        log.info("resultActions: " + resultActions.andReturn().getResponse().getContentAsString());
        return resultActions;
    }

    public ResultActions ffApiV2SuccessfulCall(MockMvc mockMvc, String token, String requestType, String content) {
        return ffApiV2SuccessfulCall(
                mockMvc,
                String.format(fileContent("ff_request_template.xml"), token, requestType, content)
        );
    }

    @SneakyThrows
    public ResultActions ffApiV2SuccessfulCall(MockMvc mockMvc, String body) {
        return mockMvc.perform(
                MockMvcRequestBuilders.post("/v2/fulfillment/query-gateway")
                        .contentType(MediaType.TEXT_XML)
                        .content(body)
        )
                .andExpect(status().isOk())
                .andExpect(xpath("/root/requestState/isError").string("false"));
    }

    @SneakyThrows
    public ResultActions ffApiV2ErrorCall(MockMvc mockMvc, String body, String errorMessage) {
        return mockMvc.perform(
                MockMvcRequestBuilders.post("/v2/fulfillment/query-gateway")
                        .contentType(MediaType.TEXT_XML)
                        .content(body)
        )
                .andExpect(status().isOk())
                .andExpect(xpath("/root/requestState/isError").string("true"))
                .andExpect(xpath("/root/requestState/errorCodes[1]/errorCode/message").string(errorMessage));
    }

    @SneakyThrows
    public ResultActions ffApiErrorCall(MockMvc mockMvc, String body, String errorMessage) {
        return mockMvc.perform(
                        MockMvcRequestBuilders.post("/fulfillment/query-gateway")
                                .contentType(MediaType.TEXT_XML)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(xpath("/root/requestState/isError").string("true"))
                .andExpect(xpath("/root/requestState/errorCodes[1]/errorCode/message").string(errorMessage));
    }

    public boolean isValid(Object obj) {
        return VALIDATOR.validate(obj).isEmpty();
    }

}
