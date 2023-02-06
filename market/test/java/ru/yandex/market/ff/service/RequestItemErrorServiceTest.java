package ru.yandex.market.ff.service;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

public class RequestItemErrorServiceTest extends IntegrationTest {

    @Autowired
    private RequestItemErrorService service;

    @JpaQueriesCount(2)
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @Test
    void findItemErrorsWithFetchedAttributesQueriesCountTest() {
        service.findByItemIdsWithFetchedAttributes(Set.of(2L, 3L, 4L, 5L));
    }

    @JpaQueriesCount(2)
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @Test
    void findByRequestIdsWithFetchedAttributesAndServiceIdsQueriesCountTest() {
        service.findByItemIdsWithFetchedAttributesAndServiceIds(Set.of(2L, 3L, 4L, 5L));
    }

    @JpaQueriesCount(3)
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @Test
    void findByRequestIdWithFetchedAttributesAndServiceIdsQueriesCountTest() {
        service.findByRequestIdWithFetchedAttributesAndServiceIds(2L);
    }

}
