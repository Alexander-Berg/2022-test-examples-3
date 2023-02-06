package ru.yandex.market.pers.list.mock;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.basket.model.AliceEntryResponse;
import ru.yandex.market.pers.basket.model.AliceEntrySaveRequest;
import ru.yandex.market.pers.basket.model.ResultDto;
import ru.yandex.market.pers.basket.model.ResultLimit;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.v2.enums.RequestPlatform;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.list.BasketClientParams.LIMIT;
import static ru.yandex.market.pers.list.BasketClientParams.OFFSET;
import static ru.yandex.market.pers.list.BasketClientParams.USER_ANY_ID;
import static ru.yandex.market.pers.list.BasketClientParams.USER_ID_TYPE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.08.2020
 */
@Service
public class AliceMvcMocks extends AbstractMvcMocks {

    public void addItems(BasketOwner owner, List<AliceEntrySaveRequest> items) {
        addItemsMvc(owner, items, x -> x);
    }

    public String addItemsMvc(BasketOwner owner,
                              List<AliceEntrySaveRequest> items,
                              Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> fun) {
        return invokeAndGet(fun.apply(
            post("/alice/entry")
                .param(USER_ID_TYPE, owner.getUserIdType().name())
                .param(USER_ANY_ID, owner.getIdStr())
                .headers(generateHeaders())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FormatUtils.toJson(items))),
            status().is2xxSuccessful());
    }

    public String deleteItem(BasketOwner owner, long itemId) {
        return invokeAndGet(
            delete("/alice/entry/" + itemId)
                .param(USER_ID_TYPE, owner.getUserIdType().name())
                .param(USER_ANY_ID, owner.getIdStr())
                .headers(generateHeaders())
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public AliceEntryResponse getItems(BasketOwner owner) {
        return getItems(owner, null);
    }

    public AliceEntryResponse getItems(BasketOwner owner, ResultLimit resultLimit) {
        return parseValue(getItemsMvc(owner, resultLimit), new TypeReference<ResultDto<AliceEntryResponse>>() {
        }).getResult();
    }

    public String getItemsMvc(BasketOwner owner, ResultLimit resultLimit) {
        return invokeAndGet(
            get("/alice/entry")
                .param(USER_ID_TYPE, owner.getUserIdType().name())
                .param(USER_ANY_ID, owner.getIdStr())
                .param(OFFSET, resultLimit != null && resultLimit.getOffset() != null
                    ? resultLimit.getOffset().toString() : null)
                .param(LIMIT,
                    resultLimit != null && resultLimit.getLimit() != null
                        ? resultLimit.getLimit().toString() : null)
                .headers(generateHeaders())
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    public int getItemsCount(BasketOwner owner) {
        return parseValue(getItemsCountMvc(owner), new TypeReference<ResultDto<Integer>>() {
        }).getResult();
    }

    public String getItemsCountMvc(BasketOwner owner) {
        return invokeAndGet(
            get("/alice/entry/count")
                .param(USER_ID_TYPE, owner.getUserIdType().name())
                .param(USER_ANY_ID, owner.getIdStr())
                .headers(generateHeaders())
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    private static HttpHeaders generateHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-platform", RequestPlatform.WEB.getName());
        headers.set("x-market-req-id", "1");
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return headers;
    }
}
