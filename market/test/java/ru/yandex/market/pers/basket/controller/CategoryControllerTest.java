package ru.yandex.market.pers.basket.controller;

import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.controller.v2.BasketV2TestHelper;
import ru.yandex.market.pers.basket.controller.v2.CategoryGetTestRequest;
import ru.yandex.market.pers.basket.controller.v2.CategoryPostTestRequest;
import ru.yandex.market.pers.basket.model.CategoryGetResponse;
import ru.yandex.market.pers.basket.model.CategoryPostRequest;
import ru.yandex.market.pers.basket.model.CategoryPostResponse;
import ru.yandex.market.pers.basket.model.Icon;
import ru.yandex.market.pers.basket.model.Nid;
import ru.yandex.market.pers.basket.service.CategoryServiceTest;

/**
 * @author maratik
 */
public class CategoryControllerTest extends PersBasketTest {

    @Autowired
    protected BasketV2TestHelper helper;

    private final CategoryPostTestRequest categoryPostTestRequest = new CategoryPostTestRequest();
    private final CategoryGetTestRequest categoryGetTestRequest = new CategoryGetTestRequest();

    @Test
    public void addAndGetCategory() throws Exception {
        Icon iconToSave = new Icon(10, 20, "http://example.com");
        Nid nidToSave = new Nid(1, "aaa", Collections.singletonList(iconToSave));

        CategoryPostRequest categoryPostRequest = new CategoryPostRequest(Collections.singletonList(nidToSave));

        CategoryPostTestRequest postRequest = categoryPostTestRequest.clone();
        postRequest.setData(categoryPostRequest);
        CategoryPostResponse postResult = helper.addCategories(postRequest, ok);
        CategoryServiceTest.checkEntries(iconToSave, nidToSave, postResult.getNids());

        CategoryGetTestRequest getRequest = categoryGetTestRequest.clone();
        CategoryGetResponse getResult = helper.getCategories(getRequest, ok);
        CategoryServiceTest.checkEntries(iconToSave, nidToSave, getResult.getNids());
    }

}
