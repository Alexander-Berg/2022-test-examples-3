package ru.yandex.direct.core.entity.statistics.repository;


import org.jooq.Select;
import org.junit.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.statistics.model.YtHashBorders;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static org.mockito.Mockito.mock;
import static ru.yandex.direct.test.utils.QueryUtils.compareQueries;

public class OrderStatRepositoryTest {

    private static final String SIMPLE_QUERY_PATH_FILE =
            "classpath:///ru/yandex/direct/core/entity/statistics/order-stat.query";

    @Test
    public void getChangedCampaignQueryTest() {
        var orderStatRepository = new OrderStatRepository(mock(YtProvider.class), mock(PpcPropertiesSupport.class),
                mock(DirectYtDynamicConfig.class));
        var expectedQuery = LiveResourceFactory.get(SIMPLE_QUERY_PATH_FILE).getContent();
        Select updateCampaignsQuery = orderStatRepository.getChangedCampaignQuery(new YtHashBorders(0, 7));
        var gotQuery = updateCampaignsQuery.toString();
        compareQueries(expectedQuery, gotQuery);
    }
}
