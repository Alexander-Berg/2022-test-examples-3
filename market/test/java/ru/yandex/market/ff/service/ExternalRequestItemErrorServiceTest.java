package ru.yandex.market.ff.service;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.ExternalRequestItemErrorSource;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

public class ExternalRequestItemErrorServiceTest extends IntegrationTest {

    @Autowired
    private ExternalRequestItemErrorService service;

    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @Test
    void findItemErrorsWithFetchedAttributesQueriesCountTest() {
        service.findByItemIdsAndSource(Set.of(2L, 3L, 4L, 5L), ExternalRequestItemErrorSource.MBO);
    }

    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @Test
    void findByItemIdWithFetchedServiceIdsQueriesCountTest() {
        service.findByItemIdWithFetchedServiceIds(Set.of(2L, 3L, 4L, 5L));
    }

    @JpaQueriesCount(2)
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @Test
    void findByRequestIdWithFetchedServiceIdsQueriesCountTest() {
        service.findByRequestIdWithFetchedServiceIds(2L);
    }

}
