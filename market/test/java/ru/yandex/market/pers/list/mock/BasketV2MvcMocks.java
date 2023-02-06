package ru.yandex.market.pers.list.mock;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.basket.model.BasketItemsDtoList;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.model.ResultDto;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;
import ru.yandex.market.pers.list.model.v2.enums.RequestPlatform;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.list.BasketClientParams.PARAM_RGB;
import static ru.yandex.market.pers.list.BasketClientParams.USER_ANY_ID;
import static ru.yandex.market.pers.list.BasketClientParams.USER_ID_TYPE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.08.2020
 */
@Service
public class BasketV2MvcMocks extends AbstractMvcMocks {
    public List<BasketReferenceItem> getItems(BasketOwner owner, MarketplaceColor color) {
        ResultDto<BasketItemsDtoList> response = parseValue(
            getItemsMvc(owner, color),
            new TypeReference<>() {
            });
        return response.getResult().getItems();
    }

    public String getItemsMvc(BasketOwner owner, MarketplaceColor color) {
        return invokeAndGet(
            get("/v2/items")
                .param(USER_ID_TYPE, owner.getUserIdType().name())
                .param(USER_ANY_ID, owner.getIdStr())
                .param(PARAM_RGB, color.getName())
                .headers(generateHeaders())
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    public int getItemsCount(BasketOwner owner, MarketplaceColor color, ResultMatcher expectedStatus) throws Exception {
        ResultDto<Integer> wrapper = parseValue(getItemsCountMvc(owner, color, expectedStatus),
            new TypeReference<>() {
        });
        return wrapper.getResult();
    }

    public String getItemsCountMvc(BasketOwner owner, MarketplaceColor color, ResultMatcher expectedStatus) throws Exception {
        return mockMvc
            .perform(get("/v2/items/count")
                .headers(generateHeaders())
                .param(USER_ID_TYPE, owner.getUserIdType().name())
                .param(USER_ANY_ID, owner.getIdStr())
                .param(PARAM_RGB, color.getName())
            )
            .andDo(print())
            .andExpect(expectedStatus)
            .andReturn().getResponse().getContentAsString();
    }

    public String deleteItem(long itemId, BasketOwner owner, MarketplaceColor color, ResultMatcher expectedStatus) throws Exception {
        return mockMvc
            .perform(delete("/v2/items/" + itemId)
                .headers(generateHeaders())
                .param(USER_ID_TYPE, owner.getUserIdType().name())
                .param(USER_ANY_ID, owner.getIdStr())
                .param(PARAM_RGB, color.getName())
            )
            .andDo(print())
            .andExpect(expectedStatus)
            .andReturn().getResponse().getContentAsString();
    }

    private static HttpHeaders generateHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-platform", RequestPlatform.WEB.getName());
        headers.set("x-market-req-id", "1");
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return headers;
    }
}
