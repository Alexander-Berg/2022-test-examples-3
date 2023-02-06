package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.OutboundInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteStockType;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class OutboundInfoRepositoryTest extends RepositoryTest {

    @Autowired
    private OutboundInfoRepository repository;

    @Test
    @DatabaseSetup(value = "classpath:repository/outbound_info_setup.xml")
    void findByYandexIdExisting() {
        Optional<OutboundInfo> found = repository.findByYandexId("OID1");

        softly.assertThat(found)
            .as("Asserting that found record is present")
            .isPresent();

        OutboundInfo outboundInfo = found.get();

        softly.assertThat(outboundInfo.getYandexId())
            .as("Asserting tha outbound yandex id")
            .isEqualTo("OID1");
        softly.assertThat(outboundInfo.getPartnerId())
            .as("Asserting tha outbound partner id")
            .isEqualTo("111");
        softly.assertThat(outboundInfo.getStock())
            .as("Asserting tha outbound stock")
            .isEqualTo(MarschrouteStockType.A);
        softly.assertThat(outboundInfo.getCreated())
            .as("Asserting tha outbound yandex id")
            .isEqualTo(LocalDateTime.parse("2018-04-05T18:55:56.430000"));
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/outbound_info_setup.xml")
    void findByYandexIdNonExisting() {
        Optional<OutboundInfo> found = repository.findByYandexId("OID4");

        softly.assertThat(found)
            .as("Asserting that found record is not present")
            .isEmpty();
    }

    /**
     * Проверяем, что при пустой БД в ответ будет возвращен пустой список.
     */
    @Test
    void findNonFitOutboundsToTrackOnEmptyDatabase() {
        Collection<OutboundInfo> result = repository.findNonFitOutboundsToTrack();

        softly.assertThat(result)
            .as("Asserting that result is empty")
            .isEmpty();
    }

    /**
     * Проверяем, что при наличии в БД изъятий с различными вариациями заполненности partner_id и stock'а
     * в ответ будут возвращены только те, у которых partner_id присутствует и stock != A
     * + только те, у которых либо нет статуса created, но в outbound_info created <= 90 дней от текущего
     * либо с момента записи статус прошло <= 90 дней.
     */
    @Test
    void findNonFitOutboundsToTrackOnOutboundsWithoutStatuses() {
        setupCreatedDateInDataSet("classpath:repository/outbound_info/outbounds_without_statuses.xml");

        Collection<OutboundInfo> result = repository.findNonFitOutboundsToTrack();

        softly.assertThat(result)
            .as("Asserting that result not empty")
            .hasSize(1);

        OutboundInfo outboundInfo = result.iterator().next();

        softly.assertThat(outboundInfo.getYandexId())
            .as("Asserting yandexId")
            .isEqualTo("506");

        softly.assertThat(outboundInfo.getPartnerId())
            .as("Asserting partnerId")
            .isEqualTo("partner-506");

        softly.assertThat(outboundInfo.getStock())
            .as("Asserting stock")
            .isEqualTo(MarschrouteStockType.Q);
    }

    /**
     * Проверяем, что при наличии в БД изъятий с различными вариациями заполненности partner_id и stock +
     * наличия/отсутствия связанных статусов будут возвращены только те,  у которых partner_id присутствует и stock != A
     * +
     */
    @Test
    @DatabaseSetup("classpath:repository/outbound_info/outbounds_with_statuses.xml")
    void findNonFitOutboundsToTrackOnOutboundsWithStatuses() {
        Collection<OutboundInfo> result = repository.findNonFitOutboundsToTrack(
            Timestamp.valueOf(LocalDateTime.of(2018, 1, 5, 0, 0))
        );

        softly.assertThat(result)
            .as("Asserting that result has two elements")
            .hasSize(2);

        List<String> yandexIds = result.stream().map(OutboundInfo::getYandexId)
            .collect(Collectors.toList());

        softly.assertThat(yandexIds)
            .as("Asserting yandexIds")
            .containsExactlyInAnyOrder("506", "507");
    }

    @Test
    @DatabaseSetup("classpath:repository/outbound_info/outbounds_with_outdated_statuses.xml")
    void findNonFitOutboundsToTrackOnOutboundsWithOutdatedStatuses() {
        Collection<OutboundInfo> result = repository.findNonFitOutboundsToTrack(
            Timestamp.valueOf(LocalDateTime.of(2018, 5, 31, 0, 0))
        );

        softly.assertThat(result)
            .as("Asserting result size")
            .hasSize(4);

        List<String> yandexIds = result.stream().map(OutboundInfo::getYandexId).collect(Collectors.toList());

        softly.assertThat(yandexIds)
            .as("Asserting yandex ids")
            .containsExactlyInAnyOrder("506", "507", "508", "509");
    }
}
