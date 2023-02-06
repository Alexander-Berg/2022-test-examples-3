package ru.yandex.market.pers.notify.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import ru.yandex.market.pers.notify.api.controller.dto.SubscriptionTypeDto;
import ru.yandex.market.pers.notify.api.controller.dto.SubscriptionTypeDto.Channel;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author vtarasoff
 * @since 19.07.2021
 */
public class SubscriptionTypesControllerTest extends MarketUtilsMockedDbTest {
    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnCorrectList() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/subscription-types"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        List<SubscriptionTypeDto> dtos = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<SubscriptionTypeDto>>() {});

        assertThat(dtos.size(), is(NotificationType.values().length));
        dtos.forEach(dto -> {
            var type = dto.getType();
            assertThat(dto.getId(), is(type.getId()));
            assertThat(dto.getDescription(), equalTo(NotificationType.getTypeDescription(type)));
            assertThat(dto.isDefault(), is(NotificationType.isDefault(type)));
            assertThat(dto.isDeprecated(), is(NotificationType.isDeprecated(type)));
            assertThat(dto.getChannels(), equalTo(Set.of(channelBy(type.getTransportType()))));
        });
    }

    private Channel channelBy(NotificationTransportType type) {
        return type == NotificationTransportType.MAIL ? Channel.EMAIL : Channel.PUSH;
    }
}
