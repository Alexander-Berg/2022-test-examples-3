package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.OutboundHistorySyncQueueItem;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class OutboundHistorySyncQueueRepositoryTest extends RepositoryTest {

    @Autowired
    private OutboundHistorySyncQueueRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/outbound_history_queue/1/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/outbound_history_queue/1/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void pushOnEmptyDatabase() {
        repository.push(new OutboundHistorySyncQueueItem(
            "1",
            LocalDateTime.of(1970, 1, 1, 0, 0)
        ));
    }

    @Test
    @DatabaseSetup("classpath:repository/outbound_history_queue/2/state.xml")
    @ExpectedDatabase(value = "classpath:repository/outbound_history_queue/2/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void pushOnFilledDatabase() {
        repository.push(
            new OutboundHistorySyncQueueItem(
                "1",
                LocalDateTime.of(2000, 1, 1, 0, 0)
            )
        );
    }

    @Test
    @DatabaseSetup("classpath:repository/outbound_history_queue/3/setup.xml")
    void pullOnEmptyDatabase() {
        Optional<OutboundHistorySyncQueueItem> item = repository.pull();

        softly.assertThat(item)
            .as("Assert that item is missing")
            .isEmpty();
    }

    @Test
    @DatabaseSetup("classpath:repository/outbound_history_queue/4/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/outbound_history_queue/4/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void pullOnFilledDatabase() {
        Optional<OutboundHistorySyncQueueItem> optionalItem = repository.pull();
        softly.assertThat(optionalItem)
            .as("Item must be present")
            .isPresent();

        optionalItem.ifPresent(item -> {
            softly.assertThat(item.getId())
                .as("Asserting item id")
                .isEqualTo(1L);

            softly.assertThat(item.getYandexId())
                .as("Asserting item yandexId")
                .isEqualTo("1");

            softly.assertThat(item.getCreated())
                .as("Asserting item created")
                .isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0));
        });
    }


    @Test
    @DatabaseSetup("classpath:repository/outbound_history_queue/5/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/outbound_history_queue/5/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void sequentialPullFromDifferentTransactions() {
        Set<String> yandexIds = new HashSet<>();

        withRequiresNewTransactionPropagation(() -> transactionTemplate.execute(t1 -> {
            Optional<OutboundHistorySyncQueueItem> firstItem = repository.pull();
            softly.assertThat(firstItem).isPresent();
            firstItem.ifPresent(i -> yandexIds.add(i.getYandexId()));

            transactionTemplate.execute(t2 -> {
                Optional<OutboundHistorySyncQueueItem> secondItem = repository.pull();
                softly.assertThat(secondItem).isPresent();
                secondItem.ifPresent(i2 -> yandexIds.add(i2.getYandexId()));

                return null;
            });

            return null;
        }));


        softly.assertThat(yandexIds).containsExactly("1", "2");
    }

    private void withRequiresNewTransactionPropagation(Runnable runnable) {
        int initialBehaviour = transactionTemplate.getPropagationBehavior();
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            runnable.run();
        } finally {
            transactionTemplate.setPropagationBehavior(initialBehaviour);
        }
    }
}
