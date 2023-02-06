package ru.yandex.market.tpl.core.domain.order;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.junit.jupiter.api.Assertions.assertTrue;


@RequiredArgsConstructor
public class SenderWithoutRepositoryTest extends TplAbstractTest {
    private final SenderWithoutExtIdRepository repository;
    private final TransactionTemplate tt;

    @Test
    void shouldFindByYandexId() {
        tt.execute(action -> {
            String yndxId = "qwer1234";
            var s = new SenderWithoutExtId(yndxId);
            repository.save(s);
            assertTrue(repository.existsByYandexId(yndxId));
            return 0;
        });
    }
}
