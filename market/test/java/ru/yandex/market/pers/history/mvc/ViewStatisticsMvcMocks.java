package ru.yandex.market.pers.history.mvc;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.history.socialecom.dto.ViewStatDto;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.history.socialecom.model.SocialEcomConstants.POST_ID_PARAM;

@Service
public class ViewStatisticsMvcMocks extends AbstractMvcMocks {

    public ViewStatDto getAuthorViews(String authorId, String userType) {
        return parseValue(invokeAndRetrieveResponse(
                get("/socialecom/views/author/" + userType + "/"+ authorId),
                status().is2xxSuccessful()),
            new TypeReference<>() {}
        );
    }

    public ViewStatDto getPostViewSingle(String postId) {
        return parseValue(invokeAndRetrieveResponse(
                get("/socialecom/views/post/" + postId),
                status().is2xxSuccessful()),
            new TypeReference<>() {}
        );
    }

    public List<ViewStatDto> getPostViewBulk(String... postIds) {
        MockHttpServletRequestBuilder requestBuilder = get("/socialecom/views/post");
        for (String postId: postIds) {
            requestBuilder = requestBuilder.param(POST_ID_PARAM, postId);
        }

        return parseValue(invokeAndRetrieveResponse(requestBuilder, status().is2xxSuccessful()),
            new TypeReference<>() {}
        );
    }
}
