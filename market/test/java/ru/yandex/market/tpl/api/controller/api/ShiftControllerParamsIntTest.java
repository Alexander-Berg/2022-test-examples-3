package ru.yandex.market.tpl.api.controller.api;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.task.LockerDeliveryTaskFailReasonTypeDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.usershift.params.UserShiftParamsDto;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonTypeUtil.getFailReasonTypeDescription;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ShiftControllerParamsIntTest extends BaseApiIntTest {
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    private Shift shift;
    private UserShift userShift;

    @BeforeEach
    void setUp() {
        var now = LocalDate.now(clock);

        var user = testUserHelper.findOrCreateUser(UID);

        shift = testUserHelper.findOrCreateOpenShift(now);
        userShift = testUserHelper.createEmptyShift(user, shift);
    }

    @Test
    void shouldReturnPhotoNeeded() throws Exception {
        String responseString = mockMvc.perform(
                get("/api/shifts/{id}/params", userShift.getId())
                        .header(AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var userShiftParamsDto = objectMapper.readValue(responseString, UserShiftParamsDto.class);

        Map<String, LockerDeliveryTaskFailReasonTypeDto> failReasons =
                StreamEx.of(userShiftParamsDto.getLockerDelivery().getTaskFailReasons())
                .toMap(LockerDeliveryTaskFailReasonTypeDto::getName, Function.identity());


        Assertions.assertThat(failReasons)
                .hasSize((int) StreamEx.of(OrderDeliveryTaskFailReasonType.LOCKER_FAIL_REASONS)
                        .count());

        StreamEx.of(OrderDeliveryTaskFailReasonType.LOCKER_FAIL_REASONS)
                .forEach(type -> {
                    Assertions.assertThat(failReasons.get(type.name()).getDescription())
                            .isEqualTo(getFailReasonTypeDescription(PartnerOrderType.LOCKER, type));
                    Assertions.assertThat(failReasons.get(type.name()).isPhotoNeeded())
                            .isEqualTo(type.isPhotoUrlsNeededForLocker());
                    Assertions.assertThat(failReasons.get(type.name()).isCommentNeeded())
                            .isEqualTo(type.isCommentNeededForLocker());
                });



    }

    @SneakyThrows
    @Test
    void shouldReturnCommentNeeded() {
        String responseString = mockMvc.perform(
                get("/api/shifts/{id}/params", userShift.getId())
                        .header(AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var userShiftParamsDto = objectMapper.readValue(responseString, UserShiftParamsDto.class);

        Assertions.assertThat(userShiftParamsDto.getLavkaDelivery().getTaskFailReasons())
                .anyMatch(failReasonType ->
                        OrderDeliveryTaskFailReasonType.LAVKA_TECHNICAL_ISSUES.name().equals(failReasonType.getName())
                                && failReasonType.isCommentNeeded()
                );

        Assertions.assertThat(userShiftParamsDto.getPvzDelivery().getTaskFailReasons())
                .anyMatch(failReasonType ->
                        OrderDeliveryTaskFailReasonType.PVZ_TECHNICAL_ISSUES.name().equals(failReasonType.getName())
                                && failReasonType.isCommentNeeded()
                );

        Assertions.assertThat(userShiftParamsDto.getLockerDelivery().getOrderExtraditionFailReasons())
                .anyMatch(failReasonType ->
                        OrderDeliveryTaskFailReasonType.CELL_IS_EMPTY.name().equals(failReasonType.getName())
                                && failReasonType.isCommentNeeded()
                );

        Assertions.assertThat(userShiftParamsDto.getLockerDelivery().getOrderExtraditionFailReasons())
                .anyMatch(failReasonType ->
                        OrderDeliveryTaskFailReasonType.ANOTHER_PLACE_IN_CELL_EXTRADITION.name().equals(failReasonType.getName())
                                && failReasonType.isCommentNeeded()
                );

    }
}
