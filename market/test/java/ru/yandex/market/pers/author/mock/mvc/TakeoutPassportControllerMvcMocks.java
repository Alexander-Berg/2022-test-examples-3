package ru.yandex.market.pers.author.mock.mvc;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.author.takeout.model.TakeoutByLinkRequestDto;
import ru.yandex.market.pers.author.takeout.model.TakeoutStatusDto;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;
import ru.yandex.market.pers.tvm.TvmUtils;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.REF_KEY;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.10.2021
 */
@Service
public class TakeoutPassportControllerMvcMocks extends AbstractMvcMocks {
    private static final String MOCK_TOKEN = "token";

    public TakeoutStatusDto getTakeoutStatus(String ref) {
        return parseValue(invokeAndRetrieveResponse(
            get("/takeout/passport/status")
                .param(REF_KEY, ref)
                .header(TvmUtils.SERVICE_TICKET_HEADER, MOCK_TOKEN)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        ), TakeoutStatusDto.class);
    }

    public String getTakeoutStatusWithToken(String ref, String token, ResultMatcher resultMatcher) {
        MockHttpServletRequestBuilder builder = get("/takeout/passport/status")
            .param(REF_KEY, ref)
            .accept(MediaType.APPLICATION_JSON);

        if (token != null) {
            builder.header(TvmUtils.SERVICE_TICKET_HEADER, token);
        }

        return invokeAndRetrieveResponse(builder, resultMatcher);
    }

    public TakeoutStatusDto startUidTakeout(long uid, String ref) {
        String result = invokeAndRetrieveResponse(post("/takeout/passport/UID/" + uid)
                .header(TvmUtils.SERVICE_TICKET_HEADER, MOCK_TOKEN)
                .param(REF_KEY, ref)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return parseValue(result, TakeoutStatusDto.class);
    }

    public TakeoutStatusDto startTakeoutByLink(String ref, String link) {
        String result = invokeAndRetrieveResponse(post("/takeout/passport/by/link")
                .header(TvmUtils.SERVICE_TICKET_HEADER, MOCK_TOKEN)
                .param(REF_KEY, ref)
                .content(FormatUtils.toJson(new TakeoutByLinkRequestDto(link)))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return parseValue(result, TakeoutStatusDto.class);
    }

    public TakeoutStatusDto cancelTakeout(String ref) {
        String result = invokeAndRetrieveResponse(post("/takeout/passport/cancel")
                .header(TvmUtils.SERVICE_TICKET_HEADER, MOCK_TOKEN)
                .param(REF_KEY, ref)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        return parseValue(result, TakeoutStatusDto.class);
    }

}
