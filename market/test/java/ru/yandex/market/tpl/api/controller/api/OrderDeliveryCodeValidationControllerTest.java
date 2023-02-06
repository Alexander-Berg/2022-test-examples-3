package ru.yandex.market.tpl.api.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationDto;
import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationRequest;
import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationResponceDto;
import ru.yandex.market.tpl.core.domain.order.code.OrderDeliveryCodeValidationService;
import ru.yandex.market.tpl.core.domain.order.code.log.OrderDeliveryCodeValidationLogService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(value = {OrderDeliveryCodeValidationController.class})
class OrderDeliveryCodeValidationControllerTest extends BaseShallowTest {

    public static final String VERIFICATION_CODE = "verificationCode";
    public static final String MULTI_ORDER_ID = "multiOrderId";

    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private OrderDeliveryCodeValidationService orderDeliveryCodeValidator;
    @MockBean
    private OrderDeliveryCodeValidationLogService codeValidationLogService;
    private User user;

    @BeforeEach
    public void setup() {
        user = UserUtil.createUserWithoutSchedule(UID);
        UserUtil.setId(user, USER_ID);
    }

    @DisplayName("Вызов api успешной валидации, код прошел проверку")
    @SneakyThrows
    @Test
    void successValidation() {
        OrderCodeValidationResponceDto validationResult = new OrderCodeValidationResponceDto(true);
        when(orderDeliveryCodeValidator.validate(any(OrderCodeValidationDto.class))).thenReturn(validationResult);
        OrderCodeValidationRequest body = new OrderCodeValidationRequest(VERIFICATION_CODE);

        mockMvc.perform(post("/api/delivery-code/{multiOrderId}/validate", MULTI_ORDER_ID)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk());

        OrderCodeValidationDto orderCodeValidationDto = OrderCodeValidationDto.builder()
                .multiOrderId(MULTI_ORDER_ID)
                .userId(UID)
                .code(VERIFICATION_CODE)
                .build();
        verify(orderDeliveryCodeValidator, atLeastOnce()).validate(orderCodeValidationDto);
        verify(codeValidationLogService, atLeastOnce())
                .saveValidationCode(orderCodeValidationDto, validationResult, user);
    }

    @DisplayName("Вызов api успешной валидации, код не прошел проверку")
    @SneakyThrows
    @Test
    void validateButCodeNoteValid() {
        OrderCodeValidationResponceDto validationResult = new OrderCodeValidationResponceDto(false);
        when(orderDeliveryCodeValidator.validate(any(OrderCodeValidationDto.class))).thenReturn(validationResult);
        OrderCodeValidationRequest body = new OrderCodeValidationRequest(VERIFICATION_CODE);

        mockMvc.perform(post("/api/delivery-code/{multiOrderId}/validate", MULTI_ORDER_ID)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk());

        OrderCodeValidationDto orderCodeValidationDto = OrderCodeValidationDto.builder()
                .multiOrderId(MULTI_ORDER_ID)
                .userId(UID)
                .code(VERIFICATION_CODE)
                .build();
        verify(orderDeliveryCodeValidator, atLeastOnce()).validate(orderCodeValidationDto);
        verify(codeValidationLogService, atLeastOnce())
                .saveValidationCode(orderCodeValidationDto, validationResult, user);
    }

}
