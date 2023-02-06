package ru.yandex.market.pers.author.mock.mvc;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.author.takeout.model.TakeoutDataWrapper;
import ru.yandex.market.pers.service.common.dto.TakeoutStatusDto;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service
public class TakeoutControllerMvcMocks extends AbstractMvcMocks {

    public TakeoutDataWrapper getData(String uid) {
        return parseValue(
            invokeAndRetrieveResponse(
                get("/takeout")
                    .param("uid", uid)
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()),
            TakeoutDataWrapper.class);
    }

    public TakeoutStatusDto getStatus(String uid) {
        return parseValue(
            invokeAndRetrieveResponse(
                get("/takeout/status")
                    .param("uid", uid)
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()),
            TakeoutStatusDto.class);
    }

    public void delete(String uid) {
        invokeAndRetrieveResponse(
            post("/takeout/delete")
                .param("uid", uid)
                .param("types", "author")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }
}
