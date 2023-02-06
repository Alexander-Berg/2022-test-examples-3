package ru.yandex.market.delivery.transport_manager.controller.health;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TableMapper;
import ru.yandex.market.delivery.transport_manager.service.health.yt.YtReplicatorChecker;
import ru.yandex.market.delivery.transport_manager.service.health.yt.YtScheduleChecker;
import ru.yandex.market.delivery.transport_manager.service.yt.YtReader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YtHealthControllerTest extends AbstractContextualTest {

    @Autowired
    YtScheduleChecker ytScheduleChecker;

    @Autowired
    TableMapper tableMapper;

    YtHealthController ytHealthController;

    Yt ytMock;

    @BeforeEach
    void setClock() {
        mockYt();
        clock.setFixed(Instant.parse("2020-07-07T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup("/repository/health/yt/data_is_obsolete.xml")
    void checkNonUpdatable() {
        softly.assertThat(ytHealthController.nonUpdatingSync()).isEqualTo(
            "2;Suspicious: last YT timestamp is too old: 1593354806, more than 3600 seconds ago (800794 seconds)." +
                " Check YT data source");
    }

    @Test
    void checkEmpty() {
        softly.assertThat(ytHealthController.emptySync()).isEqualTo("2;Error: no yt metadata present, check YT folder");
    }

    @DatabaseSetup("/repository/health/yt/sync_ok.xml")
    @Test
    void checkOK() {
        clock.setFixed(Instant.parse("2020-07-07T14:00:00.00Z"), ZoneOffset.UTC);
        softly.assertThat(ytHealthController.nonUpdatingSync()).isEqualTo("0;OK");
        softly.assertThat(ytHealthController.failedSync()).isEqualTo("0;OK");
        softly.assertThat(ytHealthController.emptySync()).isEqualTo("0;OK");
    }

    @DatabaseSetup("/repository/health/yt/replicator.xml")
    @Test
    void replicatorOK() {
        mockYt();
        copyAllSchemaToIgnoredExcept("null");
        softly.assertThat(ytHealthController.replicator()).isEqualTo("0;OK");
    }

    @DatabaseSetup("/repository/health/yt/replicator_not_ok.xml")
    @Test
    void replicatorNotOK() {
        mockYt();
        copyAllSchemaToIgnoredExcept("transportation");
        softly.assertThat(ytHealthController.replicator()).isEqualTo("1;Following tables should exist in YT.\n" +
            " Master: [transportation]\n" +
            " Backup: [transportation]");
    }

    private void mockYt() {
        var fakeMap = new HashMap<String, YTreeNode>();
        fakeMap.put("transportation", null);
        YTreeNode fakeNode = new YTreeMapNodeImpl(fakeMap);
        var cypressMock = mock(Cypress.class);
        ytMock = mock(Yt.class);
        when(cypressMock.get(YPath.simple("//home/market/production/mstat/dwh/raw/market_transport_manager")))
            .thenReturn(fakeNode);
        when(ytMock.cypress()).thenReturn(cypressMock);
        ytHealthController = new YtHealthController(
            ytScheduleChecker,
            new YtReplicatorChecker(new YtReader(ytMock, ytMock), tableMapper)
        );
    }

    private void copyAllSchemaToIgnoredExcept(String transportation) {
        jdbcTemplate.execute(String.format(
            "INSERT INTO yt_replicator_ignore_list (table_name, reason) SELECT table_name::text as table_name, " +
                "'copy' as reason " +
                "FROM information_schema.tables WHERE table_name::text != '%s';",
            transportation
        ));
    }
}
