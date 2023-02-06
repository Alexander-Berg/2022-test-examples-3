package ru.yandex.market.pers.author.mock.mvc;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.author.client.api.dto.InterestDto;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.INTEREST_ID_KEY;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 31.01.2020
 */
@Service
public class InterestMvcMocks extends AbstractMvcMocks {

    public List<Long> getUserInterests(String userId) throws Exception {
        return getUserInterests(userId, status().is2xxSuccessful());
    }

    public List<Long> getUserInterests(String userId, ResultMatcher resultMatcher) throws Exception {
        return parseValue(invokeAndRetrieveResponse(
                get("/interest/UID/" + userId)
                        .accept(MediaType.APPLICATION_JSON),
                resultMatcher
        ), new TypeReference<List<Long>>() {
        });
    }

    public void saveUserInterestsByIds(String userId, long[] interestIds) throws Exception {
        saveUserInterestsByIds(userId, interestIds, status().is2xxSuccessful());
    }

    public void saveUserInterestsByIds(String userId,
                                       long[] interestIds,
                                       ResultMatcher resultMatcher) throws Exception {
        saveUserInterestsByIds(userId,
                Arrays.stream(interestIds).mapToObj(String::valueOf).toArray(String[]::new), resultMatcher);
    }

    public void saveUserInterestsByIds(String userId,
                                       String[] interestIds,
                                       ResultMatcher resultMatcher) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/interest/UID/" + userId + "/save");
        if (interestIds != null && interestIds.length != 0) {
            requestBuilder.param(INTEREST_ID_KEY, interestIds);
        }
        invokeAndRetrieveResponse(requestBuilder.accept(MediaType.APPLICATION_JSON), resultMatcher);
    }

    public void cleanInterestsDictionary() throws Exception {
        cleanInterestsDictionary(status().is2xxSuccessful());
    }

    public void cleanInterestsDictionary(ResultMatcher resultMatcher) throws Exception {
        invokeAndRetrieveResponse(put("/interest/dictionary/reset").accept(MediaType.APPLICATION_JSON), resultMatcher);
    }

    public List<InterestDto> getAllInterests() throws Exception {
        return getAllInterests(status().is2xxSuccessful());
    }

    public List<InterestDto> getAllInterests(ResultMatcher resultMatcher) throws Exception {
        return parseValue(getAllInterestResponse(resultMatcher), new TypeReference<List<InterestDto>>() {
        });
    }

    public String getAllInterestResponse(ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
                get("/interest/dictionary").accept(MediaType.APPLICATION_JSON),
                resultMatcher
        );
    }
}
