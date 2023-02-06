package ru.yandex.market.pers.qa.mock.mvc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;
import ru.yandex.market.pers.qa.client.dto.UserBanInfoDto;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.client.utils.ControllerConstants;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * @author varvara
 * 23.09.2019
 */
@Service
public class BanMvcMocks extends AbstractMvcMocks  {

    public String trust(long moderatorId, UserType userType, String userId, String body, ResultMatcher expected) throws Exception {
        return invokeAndRetrieveResponse(
            post("/userlist/trust")
                .param(ControllerConstants.MODERATOR_ID_KEY, String.valueOf(moderatorId))
                .param(ControllerConstants.USER_TYPE_KEY, userType.name())
                .param(ControllerConstants.ID_KEY, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            expected);
    }

    public String mistrust(long moderatorId, UserType userType, String userId, ResultMatcher expected) throws Exception {
        return invokeAndRetrieveResponse(
            delete("/userlist/trust")
                .param(ControllerConstants.MODERATOR_ID_KEY, String.valueOf(moderatorId))
                .param(ControllerConstants.USER_TYPE_KEY, userType.name())
                .param(ControllerConstants.ID_KEY, userId)
                .accept(MediaType.APPLICATION_JSON),
            expected);
    }

    public String banUser(long moderatorId, UserType userType, String userId, String body, ResultMatcher expected) throws Exception {
        return invokeAndRetrieveResponse(
            post("/userlist/ban")
                .param(ControllerConstants.MODERATOR_ID_KEY, String.valueOf(moderatorId))
                .param(ControllerConstants.USER_TYPE_KEY, userType.name())
                .param(ControllerConstants.ID_KEY, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            expected);
    }

    public String unbanUser(long moderatorId, UserType userType, String userId, ResultMatcher expected) throws Exception {
        return invokeAndRetrieveResponse(
            delete("/userlist/ban")
                .param(ControllerConstants.MODERATOR_ID_KEY, String.valueOf(moderatorId))
                .param(ControllerConstants.USER_TYPE_KEY, userType.name())
                .param(ControllerConstants.ID_KEY, userId)
                .accept(MediaType.APPLICATION_JSON),
            expected);
    }

    public UserBanInfoDto getUserInfo(UserType userType, String userId, ResultMatcher expected) throws Exception {
        String response = invokeAndRetrieveResponse(
            get("/userlist/info")
                .param(ControllerConstants.USER_TYPE_KEY, userType.name())
                .param(ControllerConstants.ID_KEY, userId)
                .accept(MediaType.APPLICATION_JSON),
            expected);
        UserBanInfoDto banInfoDto = objectMapper.readValue(response, new TypeReference<UserBanInfoDto>() {
        });
        return banInfoDto;
    }

}
