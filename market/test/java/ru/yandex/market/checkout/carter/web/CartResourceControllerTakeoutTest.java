package ru.yandex.market.checkout.carter.web;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.report.ReportMockConfigurer;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;

import static java.lang.Long.parseLong;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;

public class CartResourceControllerTakeoutTest extends CarterMockedDbTestBase {

    private static final String OFFER = "some_offer";

    @Autowired
    private Carter carterClient;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ReportMockConfigurer reportMockConfigurer;

    private UserContext uidContext;

    @BeforeEach
    public void setUp() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        uidContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1,
                Long.MAX_VALUE)));
    }

    @AfterEach
    public void clear() {
        reportMockConfigurer.resetMock();
    }


    @Test
    public void takeoutHandleClientTest() {
        CartListViewModel blueList = getCartList(uidContext.getUserAnyId(), Color.BLUE);
        addItem(blueList, Color.BLUE, CartConverter.convert(generateItem(OFFER, 1)));

        reportMockConfigurer.mockReportOk();

        List<CartListViewModel> carts = carterClient.getCartTakeout(parseLong(uidContext.getUserAnyId()), Color.BLUE);

        assertThat(carts, hasSize(1));
        Collection<ItemOfferViewModel> items = carts.get(0).getItems();
        assertThat(items, hasSize(1));
        ItemOfferViewModel item = items.iterator().next();
        assertThat(item.getName(), is(OFFER));
        assertThat(item.getObjId(), nullValue());
        assertThat(item.getObjType(), nullValue());
    }

    @Test
    public void takeoutHandleTest() throws Exception {
        CartListViewModel blueList = getCartList(uidContext.getUserAnyId(), Color.BLUE);
        addItem(blueList, Color.BLUE, CartConverter.convert(generateItem(OFFER, 1)));

        reportMockConfigurer.mockReportOk();
        String resp = mockMvc.perform(get("/cart/" + uidContext.getUserAnyId() + "/takeout")
                .param("rgb", "BLUE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        assertCartList(resp, OFFER, 1);
    }

    @Test
    public void takeoutHandleEmptyCartsTest() throws Exception {
        reportMockConfigurer.mockReportOk();
        mockMvc.perform(get("/cart/" + uidContext.getUserAnyId() + "/takeout")
                .param("rgb", "BLUE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("[]")));

    }

    private void addItem(CartListViewModel list, Color rgb, ItemOfferViewModel item) {
        carterClient.addItem(uidContext.getUserAnyId(), UserIdType.UID, list.getId(), rgb, item, "");
    }

    private CartListViewModel getCartList(String userId, Color rgb) {
        CartViewModel cart = carterClient.getCart(userId, UserIdType.UID, rgb);
        return cart.getLists().get(0);
    }

    private void assertCartList(String resp, String name, int count) {
        assertThat(resp, containsString("\"name\":\"" + name + "\""));
        assertThat(resp, containsString("\"count\":" + count));
        assertThat(resp, containsString("shopId"));
        assertThat(resp, not(containsString("objId")));
//        assertThat(resp, not(containsString("listType")));
        assertThat(resp, not(containsString("objType")));
    }
}
