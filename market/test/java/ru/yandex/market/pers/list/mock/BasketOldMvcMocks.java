package ru.yandex.market.pers.list.mock;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.list.WebParams;
import ru.yandex.market.pers.list.model.BasketItem;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.UserList;
import ru.yandex.market.pers.test.common.AbstractMvcMocks;
import ru.yandex.market.util.ExecUtils;
import ru.yandex.market.util.FormatUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.08.2020
 */
@Service
public class BasketOldMvcMocks extends AbstractMvcMocks {
    public List<BasketItem> addItems(BasketOwner owner, List<BasketItem> items) {
        return parseValue(addItemsMvc(owner, items),
            new TypeReference<>() {
            });
    }

    public String addItemsMvc(BasketOwner owner, List<BasketItem> items) {
        return addItemsMvc(owner, items, x -> x);
    }

    public String addItemsMvc(BasketOwner owner,
                              List<BasketItem> items,
                              Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> fun) {
        return invokeAndGet(fun.apply(
            post(String
                .format("/users/%s/%s/wishlist", owner.getUserIdType().name(), owner.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(FormatUtils.toJson(items))),
            status().is2xxSuccessful());
    }

    public String deleteItemsMvc(BasketOwner owner, List<Long> items) {
        String itemsStr = items.stream().map(String::valueOf).collect(Collectors.joining(","));
        return invokeAndGet(
            delete(String.format("/users/%s/%s/wishlist/%s", owner.getUserIdType().name(), owner.getId(), itemsStr))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful()
        );
    }

    public String mergeItemsMvc(BasketOwner ownerFrom, BasketOwner ownerTo) {
        return mergeItemsMvc(ownerFrom, ownerTo, status().is2xxSuccessful());
    }

    public String mergeItemsMvc(BasketOwner ownerFrom, BasketOwner ownerTo, ResultMatcher resultMatcher) {
        return invokeAndGet(
            patch(String.format("/users/%s/%s/wishlist", ownerFrom.getUserIdType().name(), ownerFrom.getId()))
                .param("typeTo", ownerTo.getUserIdType().name())
                .param("idTo", ownerTo.getIdStr())
                .contentType(MediaType.APPLICATION_JSON),
            resultMatcher
        );
    }

    public String syncItemsMvc(BasketOwner owner, List<BasketItem> items) {
        return syncItemsMvc(owner, new UserList(items, List.of()));
    }

    public UserList syncItems(BasketOwner owner, UserList userList) {
        return parseValue(syncItemsMvc(owner, userList), new TypeReference<>() {
        });
    }

    public String syncItemsMvc(BasketOwner owner, UserList userList) {
        return invokeAndGet(
            post(String
                .format("/users/%s/%s", owner.getUserIdType().name(), owner.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(FormatUtils.toJson(userList)),
            status().is2xxSuccessful());
    }

    public List<BasketItem> getItems(BasketOwner owner) {
        return parseValue(getItemsMvc(owner), new TypeReference<>() {
        });
    }

    public String getItemsMvc(BasketOwner owner) {
        return invokeAndGet(
            get(String.format("/users/%s/%s/wishlist", owner.getUserIdType().name(), owner.getId()))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    public int getItemsCount(BasketOwner owner) {
        Mutable<String> countRef = new MutableObject<>();
        try {
            mockMvc.perform(
                head(String.format("/users/%s/%s/wishlist", owner.getUserIdType().name(), owner.getId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andDo(result -> countRef.setValue(result.getResponse().getHeader(WebParams.HTTP_HEADER_ITEMS_COUNT)))
                .andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw ExecUtils.silentError(e);
        }

        return Integer.parseInt(countRef.getValue());
    }

    public UserList getUserList(BasketOwner owner) {
        return parseValue(getUserListMvc(owner), new TypeReference<>() {
        });
    }

    public String getUserListMvc(BasketOwner owner) {
        return invokeAndGet(
            get(String.format("/users/%s/%s", owner.getUserIdType().name(), owner.getId()))
                .contentType(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    public List<BasketItem> getExistingItems(BasketOwner owner, List<BasketItem> items) {
        return parseValue(getExistingItemsMvc(owner, items),
            new TypeReference<>() {
            });
    }

    public String getExistingItemsMvc(BasketOwner owner, List<BasketItem> items) {
        return invokeAndGet(
            post(String
                .format("/users/%s/%s/wishlist/existing", owner.getUserIdType().name(), owner.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(FormatUtils.toJson(items)),
            status().is2xxSuccessful());
    }

}
