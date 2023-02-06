package ru.yandex.market.ff.service.implementation;

import java.util.ArrayList;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class SkuArticleValidationServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private SkuArticleValidationService skuArticleValidationService;

    /**
     * Проверяет, что в текущей поставке нет проблемы с регистром article.
     */
    @Test
    @DatabaseSetup("classpath:repository/article_validation/before.xml")
    void thereAreNoProblemsInArticleList() {
        Set<Long> errorIds = skuArticleValidationService
            .findItemIdsWithArticlesDuplicatedIgnoringCaseOnlyWithinPreviousOrCurrentRequest(
                getShopRequest(4L, 100L),
                getRequestItems());

        assertThat(errorIds).hasSize(0);
    }

    /**
     * Проверяет, что в текущей поставке найдутся товары с article которые уже были сохранены ранее,
     * но в другом регистре.
     */
    @Test
    @DatabaseSetup("classpath:repository/article_validation/before.xml")
    void foundSameArticlesWithDifferentCaseInPreviousSupplies() {
        Set<Long> errorIds = skuArticleValidationService
            .findItemIdsWithArticlesDuplicatedIgnoringCaseOnlyWithinPreviousOrCurrentRequest(
                getShopRequest(5L, 101L),
                getRequestItems());

        assertions.assertThat(errorIds).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    /**
     * Проверяет, что в текущей поставке есть пары товаров c одинаковыми article, но в другом регистре.
     */
    @Test
    @DatabaseSetup("classpath:repository/article_validation/before.xml")
    void foundSameArticlesWithDifferentCaseWithinSupply() {
        Set<Long> ids = skuArticleValidationService
            .findItemIdsWithArticlesDuplicatedIgnoringCaseOnlyWithinPreviousOrCurrentRequest(
                getShopRequest(4L, 100L),
                getRequestItemsWithSameArticleWithDifferentCase());

        assertions.assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 4L);
    }

    /**
     * Проверяет, что поставка пройдет валидацию так как ранее существующие дупликаты были в поставках со
     * статусам 4, 5, 8.
     */
    @Test
    @DatabaseSetup("classpath:repository/article_validation/before.xml")
    void errorsNotFoundDueToPreviousDuplicatesWereInInvalidSupplies() {
        Set<Long> ids = skuArticleValidationService
            .findItemIdsWithArticlesDuplicatedIgnoringCaseOnlyWithinPreviousOrCurrentRequest(
                getShopRequest(5L, 101),
                getRequestItemWithSameArticleWithDifferentCase());

        assertions.assertThat(ids).hasSize(0);
    }

    private ArrayList<RequestItem> getRequestItems() {
        ArrayList<RequestItem> requestItems = new ArrayList<>();

        RequestItem requestItem = new RequestItem();
        requestItem.setId(1L);
        requestItem.setArticle("aaa");

        RequestItem requestItem2 = new RequestItem();
        requestItem2.setId(2L);
        requestItem2.setArticle("bbb");

        RequestItem requestItem3 = new RequestItem();
        requestItem3.setId(3L);
        requestItem3.setArticle("ccc");

        RequestItem requestItem4 = new RequestItem();
        requestItem4.setId(4L);
        requestItem4.setArticle("DDD");

        requestItems.add(requestItem);
        requestItems.add(requestItem2);
        requestItems.add(requestItem3);
        requestItems.add(requestItem4);
        return requestItems;
    }

    private ArrayList<RequestItem> getRequestItemsWithSameArticleWithDifferentCase() {
        ArrayList<RequestItem> requestItems = new ArrayList<>();

        RequestItem requestItem = new RequestItem();
        requestItem.setId(1L);
        requestItem.setArticle("qwerty");

        RequestItem requestItem2 = new RequestItem();
        requestItem2.setId(2L);
        requestItem2.setArticle("QWERTY");

        RequestItem requestItem3 = new RequestItem();
        requestItem3.setId(3L);
        requestItem3.setArticle("aaa");

        RequestItem requestItem4 = new RequestItem();
        requestItem4.setId(4L);
        requestItem4.setArticle("QwErTy");

        requestItems.add(requestItem);
        requestItems.add(requestItem2);
        requestItems.add(requestItem3);
        requestItems.add(requestItem4);
        return requestItems;
    }

    private ArrayList<RequestItem> getRequestItemWithSameArticleWithDifferentCase() {
        ArrayList<RequestItem> requestItems = new ArrayList<>();

        RequestItem requestItem = new RequestItem();
        requestItem.setId(1L);
        requestItem.setArticle("aRtaRt");

        requestItems.add(requestItem);

        return requestItems;
    }

    private ShopRequest getShopRequest(long id, long serviceId) {
        ShopRequest shopRequest = new ShopRequest();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        shopRequest.setSupplier(supplier);
        shopRequest.setServiceId(serviceId);
        return shopRequest;
    }
}
