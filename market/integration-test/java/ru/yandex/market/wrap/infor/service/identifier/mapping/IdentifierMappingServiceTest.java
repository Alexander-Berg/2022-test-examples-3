package ru.yandex.market.wrap.infor.service.identifier.mapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wrap.infor.configuration.AbstractContextualTest;
import ru.yandex.market.wrap.infor.entity.InforUnitId;

import static ru.yandex.market.wrap.infor.configuration.IdentifierMappingConfiguration.DEFAULT_MAPPING_SERVICE;

class IdentifierMappingServiceTest extends AbstractContextualTest {

    private static final UnitId UNIT_ID_11 = new UnitId("SKU1", 1L, "SKU1");
    private static final UnitId UNIT_ID_12 = new UnitId("SKU1", 2L, "SKU1");
    private static final UnitId UNIT_ID_21 = new UnitId("SKU2", 1L, "SKU2");
    private static final UnitId UNIT_ID_22 = new UnitId("SKU2", 2L, "SKU2");

    private static final ImmutableList<UnitId> UNIT_IDS = ImmutableList.of(
        UNIT_ID_11,
        UNIT_ID_12,
        UNIT_ID_21,
        UNIT_ID_22
    );

    private static final InforUnitId INFOR_UNIT_ID_1 = InforUnitId.of("TST0000000000000000001", 1L);
    private static final InforUnitId INFOR_UNIT_ID_2 = InforUnitId.of("TST0000000000000000002", 2L);
    private static final InforUnitId INFOR_UNIT_ID_3 = InforUnitId.of("TST0000000000000000003", 1L);
    private static final InforUnitId INFOR_UNIT_ID_4 = InforUnitId.of("TST0000000000000000004", 2L);

    private static final ImmutableList<InforUnitId> INFOR_UNIT_IDS = ImmutableList.of(
        INFOR_UNIT_ID_1,
        INFOR_UNIT_ID_2,
        INFOR_UNIT_ID_3,
        INFOR_UNIT_ID_4
    );

    @Autowired
    @Qualifier(DEFAULT_MAPPING_SERVICE)
    private IdentifierMappingService identifierMappingService;

    /**
     * Сценарий #1:
     * <p>
     * Пытаемся сделать обратный маппинг для группы форматированных id'шников.
     * <p>
     * В БД отсутствуют значения для всех из них.
     * В ответ должна вернуться пустая Map.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/4/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/4/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void reverseMapOnEmptyDatabase() {
        Map<InforUnitId, UnitId> result = identifierMappingService.unmap(INFOR_UNIT_IDS);

        softly.assertThat(result)
            .doesNotContainKeys(INFOR_UNIT_ID_1, INFOR_UNIT_ID_2, INFOR_UNIT_ID_3, INFOR_UNIT_ID_4);
    }

    /**
     * Сценарий #2:
     * <p>
     * Пытаемся сделать обратный маппинг для группы форматированных id'шников.
     * <p>
     * В БД отсутствуют значения для части из них.
     * В ответ должны вернуться только те пары Key/Value, у которых присутствовал маппинг в БД.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/5/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/5/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void reverseMapOnPartiallyFilledDatabase() {
        Map<InforUnitId, UnitId> result = identifierMappingService.unmap(INFOR_UNIT_IDS);

        softly.assertThat(result).containsEntry(INFOR_UNIT_ID_1, UNIT_ID_11);
        softly.assertThat(result).containsEntry(INFOR_UNIT_ID_2, UNIT_ID_12);

        softly.assertThat(result).doesNotContainKeys(INFOR_UNIT_ID_3, INFOR_UNIT_ID_4);
    }

    /**
     * Сценарий #3:
     * <p>
     * Пытаемся сделать обратный маппинг для группы форматированных id'шников.
     * <p>
     * В БД присутствуют значения для всех из них.
     * <p>
     * В ответ должны вернуться маппинга для всех из них.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/6/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/6/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void reverseMapOnFullyFilledDatabase() {
        Map<InforUnitId, UnitId> result = identifierMappingService.unmap(INFOR_UNIT_IDS);

        softly.assertThat(result).containsEntry(INFOR_UNIT_ID_1, UNIT_ID_11);
        softly.assertThat(result).containsEntry(INFOR_UNIT_ID_2, UNIT_ID_12);
        softly.assertThat(result).containsEntry(INFOR_UNIT_ID_3, UNIT_ID_21);
        softly.assertThat(result).containsEntry(INFOR_UNIT_ID_4, UNIT_ID_22);
    }

    /**
     * Сценарий #4:
     * <p>
     * Пытаемся сделать обратной маппинг для пустой коллекции форматированных id'шников.
     * <p>
     * В БД существует маппинг для определенного набора форматированных id'шников.
     * В результате в ответ должна вернуться пустая Map'а, а состояние БД должно остаться неизменным.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/8/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/8/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void reverseMapOnEmptyCollection() {
        Map<InforUnitId, UnitId> result = identifierMappingService.unmap(Collections.emptyList());

        softly.assertThat(result).isEmpty();
    }

    /**
     * Сценарий #5:
     * <p>
     * Пытаемся сделать прямой маппинг для группы id'шников.
     * <p>
     * В БД отсутствуют значения для всех из них.
     * В ответ должна вернуться пустая Map.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/4/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/4/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void forwardMapOnEmptyDatabase() {
        Map<UnitId, InforUnitId> result = identifierMappingService.map(UNIT_IDS);

        softly.assertThat(result).isEmpty();
    }


    /**
     * Сценарий #6:
     * <p>
     * Пытаемся сделать прямой маппинг для группы id'шников.
     * <p>
     * В БД отсутствуют значения для части из них.
     * В ответ должны вернуться только те пары Key/Value, у которых присутствовал маппинг в БД.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/5/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/5/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void forwardMapOnPartiallyFilledDatabase() {
        Map<UnitId, InforUnitId> result = identifierMappingService.map(UNIT_IDS);

        softly.assertThat(result).containsEntry(UNIT_ID_11, INFOR_UNIT_ID_1);
        softly.assertThat(result).containsEntry(UNIT_ID_12, INFOR_UNIT_ID_2);

        softly.assertThat(result).doesNotContainKeys(UNIT_ID_21, UNIT_ID_22);
    }


    /**
     * Сценарий #7:
     * <p>
     * Пытаемся сделать прямой маппинг для группы id'шников.
     * <p>
     * В БД присутствуют значения для всех из них.
     * <p>
     * В ответ должны вернуться маппинга для всех из них.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/6/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/6/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void forwardMapOnFullyFilledDatabase() {
        Map<UnitId, InforUnitId> result = identifierMappingService.map(UNIT_IDS);

        softly.assertThat(result).containsEntry(UNIT_ID_11, INFOR_UNIT_ID_1);
        softly.assertThat(result).containsEntry(UNIT_ID_12, INFOR_UNIT_ID_2);
        softly.assertThat(result).containsEntry(UNIT_ID_21, INFOR_UNIT_ID_3);
        softly.assertThat(result).containsEntry(UNIT_ID_22, INFOR_UNIT_ID_4);
    }

    /**
     * Сценарий #8:
     * <p>
     * Пытаемся сделать прямой маппинг для пустой коллекции id'шников.
     * <p>
     * В БД существует маппинг для определенного набора форматированных id'шников.
     * В результате в ответ должна вернуться пустая Map'а, а состояние БД должно остаться неизменным.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/8/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/8/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void forwardMapOnEmptyCollection() {
        Map<UnitId, InforUnitId> result = identifierMappingService.map(Collections.emptyList());

        softly.assertThat(result).isEmpty();
    }


    /**
     * Сценарий #9:
     * <p>
     * Пытаемся сделать маппинг для всех UnitId.
     * <p>
     * В БД маппинг для этих записей отсутствует.
     * В результате в БД должны появиться записи для каждой из SKU с индексами 1,2,3,4.
     */
    @Test
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/1/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mapOnEmptyDatabase() {
        Map<UnitId, InforUnitId> result = identifierMappingService.createMapping(UNIT_IDS);

        softly
            .assertThat(result.values())
            .containsExactlyInAnyOrder(INFOR_UNIT_ID_1, INFOR_UNIT_ID_2, INFOR_UNIT_ID_3, INFOR_UNIT_ID_4);
    }

    /**
     * Сценарий #10:
     * <p>
     * Пытаемся сделать маппинг для всех UnitId.
     * <p>
     * В БД маппинг для всех этих комбинаций уже существует.
     * В результате должна вернуться соответствующая информация из БД без изменений.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/2/setup.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/2/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mapOnFullDatabase() {
        Map<UnitId, InforUnitId> result = identifierMappingService.createMapping(UNIT_IDS);

        softly.assertThat(result.get(UNIT_ID_11)).isEqualTo(INFOR_UNIT_ID_1);
        softly.assertThat(result.get(UNIT_ID_12)).isEqualTo(INFOR_UNIT_ID_2);
        softly.assertThat(result.get(UNIT_ID_21)).isEqualTo(INFOR_UNIT_ID_3);
        softly.assertThat(result.get(UNIT_ID_22)).isEqualTo(INFOR_UNIT_ID_4);
    }


    /**
     * Сценарий #11:
     * <p>
     * Пытаемся сделать маппинг для всех UnitId.
     * <p>
     * В БД маппинг существует для части из этих записей.
     * В результате в БД должны появиться записи для недостающей части маппингов.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/3/setup.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/3/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mapOnPartiallyFilledDatabase() {
        Map<UnitId, InforUnitId> result = identifierMappingService.createMapping(UNIT_IDS);

        softly.assertThat(result.size()).isEqualTo(UNIT_IDS.size());
        softly.assertThat(result.remove(UNIT_ID_11)).isEqualTo(InforUnitId.of("TST0000000000000000100", 1L));
        softly.assertThat(result.remove(UNIT_ID_12)).isEqualTo(InforUnitId.of("TST0000000000000000200", 2L));

        List<String> newFormattedIds = result.values().stream()
            .map(InforUnitId::getFormattedId)
            .collect(Collectors.toList());

        softly.assertThat(newFormattedIds)
            .containsExactlyInAnyOrder(INFOR_UNIT_ID_1.getFormattedId(), INFOR_UNIT_ID_2.getFormattedId());
    }


    /**
     * Сценарий #12:
     * <p>
     * Пытаемся сделать маппинг для пустой коллекции UnitId.
     * <p>
     * В БД существует маппинг для определенного набора UnitId.
     * В результате в ответ должна вернуться пустая Map'а, а состояние БД должно остаться неизменным.
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/7/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/integration/id_mapping/7/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mapEmptyCollection() {
        Map<UnitId, InforUnitId> result = identifierMappingService.createMapping(Collections.emptyList());

        softly.assertThat(result).isEmpty();
    }
}
