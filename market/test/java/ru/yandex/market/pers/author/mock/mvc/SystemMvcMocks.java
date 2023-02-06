package ru.yandex.market.pers.author.mock.mvc;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 04.03.2020
 */
@Service
public class SystemMvcMocks extends AbstractMvcMocks {

    public String getPageMatcher() throws Exception {
        return getPageMatcher(status().is2xxSuccessful());
    }

    public String getPageMatcher(ResultMatcher resultMatcher) throws Exception {
       return invokeAndRetrieveResponse(get("/pagematch").accept(MediaType.APPLICATION_JSON), resultMatcher);
    }
}
