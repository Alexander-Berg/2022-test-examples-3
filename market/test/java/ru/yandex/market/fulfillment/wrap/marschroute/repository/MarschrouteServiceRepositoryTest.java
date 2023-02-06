package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.EntityType;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteService;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class MarschrouteServiceRepositoryTest extends RepositoryTest {

    @Autowired
    private MarschrouteServiceRepository repository;

    /**
     * Проверяет логику поиска услуг по идентификатору заказа.
     * <p>
     * Запрос должен успешно отфильтровать услуги с другим типом сущности (не заказ)
     * и для заказов, чье id отлично от запрашиваемого.
     */
    @Test
    @DatabaseSetup("classpath:repository/marschroute_service/find_by_order_id_setup.xml")
    void findByOrderId() {
        Collection<MarschrouteService> services = transactionTemplate.execute(
            t -> repository.findAllByEntityId("555", EntityType.ORDER)
        );

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(services)
                .as("Asserting services size")
                .hasSize(1);

            MarschrouteService service = services.iterator().next();

            assertions.assertThat(service.getId())
                .as("Asserting service id")
                .isEqualTo(100L);
        });
    }

    /**
     * Проверяет логику поиска по диапазону serviceDateTime [from;to)
     * <p>
     * Услуги, не вошедшие в этот диапазон, не должны попасть в итоговую выборку.
     */
    @Test
    @DatabaseSetup("classpath:repository/marschroute_service/find_by_service_date_time.xml")
    void findByServiceDateTime() {
        assertDateTimeSearch(
            Arrays.asList(1L, 2L, 3L, 4L),
            () -> repository.findAllByServiceDateTime(
                EntityType.ORDER,
                LocalDate.of(2018, 4, 5).atStartOfDay(),
                LocalDate.of(2018, 4, 6).atStartOfDay()
            )
        );
    }

    /**
     * Проверяет логику поиска по диапазону serviceDateTime [from;to) + isLatestVersion
     */
    @Test
    @DatabaseSetup("classpath:repository/marschroute_service/find_by_service_date_time.xml")
    void findActualVersionsByServiceDateTime() {
        assertDateTimeSearch(
            Arrays.asList(1L, 2L),
            () -> repository.findActualVersionsByServiceDateTime(
                EntityType.ORDER,
                LocalDate.of(2018, 4, 5).atStartOfDay(),
                LocalDate.of(2018, 4, 6).atStartOfDay()
            )
        );
    }

    /**
     * Проверяет логику поиска по диапазону discoveryDateTime [from;to)
     * <p>
     * Услуги, не вошедшие в этот диапазон, не должны попасть в итоговую выборку.
     */
    @Test
    @DatabaseSetup("classpath:repository/marschroute_service/find_by_discovery_date_time.xml")
    void findByDiscoveryDateTime() {
        assertDateTimeSearch(
            Arrays.asList(1L, 2L, 3L, 4L),
            () -> repository.findAllByDiscoveryDateTime(
                EntityType.ORDER,
                LocalDate.of(2018, 4, 5).atStartOfDay(),
                LocalDate.of(2018, 4, 6).atStartOfDay()
            )
        );
    }

    private void assertDateTimeSearch(List<Long> expectedIds,
                                      Supplier<Collection<MarschrouteService>> supplier) {
        SoftAssertions.assertSoftly(assertions -> {
            List<Long> ids = transactionTemplate.execute(t -> supplier.get())
                .stream()
                .map(MarschrouteService::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            assertions.assertThat(ids)
                .as("Assert that correct ids were pulled")
                .isEqualTo(expectedIds);
        });
    }
}
