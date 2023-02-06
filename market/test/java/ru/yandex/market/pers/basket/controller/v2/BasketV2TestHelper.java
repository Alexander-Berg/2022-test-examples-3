package ru.yandex.market.pers.basket.controller.v2;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.service.AccountsService;
import ru.yandex.market.pers.basket.service.BasketService;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.UserIdType;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.list.BasketClientParams.LIMIT;
import static ru.yandex.market.pers.list.BasketClientParams.OFFSET;
import static ru.yandex.market.pers.list.BasketClientParams.PAGE;
import static ru.yandex.market.pers.list.BasketClientParams.PAGE_SIZE;
import static ru.yandex.market.pers.list.BasketClientParams.PARAM_RGB;
import static ru.yandex.market.pers.list.BasketClientParams.REGION_ID;
import static ru.yandex.market.pers.list.BasketClientParams.USER_ANY_ID;
import static ru.yandex.market.pers.list.BasketClientParams.USER_ID_TYPE;

/**
 * @author ifilippov5
 */
@Component
public class BasketV2TestHelper {

    protected ResultMatcher ok = status().isOk();

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new Jdk8Module())
        .registerModule(new ParameterNamesModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private BasketService basketService;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private MockMvc mockMvc;

    public PersBasketResponse.Result getItems(BasketV2TestRequest commonRequest, ResultMatcher expectedStatus) throws Exception {
        BasketV2GetTestRequest request = (BasketV2GetTestRequest)commonRequest;
        MultiValueMap<String, String> refs = new LinkedMultiValueMap<>();
        for (BasketV2PostTestRequest.BasketRefItem item : request.getRefs()) {
            refs.add(item.getReferenceType().getName(), item.getReferenceId());
        }
        String response = mockMvc
            .perform(get("/v2/items")
                .headers(createHeaders(request))
                .param(USER_ID_TYPE, request.getUserIdType())
                .param(USER_ANY_ID, request.getUserAnyId())
                .param(PARAM_RGB, request.getRgb())
                .param(PAGE, request.getPage())
                .param(PAGE_SIZE, request.getPageSize())
                .param(LIMIT, request.getLimit())
                .param(OFFSET, request.getOffset())
                .params(refs)
            )
            .andDo(print())
            .andExpect(expectedStatus)
            .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readValue(response, PersBasketResponse.class).result;
    }

    public BasketReferenceItem addItem(BasketV2TestRequest commonRequest,
                                       ResultMatcher expectedStatus) throws Exception {
        BasketV2PostTestRequest request = (BasketV2PostTestRequest) commonRequest;
        String response = mockMvc
            .perform(post("/v2/items")
                .headers(createHeaders(request))
                .param(USER_ID_TYPE, request.getUserIdType())
                .param(USER_ANY_ID, request.getUserAnyId())
                .param(PARAM_RGB, request.getRgb())
                .param(REGION_ID, request.getRegionId() == null ? null : String.valueOf(request.getRegionId()))
                .content(OBJECT_MAPPER.writeValueAsBytes(request.getItem()))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(expectedStatus)
            .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readValue(response, PersBasketPostResponse.class).result;
    }

    public void deleteItem(BasketV2TestRequest commonRequest, ResultMatcher expectedStatus) throws Exception {
        tryToAuthorizeUser(commonRequest);
        BasketV2DeleteTestRequest request = (BasketV2DeleteTestRequest)commonRequest;
        mockMvc
            .perform(delete("/v2/items/" + String.valueOf(request.getItemId()))
                .headers(createHeaders(request))
                .param(USER_ID_TYPE, request.getUserIdType())
                .param(USER_ANY_ID, request.getUserAnyId())
                .param(PARAM_RGB, request.getRgb())
            )
            .andExpect(expectedStatus);
    }

    public ru.yandex.market.pers.basket.model.CategoryPostResponse addCategories(
        BasketV2TestRequest commonRequest, ResultMatcher expectedStatus
    ) throws Exception {
        CategoryPostTestRequest request =
            (CategoryPostTestRequest) commonRequest;
        String response = mockMvc
            .perform(post("/favourite/nids")
                .headers(createHeaders(request))
                .param(USER_ID_TYPE, request.getUserIdType())
                .param(USER_ANY_ID, request.getUserAnyId())
                .param(PARAM_RGB, request.getRgb())
                .content(OBJECT_MAPPER.writeValueAsBytes(request.getData()))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(expectedStatus)
            .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readValue(response, CategoryPostResponse.class).result;
    }

    public ru.yandex.market.pers.basket.model.CategoryGetResponse getCategories(
        BasketV2TestRequest commonRequest, ResultMatcher expectedStatus
    ) throws Exception {
        CategoryGetTestRequest request = (CategoryGetTestRequest) commonRequest;
        String response = mockMvc
            .perform(get("/favourite/nids")
                .headers(createHeaders(request))
                .param(USER_ID_TYPE, request.getUserIdType())
                .param(USER_ANY_ID, request.getUserAnyId())
                .param(PARAM_RGB, request.getRgb())
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(expectedStatus)
            .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readValue(response, CategoryGetResponse.class).result;
    }

    private static HttpHeaders createHeaders(BasketV2TestRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-platform", request.getPlatformHeader());
        headers.set("x-market-req-id", request.getReqIdHeader());
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return headers;
    }

    public Long tryToAuthorizeUser(BasketV2TestRequest basketRequest) {
        try {
            UserIdType userIdType = UserIdType.valueOf(basketRequest.getUserIdType());
            String userAnyId = basketRequest.getUserAnyId();
            BasketOwner owner = BasketOwner.from(userIdType, userAnyId);
            return basketService.getOrAddOwnerId(owner);
        } catch (Exception e) {
            return null;
        }
    }

    public BasketReferenceItem addItem(BasketV2PostTestRequest request, BasketV2PostTestRequest.BasketRefItem item) throws Exception {
        // only for tests, simpliest solution
        MarketplaceColor color = item.getReferenceType() == ReferenceType.FEED_GROUP_ID_HASH
            ? MarketplaceColor.RED
            : MarketplaceColor.BLUE;
        request.setRgb(color.getName());
        request.setItem(item);
        BasketReferenceItem refItem = addItem(request, ok);
        item.setAddedAt(refItem.getAddedAt());
        return refItem;
    }

    public List<BasketV2PostTestRequest.BasketRefItem> getItems(BasketV2GetTestRequest getRequest) throws Exception {
        return getItems(getRequest, ok)
            .getItems()
            .stream()
            .map(BasketV2PostTestRequest.BasketRefItem::from)
            .collect(Collectors.toList());
    }

    public static class PersBasketResponse {

        @JsonProperty
        private PersBasketResponse.Result result;

        public static class Result {

            @JsonProperty
            private int total;
            @JsonProperty
            private List<BasketReferenceItem> items;

            public int getTotal() {
                return total;
            }

            public List<BasketReferenceItem> getItems() {
                return items;
            }

        }

    }

    public static class PersBasketPostResponse {

        @JsonProperty
        private BasketReferenceItem result;

    }

    public static class CategoryGetResponse {
        @JsonProperty
        private ru.yandex.market.pers.basket.model.CategoryGetResponse result;
    }

    public static class CategoryPostResponse {
        @JsonProperty
        private ru.yandex.market.pers.basket.model.CategoryPostResponse result;
    }

    private String value(Object object) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(object);
    }

}
