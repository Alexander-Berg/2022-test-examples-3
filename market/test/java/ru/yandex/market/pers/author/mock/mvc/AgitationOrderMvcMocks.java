package ru.yandex.market.pers.author.mock.mvc;

import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.util.ExecUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.FORCE_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.ORDER_ID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.TYPE_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 08.07.2020
 */
@Service
public class AgitationOrderMvcMocks extends AgitationMvcMocks {
    public AgitationOrderMvcMocks() {
        super("order/");
    }

    public void addOrderAgitation(AgitationUser user,
                             AgitationType type,
                             String entityId,
                             boolean force) {
        invokeAndRetrieveResponse(
            post("/agitation/" + getUserPath(user))
                .param(TYPE_KEY, String.valueOf(type.value()))
                .param(ORDER_ID_KEY, entityId)
                .param(FORCE_KEY, String.valueOf(force))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public void addOrderAgitationIncorrectMediaType(AgitationUser user,
                                                    AgitationType type,
                                                    String entityId,
                                                    boolean force) {
        try {
            mockMvc.perform(
                    post("/agitation/" + getUserPath(user))
                            .param(TYPE_KEY, String.valueOf(type.value()))
                            .param(ORDER_ID_KEY, entityId)
                            .param(FORCE_KEY, String.valueOf(force))
                            .content("<tag>sometext</tag>")
                            .accept("application/xml;charset=UTF-8"))
                    .andDo(print())
                    .andExpect(status().is(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415))
                    .andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw ExecUtils.silentError(e, "Failed mvcMock call");
        }
    }
}
