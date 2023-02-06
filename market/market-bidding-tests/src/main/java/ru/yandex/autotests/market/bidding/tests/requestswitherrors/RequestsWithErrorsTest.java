package ru.yandex.autotests.market.bidding.tests.requestswitherrors;

import com.jayway.restassured.http.ContentType;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.rules.BiddingApiTestsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.*;
import static ru.yandex.autotests.market.bidding.client.config.BiddingProperties.PROPS;
import static ru.yandex.autotests.market.bidding.client.main.MainInterfaceBaseRequestSpecificationBuilder.*;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_WRITE_WITH_OFFER_IDS;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * User: alkedr
 * Date: 29.09.2014
 */
@Aqua.Test(title = "Некорректные запросы")
@Features("Некорректные запросы")
@Issue("AUTOTESTMARKET-69")
public class RequestsWithErrorsTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_WRITE_WITH_OFFER_IDS);

    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();

    @Test
    @Title("Бекенд должен возвращать ошибку 401 если в запросе нет логина/пароля")
    public void requestWithoutAuthHeaderShouldReturnError401() {
        baseRequestSpec(bidding.backend.getHost(), PROPS.getMainInterfacePort())
                .get("/market/bidding/{shopId}/category-bids", shopId)
        .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    @Title("Бекенд должен возвращать ошибку 401 если логин неправильный")
    public void requestWithIncorrectLoginShouldReturnError401() {
        baseRequestSpecWithAuthWithIncorrectLogin(bidding.backend.getHost(), PROPS.getMainInterfacePort())
                .post("/market/bidding/{shopId}/id-bids", shopId)
        .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    @Title("Бекенд должен возвращать ошибку 401 если пароль неправильный")
    public void requestWithIncorrectPasswordShouldReturnError401() {
        baseRequestSpecWithAuthWithIncorrectPassword(bidding.backend.getHost(), PROPS.getMainInterfacePort())
                .post("/market/bidding/{shopId}/id-bids", shopId)
        .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    @Title("Бекенд должен возвращать ошибку 404 если ручка неправильная")
    public void requestWithIncorrectPathShouldReturnError404() {
        baseRequestSpecWithAuth(bidding.backend.getHost(), PROPS.getMainInterfacePort())
                .post("/market/bidding/{shopId}/IncorrectPath", shopId)
        .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    @Title("Бекенд должен возвращать ошибку 404 если http-метод неправильный")
    public void requestWithIncorrectMethodShouldReturnError405() {
        baseRequestSpecWithAuth(bidding.backend.getHost(), PROPS.getMainInterfacePort())
                .get("/market/bidding/{shopId}/id-bids", shopId)
        .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    @Title("Бекенд должен возвращать ошибку 415 если заголовок Content-Type не равен 'application/json'")
    public void requestWithIncorrectContentTypeShouldReturnError415() {
        baseRequestSpecWithAuth(bidding.backend.getHost(), PROPS.getMainInterfacePort())
                .contentType(ContentType.TEXT)
                .body("plain text body")
                .post("/market/bidding/{shopId}/id-bids", shopId)
        .then()
                .statusCode(SC_UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @Title("Бекенд должен возвращать ошибку 400 если в теле запроса невалидный JSON")
    public void requestWithInvalidJsonShouldReturnError400() {
        baseRequestSpecWithAuth(bidding.backend.getHost(), PROPS.getMainInterfacePort())
                .contentType(ContentType.JSON)
                .body("{\"1\": [\"2\"],}")
                .post("/market/bidding/{shopId}/id-bids", shopId)
        .then()
                .statusCode(SC_BAD_REQUEST);
    }
}
