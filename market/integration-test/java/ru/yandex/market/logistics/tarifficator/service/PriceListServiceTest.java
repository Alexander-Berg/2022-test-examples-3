package ru.yandex.market.logistics.tarifficator.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableSortedMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.jobs.model.QueueType;
import ru.yandex.market.logistics.tarifficator.model.entity.PriceList;
import ru.yandex.market.logistics.tarifficator.service.pricelist.PriceListService;
import ru.yandex.market.logistics.tarifficator.util.PayloadFactory;
import ru.yandex.market.logistics.tarifficator.util.QueueTasksTestUtil;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Интеграционный тест сервиса PriceListService")
class PriceListServiceTest extends AbstractContextualTest {

    private static final Instant TIME_11_AM = Instant.parse("2019-08-12T11:00:00.00Z");

    @Autowired
    private PriceListService priceListService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        clock.setFixed(TIME_11_AM, ZoneOffset.UTC);
    }

    @AfterEach
    void after() {
        clock.clearFixed();
    }

    @Test
    @DisplayName("Активировать прайс-листы тарифа")
    @DatabaseSetup("/service/price-list/db/before/processed-minimal-price-list.xml")
    @ExpectedDatabase(
        value = "/service/price-list/db/after/processed-minimal-two-price-lists-activated.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/multiple-price-lists-activation-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activate() {
        priceListService.activate(List.of(1L, 2L));
        assertGenerationScheduledForPriceLists(Set.of(1L, 2L));
    }

    @Test
    @DisplayName("Активировать прайс-листы тарифа и деактивировать существующие")
    @DatabaseSetup("/service/price-list/db/before/already-activated-price-lists.xml")
    @ExpectedDatabase(
        value = "/history-events/activate-and-archive-price-list-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activateNewAndArchiveOld() {
        priceListService.activate(List.of(3L, 4L));
        assertGenerationScheduledForPriceLists(Set.of(3L, 4L));
    }

    @Test
    @DisplayName("Активировать прайс-листы тарифа и деактивировать существующие — тариф выключен")
    @DatabaseSetup("/service/price-list/db/before/already-activated-price-lists.xml")
    @DatabaseSetup(
        value = "/service/price-list/db/before/tariff_is_not_enabled.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/history-events/activate-and-archive-price-list-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activateNewAndArchiveOldForNotEnabledTariff() {
        priceListService.activate(List.of(3L, 4L));
        assertGenerationScheduledForPriceLists(Set.of(3L, 4L));
    }

    @Test
    @DisplayName("Активировать прайс-листы тарифа своей доставки")
    @DatabaseSetup("/service/price-list/db/before/processed-minimal-price-list-own-delivery.xml")
    @ExpectedDatabase(
        value = "/service/price-list/db/after/processed-minimal-one-price-list-activated.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/single-price-list-activation-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activateOwnDelivery() {
        priceListService.activate(List.of(1L));
        assertGenerationScheduledForPriceLists(Set.of(1L));
    }

    @Test
    @DisplayName("Активировать прайс-лист общего тарифа с равными себестоимостью и стоимостью")
    @DatabaseSetup("/service/price-list/db/before/processed-minimal-price-list-general-one-price.xml")
    @ExpectedDatabase(
        value = "/service/price-list/db/after/processed-minimal-two-price-lists-activated.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/multiple-price-lists-activation-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activateGeneralOnePrice() {
        priceListService.activate(List.of(1L, 2L));
        assertGenerationScheduledForPriceLists(Set.of(1L, 2L));
    }

    @Test
    @DisplayName("Попытка активировать уже активированный прайс-лист")
    @DatabaseSetup("/service/price-list/db/before/activated-price-list.xml")
    @ExpectedDatabase(
        value = "/service/price-list/db/before/activated-price-list.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activateAlreadyActivatedError() {
        softly.assertThatThrownBy(() -> priceListService.activate(List.of(1L, 2L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Price-lists [1, 2] must not be activated or ended");

        assertNoGenerationScheduled();
    }

    @Test
    @DisplayName("Попытка активировать прайс-лист отправленный в архив")
    @DatabaseSetup("/service/price-list/db/before/archived-price-list.xml")
    @ExpectedDatabase(
        value = "/service/price-list/db/before/archived-price-list.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activateAlreadyArchivedError() {
        softly.assertThatThrownBy(() -> priceListService.activate(List.of(1L, 2L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Price-lists [1, 2] must not be activated or ended");

        assertNoGenerationScheduled();
    }

    @Test
    @DisplayName("Попытка активировать прайс-листы из разных тарифов")
    @DatabaseSetup("/service/price-list/db/before/price-list-from-different-tariffs.xml")
    @ExpectedDatabase(
        value = "/service/price-list/db/before/price-list-from-different-tariffs.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activateFromDifferentTariffs() {
        softly.assertThatThrownBy(() -> priceListService.activate(List.of(1L, 3L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("All price-lists must belong to the same tariff");

        assertNoGenerationScheduled();
    }

    @Test
    @DisplayName(
        "Попытка активировать публичный прайс-лист без внутреннего для GENERAL типа " +
        "без флага равенства себестоимости и стоимости"
    )
    @DatabaseSetup("/service/price-list/db/before/price-list-from-different-tariffs.xml")
    @ExpectedDatabase(
        value = "/service/price-list/db/before/price-list-from-different-tariffs.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activatePublicOnlyForGeneral() {
        softly.assertThatThrownBy(() -> priceListService.activate(List.of(1L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Two price-lists are required for activating 'GENERAL' tariff type");

        assertNoGenerationScheduled();
    }

    @Test
    @DisplayName("Попытка активировать несколько прайс-листов для OWN_DELIVERY типа")
    @DatabaseSetup("/service/price-list/db/before/many-price-lists-own-delivery.xml")
    @ExpectedDatabase(
        value = "/service/price-list/db/before/many-price-lists-own-delivery.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activatePublicAndNonpublicForOwnDelivery() {
        softly.assertThatThrownBy(() -> priceListService.activate(List.of(1L, 2L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("One price-list is required for activating 'OWN_DELIVERY' tariff type");

        assertNoGenerationScheduled();
    }

    @Test
    @DisplayName("Получение готовых для генерации прайс-листов для beru")
    @DatabaseSetup({
        "/tags/tags.xml",
        "/service/price-list/db/before/beru-price-lists.xml"
    })
    void findReadyForDatasetGenerationForBeru() {
        softly.assertThat(priceListService.findReadyForDatasetGeneration())
            .extracting(PriceList::getId)
            .containsOnly(1L, 2L, 3L, 4L);
    }

    private void assertGenerationScheduledForPriceLists(Set<Long> changedPriceListIds) {
        QueueTasksTestUtil.assertQueueTasks(
            softly,
            objectMapper,
            jdbcTemplate,
            ImmutableSortedMap.of(
                QueueType.GENERATE_REVISION,
                PayloadFactory.createGenerateRevisionPayload(Set.of(), changedPriceListIds, 1)
            )
        );
    }

    private void assertNoGenerationScheduled() {
        QueueTasksTestUtil.assertQueueTasks(softly, objectMapper, jdbcTemplate, ImmutableSortedMap.of());
    }
}
