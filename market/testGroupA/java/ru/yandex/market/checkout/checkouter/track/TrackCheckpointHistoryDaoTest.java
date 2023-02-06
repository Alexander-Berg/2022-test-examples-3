package ru.yandex.market.checkout.checkouter.track;

import java.util.Collection;
import java.util.List;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.event.HistoryPoint;
import ru.yandex.market.checkout.checkouter.storage.track.checkpoint.history.TrackCheckpointHistoryDao;

public class TrackCheckpointHistoryDaoTest extends AbstractWebTestBase {

    @Autowired
    private TrackCheckpointHistoryDao trackCheckpointHistoryDao;

    @Test
    public void checkGetHistoryForMissingCollection() {
        HistoryPoint<Collection<TrackCheckpoint>> historyPoint =
                trackCheckpointHistoryDao.getHistoryForCollection(List.of(DSL.trueCondition()), -1);

        Assertions.assertTrue(historyPoint.getBefore().isEmpty());
        Assertions.assertTrue(historyPoint.getAfter().isEmpty());
    }
}
