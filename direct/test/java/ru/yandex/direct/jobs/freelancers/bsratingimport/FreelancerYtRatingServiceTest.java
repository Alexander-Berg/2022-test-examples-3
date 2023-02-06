package ru.yandex.direct.jobs.freelancers.bsratingimport;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerBase;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FreelancerYtRatingServiceTest {

    private static final YtCluster CLUSTER = YtCluster.HAHN;
    private static final String YT_PATH = "//tmp/bs2direct";
    private static final long TABLE_ROWS = 10L;
    private static final List<FreelancerBase> RATINGS_FROM_YT = emptyList();

    private FreelancerYtRatingService testedService;
    private FreelancerBsRatingRowConsumer rowConsumerMock;
    private Cypress cypressMock;
    private YtOperator ytOperatorMock;

    @BeforeEach
    void setUp() {
        FreelancerBsRatingRowConsumerProvider rowConsumerProviderMock =
                mock(FreelancerBsRatingRowConsumerProvider.class);
        rowConsumerMock = mock(FreelancerBsRatingRowConsumer.class);
        when(rowConsumerProviderMock.getConsumer(anyInt()))
                .thenReturn(rowConsumerMock);

        YtProvider ytProviderMock = mock(YtProvider.class);
        cypressMock = mock(Cypress.class);
        when(cypressMock.list(any(YPath.class)))
                .thenReturn(Cf.arrayList());
        Yt ytMock = when(mock(Yt.class).cypress())
                .thenReturn(cypressMock).getMock();
        when(ytProviderMock.get(any(YtCluster.class)))
                .thenReturn(ytMock);

        ytOperatorMock = mock(YtOperator.class);
        when(ytProviderMock.getOperator(any(YtCluster.class)))
                .thenReturn(ytOperatorMock);

        testedService = new FreelancerYtRatingService(ytProviderMock, rowConsumerProviderMock);
    }

    @Test
    void getFreshestRatingTableName_success() {
        when(cypressMock.list(any(YPath.class)))
                .thenReturn(Cf.list(
                        new YTreeStringNodeImpl("archive", Cf.hashMap()),
                        new YTreeStringNodeImpl("2018-11-10", Cf.hashMap()),
                        new YTreeStringNodeImpl("2018-11-20", Cf.hashMap()),
                        new YTreeStringNodeImpl("2018-11-15", Cf.hashMap())
                ));

        String actual = testedService.getFreshestRatingTableName(CLUSTER, YT_PATH);
        assertThat(actual).isEqualTo("2018-11-20");
    }

    @Test
    void getFreshestRatingTableName_null_forEmptyDirectory() {
        String actual = testedService.getFreshestRatingTableName(CLUSTER, YT_PATH);
        assertThat(actual).isNull();
    }

    @Test
    void readRatingsFromYt() {
        when(ytOperatorMock.readTableNumericAttribute(any(YtTable.class), eq("row_count")))
                .thenReturn(TABLE_ROWS);
        when(rowConsumerMock.getDataAndCleanup())
                .thenReturn(RATINGS_FROM_YT);

        List<FreelancerBase> actual = testedService.readRatingsFromYt(CLUSTER, new YtTable(YT_PATH));

        assertThat(actual).isSameAs(RATINGS_FROM_YT);
    }
}
