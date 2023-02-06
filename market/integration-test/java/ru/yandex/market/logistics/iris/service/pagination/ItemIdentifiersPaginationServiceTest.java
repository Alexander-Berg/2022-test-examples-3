package ru.yandex.market.logistics.iris.service.pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;

public class ItemIdentifiersPaginationServiceTest extends AbstractContextualTest {

    @Autowired
    private ItemIdentifiersPaginationService service;

    /**
     * Проверяем, что на пустой БД
     * Размер страницы = 1
     * Счетчик вызовов = 0
     * Регистр значений = пустой.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_identifier_pagination_service/empty.xml")
    public void forEachPageOnEmptyDatabase() {
        TestCountingConsumer testCounter = new TestCountingConsumer();
        service.forEachPage(testCounter, "1", 1);

        assertions().assertThat(testCounter.calls).isEqualByComparingTo(0);
        assertions().assertThat(testCounter.values).isEmpty();
    }

    /**
     * Проверяем, что на не пустой БД
     * Размер страницы = 1
     * Счетчик вызовов = кол-во записей / размер страницы. (2 вызова)
     * Регистр значений  = все значения из БД в нужном порядке. (2 значения)
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_identifier_pagination_service/filled.xml")
    public void forEachPageOnFilledDatabaseWithSingleSizedBatch() {
        TestCountingConsumer testCounter = new TestCountingConsumer();
        service.forEachPage(testCounter, "2", 1);

        assertions().assertThat(testCounter.calls).isEqualByComparingTo(2);

        assertions().assertThat(testCounter.values).containsExactly(
            new EmbeddableItemIdentifier("2", "1"),
            new EmbeddableItemIdentifier("2", "2")
        );
    }

    private static final class TestCountingConsumer implements Consumer<List<EmbeddableItemIdentifier>> {
        int calls = 0;
        List<EmbeddableItemIdentifier> values = new ArrayList<>();

        @Override
        public void accept(List<EmbeddableItemIdentifier> embeddableItemIdentifiers) {
            calls++;
            values.addAll(embeddableItemIdentifiers);
        }
    }
}
