package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.OutboundStatusInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class OutboundStatusInfoRepositoryTest extends RepositoryTest {

    @Autowired
    private OutboundStatusInfoRepository outboundStatusInfoRepository;

    @Test
    void findAllByYandexIdOnEmptyDatabase() {
        Collection<OutboundStatusInfo> allByYandexId = outboundStatusInfoRepository.findAllByYandexId("1");

        softly.assertThat(allByYandexId)
            .as("Asserting that outbound statuses are empty")
            .isEmpty();
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/outbound_status_info/1/setup.xml")
    void findAllByYandexIdOnDatabaseWithSingleValue() {
        Collection<OutboundStatusInfo> allByYandexId = outboundStatusInfoRepository.findAllByYandexId("503");

        softly.assertThat(allByYandexId)
            .as("Asserting returned result list size")
            .hasSize(1);

        OutboundStatusInfo info = allByYandexId.iterator().next();

        softly.assertThat(info.getId())
            .as("Asserting id value")
            .isEqualTo(1L);

        softly.assertThat(info.getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("503");

        softly.assertThat(info.getStatusCode())
            .as("Asserting status code")
            .isEqualTo(21);

        softly.assertThat(info.getCreated())
            .as("Asserting created value")
            .isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0));

        softly.assertThat(info.getStatusDateTime())
            .as("Asserting status date time")
            .isEqualTo(LocalDateTime.of(1970, 1, 2, 0, 0));
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/outbound_status_info/2/setup.xml")
    void findAllByYandexIdOnDatabaseWithMultipleValues() {
        Collection<OutboundStatusInfo> allByYandexId = outboundStatusInfoRepository.findAllByYandexId("503");

        softly.assertThat(allByYandexId)
            .as("Asserting returned result list size")
            .hasSize(2);

        List<Long> statusIds = allByYandexId
            .stream()
            .map(OutboundStatusInfo::getId)
            .collect(Collectors.toList());

        softly.assertThat(statusIds)
            .as("Asserting returned ids values")
            .containsExactlyInAnyOrder(1L, 2L);
    }


    @Test
    @DatabaseSetup("classpath:repository/outbound_status_info/3/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/outbound_status_info/3/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insertOrDoNothingOnEmptyDatabase() {
        outboundStatusInfoRepository.insertOrDoNothing(new OutboundStatusInfo()
            .setYandexId("500")
            .setStatusCode(21)
            .setStatusDateTime(LocalDateTime.of(1970, 1, 1, 0, 0))
            .setCreated(LocalDateTime.of(1970, 1, 2, 0, 0))
        );
    }

    @Test
    @DatabaseSetup("classpath:repository/outbound_status_info/4/state.xml")
    @ExpectedDatabase(value = "classpath:repository/outbound_status_info/4/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insertOrDoNothingOnFilledDatabase() {
        outboundStatusInfoRepository.insertOrDoNothing(new OutboundStatusInfo()
            .setYandexId("500")
            .setStatusCode(21)
            .setStatusDateTime(LocalDateTime.of(1970, 1, 1, 0, 0))
            .setCreated(LocalDateTime.of(2000, 1, 2, 0, 0))
        );
    }
}
