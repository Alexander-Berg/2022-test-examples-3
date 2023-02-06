package ru.yandex.market.ff.repository;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.RequestHistoryEntity;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

class RequestHistoryRepositoryTest extends IntegrationTest {
    @Autowired
    private RequestHistoryRepository repository;

    @ExpectedDatabase(value = "classpath:repository/shop-request-history/after_save.xml",
            assertionMode = NON_STRICT)
    @Test
    public void repoWorks() {
        repository.save(RequestHistoryEntity.builder()
                .eventCode("myEventCode")
                .shopRequestStatusBefore("123")
                .shopRequestStatusAfter("345")
                .build());
    }
}
