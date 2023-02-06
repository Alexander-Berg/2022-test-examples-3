package ru.yandex.market.wrap.infor.service.identifier.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.immutable.SingletonMap;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.wrap.infor.configuration.AbstractContextualTest;
import ru.yandex.market.wrap.infor.entity.InforUnitId;

class MonitoredMappingServiceTest extends AbstractContextualTest {


    private static final UnitId UNIT_ID_11 = new UnitId("SKU1", 1L, "SKU1");
    private static final UnitId UNIT_ID_12 = new UnitId("SKU1", 2L, "SKU1");
    private static final UnitId UNIT_ID_21 = new UnitId("SKU2", 1L, "SKU2");
    private static final UnitId UNIT_ID_22 = new UnitId("SKU2", 2L, "SKU2");

    private static final InforUnitId INFOR_UNIT_ID_1 = InforUnitId.of("TST0000000000000000001", 1L);
    private static final InforUnitId INFOR_UNIT_ID_2 = InforUnitId.of("TST0000000000000000002", 2L);
    private static final InforUnitId INFOR_UNIT_ID_3 = InforUnitId.of("TST0000000000000000003", 1L);
    private static final InforUnitId INFOR_UNIT_ID_4 = InforUnitId.of("TST0000000000000000004", 2L);
    private static final InforUnitId INFOR_UNIT_ID_5 = InforUnitId.of("TST0000000000000000005", 99L);

    @BeforeEach
    void setUp() {
        RequestContextHolder.setContext(new RequestContext("123"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clearContext();
    }

    @Autowired
    private IdentifierMappingService identifierMappingService;

    /**
     * Сценарий #1:
     * <p>
     * Пытаемся сделать обратной маппинг для пустой коллекции форматированных id'шников.
     * <p>
     * В БД существует маппинг для определенного набора форматированных id'шников.
     * В результате в ответ должна вернуться пустая Map'а, а состояние БД должно остаться неизменным.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/all_mapping.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/all_mapping.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unmapOnEmptyCollection() {
        Map<InforUnitId, UnitId> result = identifierMappingService.unmap(Collections.emptyList());

        softly.assertThat(result).isEmpty();
    }

    /**
     * Сценарий #2:
     * <p>
     * Пытаемся сделать обратной маппинг для коллекции,
     * которая содержит один неизвестный и один известный SKU.
     * <p>
     * В БД существует маппинг для определенного набора форматированных id'шников.
     * БД неизвестных SKU - пуста.
     * <p>
     * В результате в ответ должна вернуться Map'а из одного элемента,
     * в таблице неизвестных SKU добавится одна запись.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/partially_mapping.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/partially_mapping.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/2/unknown_skus.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unmapOnCollectionWhichContainsSingleUnknownSku() {
        Map<InforUnitId, UnitId> result = identifierMappingService.unmap(ImmutableList.of(INFOR_UNIT_ID_1, INFOR_UNIT_ID_2));

        softly.assertThat(result).isEqualTo(SingletonMap.of(INFOR_UNIT_ID_1, UNIT_ID_11));
    }

    /**
     * Сценарий #3:
     * <p>
     * Пытаемся сделать обратной маппинг для коллекции,
     * которая содержит только неизвестные SKU.
     * <p>
     * В БД существует маппинг для определенного набора форматированных id'шников.
     * БД неизвестных SKU - пуста.
     * <p>
     * В результате в ответ должна вернуться пустая Map'а,
     * в таблице неизвестных SKU добавятся 2 записи.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/partially_mapping.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/partially_mapping.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/3/unknown_skus.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unmapOnUnknownSkus() {
        Map<InforUnitId, UnitId> result = identifierMappingService.unmap(ImmutableList.of(INFOR_UNIT_ID_2, INFOR_UNIT_ID_3));

        softly.assertThat(result).isEmpty();
    }

    /**
     * Сценарий #4:
     * <p>
     * Пытаемся сделать обратной маппинг для коллекции,
     * которая содержит только неизвестные SKU в случае,
     * когда в таблице неизвестных SKU уже есть такие записи.
     * <p>
     * В БД существует маппинг для определенного набора форматированных id'шников.
     * В таблице неизвестных SKU содержатся: {@link #INFOR_UNIT_ID_2} с CRIT,
     * {@link #INFOR_UNIT_ID_3} с WARN, {@link #INFOR_UNIT_ID_5} с OK.
     * <p>
     * В результате в ответ должна вернуться пустая Map'а,
     * в таблице неизвестных SKU добавится 1 запись с уровнем CRIT для {@link #INFOR_UNIT_ID_5}.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/partially_mapping.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/4/unknown_skus_state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/partially_mapping.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/4/unknown_skus_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unmapUnknownSkusOnNonEmptyDatabase() {
        Map<InforUnitId, UnitId> result = identifierMappingService.unmap(
            ImmutableList.of(INFOR_UNIT_ID_2, INFOR_UNIT_ID_3, INFOR_UNIT_ID_5));

        softly.assertThat(result).isEmpty();
    }


    /**
     * Сценарий #5:
     * <p>
     * Пытаемся сделать обратной маппинг только для известных SKU.
     * <p>
     * В БД существует маппинг для определенного набора форматированных id'шников.
     * В результате в ответ должна вернуться пустая Map'а, содержащая все 4 SKU.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/all_mapping.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/5/unknown_skus.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/all_mapping.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/monitored_id_mapping/5/unknown_skus.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unmapOnlyKnownSkus() {
        Collection<InforUnitId> formattedSkus = ImmutableList.of(INFOR_UNIT_ID_1, INFOR_UNIT_ID_2, INFOR_UNIT_ID_3, INFOR_UNIT_ID_4);
        Map<InforUnitId, UnitId> result = identifierMappingService.unmap(formattedSkus);

        Map<InforUnitId, UnitId> expected = ImmutableMap.of(
            INFOR_UNIT_ID_1, UNIT_ID_11,
            INFOR_UNIT_ID_2, UNIT_ID_12,
            INFOR_UNIT_ID_3, UNIT_ID_21,
            INFOR_UNIT_ID_4, UNIT_ID_22
        );

        softly.assertThat(result).isEqualTo(expected);
    }
}
