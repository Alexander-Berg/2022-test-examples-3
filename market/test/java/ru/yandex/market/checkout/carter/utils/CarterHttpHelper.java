package ru.yandex.market.checkout.carter.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.utils.serialization.TestSerializationService;
import ru.yandex.market.checkout.carter.web.CartListViewModel;
import ru.yandex.market.checkout.carter.web.CartViewModel;
import ru.yandex.market.checkout.carter.web.ItemOfferViewModel;
import ru.yandex.market.checkout.carter.web.ResultViewModel;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.HitRateGroup.UNLIMIT;

/**
 * @author Kirill Khromov
 * date: 15/02/2018
 */

public class CarterHttpHelper {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestSerializationService testSerializationService;

    public void cleanCart(UserIdType userIdType, String userId) throws Exception {
        cleanCart(userIdType, userId, Color.BLUE);
    }

    public void cleanCart(UserIdType userIdType, String userId, Color color) throws Exception {
        CartListViewModel cartList = getList(userIdType, userId, color, status().isOk());
        if (Boolean.FALSE.equals(cartList.getItems().isEmpty())) {
            Long listId = cartList.getId();
            deleteItems(userIdType, userId, listId, cartList.getItems().stream()
                    .map(ItemOfferViewModel::getId)
                    .collect(Collectors.toList()), color, status().isOk());
        }
    }

    public CartListViewModel getList(UserIdType userIdType, String userId, ResultMatcher resultMatcher)
            throws Exception {
        return getList(userIdType, userId, Color.BLUE, resultMatcher);
    }

    public CartListViewModel getList(UserIdType userIdType, String userId, Color color, ResultMatcher resultMatcher)
            throws Exception {
        MvcResult response = mockMvc.perform(
                get(String.format("/cart/%s/%s/list", userIdType, userId))
                        .param("rgb", color.name())
        )
                .andExpect(resultMatcher)
                .andReturn();

        ResultViewModel<CartViewModel> result =
                testSerializationService.deserializeCarterObjectWithOm(response.getResponse()
                .getContentAsString());
        CartViewModel cart = result.getResult();
        return cart.getLists().get(0);
    }

    public Long postItem(UserIdType userIdType, String userId, Long listId, ItemOffer offer,
                         ResultMatcher resultMatcher) throws Exception {
        return postItem(userIdType, userId, listId, offer, Color.BLUE, resultMatcher);
    }

    public Long postItem(UserIdType userIdType, String userId, Long listId, ItemOffer offer, Color color, ResultMatcher
            resultMatcher) throws Exception {
        String offerString = testSerializationService.serializeCarterObject(CartConverter.convert(offer));
        MvcResult response = mockMvc.perform(
                post(String.format("/cart/%s/%s/list/%d/item", userIdType, userId, listId))
                        .param("userGroup", UNLIMIT.name())
                        .param("rgb", color.name())
                        .contentType(APPLICATION_JSON_UTF8)
                        .content(offerString)
        )
                .andExpect(resultMatcher)
                .andReturn();

        ResultViewModel itemId = testSerializationService.deserializeCarterObject(response.getResponse()
                .getContentAsString(), ResultViewModel.class);
        return (Long) itemId.getResult();
    }

    public void putItem(UserIdType userIdType, String userId, Long listId, Long itemId, Integer newCount,
                        ResultMatcher resultMatcher) throws Exception {
        putItem(userIdType, userId, listId, itemId, newCount, Color.BLUE, resultMatcher);
    }

    public void putItem(UserIdType userIdType, String userId, Long listId, Long itemId, Integer newCount, Color color,
                        ResultMatcher resultMatcher) throws Exception {
        if (newCount > 0) {
            mockMvc.perform(
                    put(String.format("/cart/%s/%s/list/%d/item/%d", userIdType, userId, listId, itemId))
                            .param("userGroup", UNLIMIT.name())
                            .param("count", newCount.toString())
                            .param("rgb", color.name())
            )
                    .andExpect(resultMatcher)
                    .andReturn();

        } else {
            mockMvc.perform(
                    put(String.format("/cart/%s/%s/list/%d/item/%d", userIdType, userId, listId, itemId))
                            .param("userGroup", UNLIMIT.name())
                            .param("count", newCount.toString())
                            .param("rgb", color.name())

            )
                    .andExpect(resultMatcher)
                    .andExpect(jsonPath("$.message").value("Item count parameter should exist"))
                    .andReturn();
        }
    }

    public void postItems(UserIdType userIdType, String userId, Long listId, List<String> items, ResultMatcher
            resultMatcher) throws Exception {
        postItems(userIdType, userId, listId, items, Color.BLUE, resultMatcher);
    }

    public void postItems(UserIdType userIdType, String userId, Long listId, List<String> items, Color color,
                          ResultMatcher
            resultMatcher) throws Exception {
        mockMvc.perform(
                post(String.format("/cart/%s/%s/list/%d/items", userIdType, userId, listId))
                        .param("userGroup", UNLIMIT.name())
                        .param("rgb", color.name())
                        .contentType(APPLICATION_JSON_UTF8)
                        .content("{\"listType\": \"BASKET\", \"items\" : " + items.toString() + "}")
        )
                .andExpect(resultMatcher)
                .andReturn();
    }


    public void updateItems(UserIdType userIdType, String userId, Long listId, List<String> items, ResultMatcher
            resultMatcher) throws Exception {
        updateItems(userIdType, userId, listId, items, Color.BLUE, resultMatcher);
    }

    public void updateItems(UserIdType userIdType, String userId, Long listId, List<String> items, Color color,
                            ResultMatcher
            resultMatcher) throws Exception {
        mockMvc.perform(
                patch(String.format("/cart/%s/%s/list/%d/items", userIdType, userId, listId))
                        .param("userGroup", UNLIMIT.name())
                        .param("rgb", color.name())
                        .contentType(APPLICATION_JSON_UTF8)
                        .content("{\"listType\": \"BASKET\", \"items\" : " + items.toString() + "}")
        )
                .andExpect(resultMatcher)
                .andReturn();
    }


    public void updateCart(UserIdType userIdType, String userId, Long listId, List<String> items, ResultMatcher
            resultMatcher) throws Exception {
        updateCart(userIdType, userId, listId, items, Color.BLUE, resultMatcher);
    }

    public void updateCart(UserIdType userIdType, String userId, Long listId, List<String> items, Color color,
                           ResultMatcher
            resultMatcher) throws Exception {
        mockMvc.perform(
                put(String.format("/cart/%s/%s/list/%d", userIdType, userId, listId))
                        .param("userGroup", UNLIMIT.name())
                        .param("rgb", color.name())
                        .contentType(APPLICATION_JSON_UTF8)
                        .content("{\"listType\": \"BASKET\", \"items\" : " + items.toString() + "}")
        )
                .andExpect(resultMatcher)
                .andReturn();
    }

    public void deleteItem(UserIdType userIdType, String userId, Long listId, Long itemId, ResultMatcher
            resultMatcher) throws Exception {
        deleteItem(userIdType, userId, listId, itemId, Color.BLUE, resultMatcher);
    }

    public void deleteItem(UserIdType userIdType, String userId, Long listId, Long itemId, Color color, ResultMatcher
            resultMatcher) throws Exception {
        mockMvc.perform(
                delete(String.format("/cart/%s/%s/list/%d/item/%d", userIdType, userId, listId, itemId))
                        .param("userGroup", UNLIMIT.name())
                        .param("rgb", color.name())
        )
                .andExpect(resultMatcher)
                .andReturn();
    }

    public void deleteItems(UserIdType userIdType, String userId, Long listId, List<Long> itemsIds, ResultMatcher
            resultMatcher) throws Exception {
        deleteItems(userIdType, userId, listId, itemsIds, Color.BLUE, resultMatcher);
    }

    public void deleteItems(UserIdType userIdType, String userId, Long listId, List<Long> itemsIds, Color color,
                            ResultMatcher
            resultMatcher) throws Exception {
        mockMvc.perform(
                delete(String.format("/cart/%s/%s/list/%d/item", userIdType, userId, listId))
                        .param("userGroup", UNLIMIT.name())
                        .param("rgb", color.name())
                        .contentType(APPLICATION_JSON_UTF8)
                        .content(itemsIds.toString())
        )
                .andExpect(resultMatcher)
                .andReturn();
    }

    public void patchList(UserIdType userIdTypeFrom, String userIdFrom, UserIdType userIdTypeTo, String userIdTo,
                          ResultMatcher resultMatcher) throws Exception {
        patchList(userIdTypeFrom, userIdFrom, userIdTypeTo, userIdTo, Color.BLUE, resultMatcher);
    }

    public void patchList(UserIdType userIdTypeFrom, String userIdFrom, UserIdType userIdTypeTo, String userIdTo,
                          Color color,
                          ResultMatcher resultMatcher) throws Exception {
        mockMvc.perform(
                patch(String.format("/cart/%s/%s/list", userIdTypeFrom, userIdFrom))
                        .param("typeTo", userIdTypeTo.toString())
                        .param("idTo", userIdTo)
                        .param("rgb", color.name())
        )
                .andExpect(resultMatcher)
                .andReturn();
    }

    public ResultViewModel getSearch(Long from, Long to, ResultMatcher resultMatcher) throws Exception {
        MvcResult response = mockMvc.perform(
                get("/cart/search")
                        .param("fromTs", from.toString())
                        .param("toTs", to.toString())
        )
                .andExpect(resultMatcher)
                .andReturn();

        return testSerializationService.deserializeCarterObject(response.getResponse()
                .getContentAsString(), ResultViewModel.class);
    }

    public ResultViewModel getSearchOwners(Long from, Long to, ResultMatcher resultMatcher) throws Exception {
        MvcResult response = mockMvc.perform(
                get("/cart/search-owners")
                        .param("fromTs", from.toString())
                        .param("toTs", to.toString())
        )
                .andExpect(resultMatcher)
                .andReturn();

        return testSerializationService.deserializeCarterObject(response.getResponse()
                .getContentAsString(), ResultViewModel.class);
    }
}
