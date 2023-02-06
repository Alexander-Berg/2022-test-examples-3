package ru.yandex.market.fulfillment.stockstorage;

import java.util.ArrayList;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.FulfillmentFeedId;
import ru.yandex.market.fulfillment.stockstorage.repository.JdbcFeedIdRepository;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class JdbcFeedIdRepositoryTest extends AbstractContextualTest {

    @SpyBean
    private JdbcFeedIdRepository repository;

    /**
     * Кейс, когда feed_Id уже вставлены, не должно произойти перевставки данных
     */
    @Test
    @DatabaseSetup("classpath:database/states/feed_id_repository/feed_id.xml")
    @ExpectedDatabase(value = "classpath:database/expected/feed_id_repository/inserted_feed_id.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void checkSavedWithCheck() {
        ArrayList<FulfillmentFeedId> fulfillmentFeedIds = new ArrayList<>();
        fulfillmentFeedIds.add(new FulfillmentFeedId(1, 1));
        fulfillmentFeedIds.add(new FulfillmentFeedId(2, 2));
        fulfillmentFeedIds.add(new FulfillmentFeedId(3, 3));
        fulfillmentFeedIds.add(new FulfillmentFeedId(4, 4));
        repository.insertAllWithExistenceCheck(fulfillmentFeedIds);
    }

    /**
     * Сценарий #1:
     * <p>
     * В БД нет ску.
     * Запрос на получение доступных складов должен вернуть пустой set.
     */
    @Test
    @DatabaseSetup("classpath:database/states/warehouse/empty.xml")
    @ExpectedDatabase(value = "classpath:database/states/warehouse/empty.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void noWarehousesIdsStored() {
        Set<Integer> storedWarehousesIds = repository.getStoredWarehousesIds();
        softly.assertThat(storedWarehousesIds).isEmpty();
    }

    /**
     * Сценарий #2:
     * <p>
     * В БД ровно 1 ску, чей warehouse_id =1 .
     * Запрос на получение доступных складов должен вернуть Set(1).
     */
    @Test
    @DatabaseSetup("classpath:database/states/warehouse/single.xml")
    @ExpectedDatabase(value = "classpath:database/states/warehouse/single.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void singleWarehouseIdStored() {
        Set<Integer> storedWarehouses = repository.getStoredWarehousesIds();
        softly.assertThat(storedWarehouses).containsExactly(1);
    }

    /**
     * Сценарий #3:
     * <p>
     * В БД ровно 2 ску, со значениями wh_id = 1 и wh_id = 2.
     * Запрос на получение доступных складов должен вернуть Set(1,2).
     */
    @Test
    @DatabaseSetup("classpath:database/states/warehouse/multiple.xml")
    @ExpectedDatabase(value = "classpath:database/states/warehouse/multiple.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void multipleWarehousesIdsStored() {
        Set<Integer> storedWarehouses = repository.getStoredWarehousesIds();
        softly.assertThat(storedWarehouses).containsExactly(1, 2);
    }
}
