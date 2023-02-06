package ru.yandex.market.logistics.iris.repository.pagination;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;

public class ItemIdentifierPaginationRepositoryTest extends AbstractContextualTest {

    @Autowired
    private ItemIdentifierPaginationRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Проверяем, что при выборке первых N строк на пустой базе будет возвращен пустой список.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/empty.xml")
    public void fetchFirstOnEmptyDatabase() {
        List<EmbeddableItemIdentifier> result = transactionTemplate.execute(tx ->
            repository.fetch(500, "1")
        );

        assertions().assertThat(result).isEmpty();
    }

    /**
     * Проверяем, что при выборке следующих N строк (начиная со случайного значения)
     * на пустой базе будет возвращен пустой список.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/empty.xml")
    public void fetchNextWithGivenPartnerIdOnEmptyDatabase() {
        final String start = "1";

        List<EmbeddableItemIdentifier> result = transactionTemplate.execute(tx ->
            repository.fetchNextWithGivenPartnerId(500, new EmbeddableItemIdentifier(start, start))
        );

        assertions().assertThat(result).isEmpty();
    }

    /**
     * Проверяем, что выборка первых N записей на заполненной базе вернет нужное кол-во записей
     * в корректном (лексикографическом) порядке.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/lexicographical_order_filled.xml")
    public void fetchFirstOnFilledDatabase() {
        List<EmbeddableItemIdentifier> identifiers = transactionTemplate.execute(tx ->
            repository.fetch(6, "2")
        );

        assertions().assertThat(identifiers).containsExactly(
            new EmbeddableItemIdentifier("2", "1"),
            new EmbeddableItemIdentifier("2", "2")
        );
    }

    /**
     * Проверяем, что выборка следующих N записей после указанной на заполненной базе вернет нужное кол-во записей
     * в корректном (лексикографическом) порядке.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/lexicographical_order_filled.xml")
    public void fetchNextWithGivenPartnerIdOnFilledDatabaseWithSmallPageSize() {
        List<EmbeddableItemIdentifier> identifiers = transactionTemplate.execute(tx ->
            repository.fetchNextWithGivenPartnerId(1, new EmbeddableItemIdentifier("2", "0"))
        );

        assertions().assertThat(identifiers).containsExactly(
            new EmbeddableItemIdentifier("2", "1")
        );
    }

    /**
     * Проверяем, что выборка следующих N записей после указанной на заполненной базе вернет нужное кол-во записей
     * в корректном (лексикографическом) порядке.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/lexicographical_order_filled.xml")
    public void fetchNextWithGivenPartnerIdOnFilledDatabaseWithBigPageSize() {
        List<EmbeddableItemIdentifier> identifiers = transactionTemplate.execute(tx ->
                repository.fetchNextWithGivenPartnerId(3, new EmbeddableItemIdentifier("2", "0"))
        );

        assertions().assertThat(identifiers).containsExactly(
                new EmbeddableItemIdentifier("2", "1"),
                new EmbeddableItemIdentifier("2", "2")
        );
    }

    /**
     * Проверяем, что выборка следующих N записей после указанной на заполненной базе вернет нужное кол-во записей
     * в корректном (лексикографическом) порядке.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/lexicographical_order_filled.xml")
    public void fetchNextWithGivenPartnerIdOnFilledDatabaseWithSkuFilter() {
        List<EmbeddableItemIdentifier> identifiers = transactionTemplate.execute(tx ->
                repository.fetchNextWithGivenPartnerId(3, new EmbeddableItemIdentifier("2", "1"))
        );

        assertions().assertThat(identifiers).containsExactly(
                new EmbeddableItemIdentifier("2", "2")
        );
    }
}
