package ru.yandex.market.pers.author.mock.mvc;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.author.agitation.model.Agitation;
import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoListWithPriority;
import ru.yandex.market.pers.author.client.api.model.AgitationType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.TYPE_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 08.07.2020
 */
@Service
public class AgitationBeruMvcMocks extends AgitationMvcMocks {
    public AgitationBeruMvcMocks() {
        super("beru/");
    }

    public List<Agitation> getNotification(AgitationUser user, AgitationType... types) {
        DtoListWithPriority<Agitation> result = parseValue(
            invokeAndRetrieveResponse(
                get("/agitation/" + getUserPath(user) + "/notification")
                    .param(TYPE_KEY, toArrayStr(List.of(types), AgitationType::value))
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()
            ), new TypeReference<>() {
            });

        return result.getData();
    }

    public List<Agitation> getAgitationsByOrderId(AgitationUser user, long orderId, AgitationType... types) {
        DtoList<Agitation> result = parseValue(
                invokeAndRetrieveResponse(
                        get("/agitation/" + getUserPath(user) + "/by-order/" + orderId)
                                .param(TYPE_KEY, toArrayStr(List.of(types), AgitationType::value))
                                .accept(MediaType.APPLICATION_JSON),
                        status().is2xxSuccessful()
                ), new TypeReference<>() {
                });
        return result.getData();
    }
}
