package ru.yandex.market.pers.notify.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.notify.model.DeletedUserInfo;
import ru.yandex.market.pers.notify.model.Market;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.web.PersNotifyTag;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


public class UserControllerInvoker {
    @Autowired
    private MockMvc mockMvc;

    public DeletedUserInfo deleteUser(Uid uid, Market store, String reason, ResultMatcher resultMatcher) throws Exception {
        MvcResult result = mockMvc.perform(delete("/user/" + uid.getValue())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .param(PersNotifyTag.MARKET, store.name())
                                    .param(PersNotifyTag.DELETE_REASON, reason))
                                    .andDo(print())
                                    .andExpect(resultMatcher)
                                    .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(result.getResponse().getContentAsString(), DeletedUserInfo.class);
    }
}
