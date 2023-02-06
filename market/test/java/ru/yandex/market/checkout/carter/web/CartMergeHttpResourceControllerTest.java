package ru.yandex.market.checkout.carter.web;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.carter.report.ReportMockConfigurer;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.utils.CarterHttpHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;

/**
 * @author Kirill Khromov
 * date: 15/02/2018
 */

public class CartMergeHttpResourceControllerTest extends CarterMockedDbTestBase {

    private static final String USER_ID = "777";
    private static final String UUID_STR = "ffffuuuuu";
    private static final String FAKE_USER_ID = "5875984795734";
    private static final ItemOffer OFFER = generateItem("test");
    private static final ResultMatcher OK = status().isOk();

    private UserIdType userIdTypeFrom;
    private String userIdFrom;
    private UserIdType userIdTypeTo;
    private String userIdTo;

    @Autowired
    private CarterHttpHelper carterHttpHelper;
    @Autowired
    private ReportMockConfigurer reportMockConfigurer;


    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(new Object[][]{
                //TODO https://st.yandex-team.ru/MARKETCHECKOUT-5509 - внести правки когда починят
                {UserIdType.UID, USER_ID, UserIdType.UUID, UUID_STR},
                {UserIdType.UUID, UUID_STR, UserIdType.UUID, UUID_STR + 1},
                {UserIdType.YANDEXUID, FAKE_USER_ID, UserIdType.UUID, UUID_STR},
                {UserIdType.UID, USER_ID, UserIdType.YANDEXUID, FAKE_USER_ID},
                {UserIdType.UUID, UUID_STR, UserIdType.YANDEXUID, FAKE_USER_ID},
                {UserIdType.YANDEXUID, FAKE_USER_ID, UserIdType.YANDEXUID, FAKE_USER_ID + 1},
                {UserIdType.UID, USER_ID, UserIdType.UID, USER_ID + 1},
                {UserIdType.UUID, UUID_STR, UserIdType.UID, USER_ID},
                {UserIdType.YANDEXUID, FAKE_USER_ID, UserIdType.UID, USER_ID}
        }).stream().map(Arguments::of);
    }

    @AfterEach
    public void cleanCart() throws Exception {
        reportMockConfigurer.resetMock();
        carterHttpHelper.cleanCart(userIdTypeFrom, userIdFrom);
        carterHttpHelper.cleanCart(userIdTypeTo, userIdTo);
    }

    @DisplayName("PATCH /list: мёрж синей корзины")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void patchListTestBlue(
            UserIdType userIdTypeFrom, String userIdFrom, UserIdType userIdTypeTo,
            String userIdTo
    ) throws Exception {
        this.userIdTypeFrom = userIdTypeFrom;
        this.userIdFrom = userIdFrom;
        this.userIdTypeTo = userIdTypeTo;
        this.userIdTo = userIdTo;

        reportMockConfigurer.mockReportOk(new ReportGeneratorParameters());

        CartList cartList = CartConverter.convert(carterHttpHelper.getList(userIdTypeFrom, userIdFrom, OK));
        Long listId = cartList.getId();
        carterHttpHelper.postItem(userIdTypeFrom, userIdFrom, listId, OFFER, OK);
        cartList = CartConverter.convert(carterHttpHelper.getList(userIdTypeFrom, userIdFrom, OK));
        CartItem cartItem = cartList.getItems().iterator().next();

        carterHttpHelper.patchList(userIdTypeFrom, userIdFrom, userIdTypeTo, userIdTo, OK);
        cartList = CartConverter.convert(carterHttpHelper.getList(userIdTypeFrom, userIdFrom, OK));
//        Collection<CartItem> cartItems = cartList.getItems();
//        assertTrue(cartItems.isEmpty());
        cartList = CartConverter.convert(carterHttpHelper.getList(userIdTypeTo, userIdTo, OK));
        assertEquals(cartItem, cartList.getItems().iterator().next());
    }
}
