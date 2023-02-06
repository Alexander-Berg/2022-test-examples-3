package ru.yandex.direct.ess.router.rules.bsexport.feeds;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.FeedsUpdateStatus;
import ru.yandex.direct.ess.logicobjects.bsexport.feeds.BsExportFeedsObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.FeedsChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.FEEDS;
import static ru.yandex.direct.dbschema.ppc.enums.FeedsUpdateStatus.Done;
import static ru.yandex.direct.dbschema.ppc.enums.FeedsUpdateStatus.New;
import static ru.yandex.direct.ess.router.testutils.FeedsChange.createFeedsEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BsExportFeedsRuleTest {
    private static final String METHOD = "method";
    private static final String SERVICE = "service";
    private static final Long REQID = 1342352352532L;
    @Autowired
    private BsExportFeedsRule rule;

    @Test
    void insertDoneFeedTest() {
        BigInteger feedId = BigInteger.valueOf(10L);
        FeedsChange feedsChange = new FeedsChange().withFeedId(feedId);
        feedsChange.addInsertedColumn(FEEDS.UPDATE_STATUS, Done.getLiteral());
        var binlogEvent = createFeedsEvent(List.of(feedsChange), Operation.INSERT);
        addSystemFieldsToEvent(binlogEvent);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportFeedsObject(feedId.longValue());
        addSystemFieldsToObject(expectedObject);
        assertThat(objects).hasSize(1);
        assertThat(objects.get(0)).isEqualToComparingFieldByField(expectedObject);
    }

    @Test
    void insertNotDoneFeedTest() {
        BigInteger feedId = BigInteger.valueOf(10L);
        var notTriggeredChanges = Arrays.stream(FeedsUpdateStatus.values())
                .filter(status -> !Done.equals(status))
                .map(FeedsUpdateStatus::getLiteral)
                .map(status -> {
                    var change = new FeedsChange().withFeedId(feedId);
                    change.addInsertedColumn(FEEDS.UPDATE_STATUS, status);
                    return change;
                })
                .collect(Collectors.toList());


        var binlogEvent = createFeedsEvent(notTriggeredChanges, Operation.INSERT);
        addSystemFieldsToEvent(binlogEvent);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void updateStatusToDoneFeedTest() {
        BigInteger feedId = BigInteger.valueOf(10L);
        FeedsChange feedsChange = new FeedsChange().withFeedId(feedId);
        feedsChange.addChangedColumn(FEEDS.UPDATE_STATUS, New.getLiteral(), Done.getLiteral());
        var binlogEvent = createFeedsEvent(List.of(feedsChange), Operation.UPDATE);
        addSystemFieldsToEvent(binlogEvent);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportFeedsObject(feedId.longValue());
        addSystemFieldsToObject(expectedObject);
        assertThat(objects).hasSize(1);
        assertThat(objects.get(0)).isEqualToComparingFieldByField(expectedObject);
    }

    @Test
    void updateNotToDoneFeedTest() {
        BigInteger feedId = BigInteger.valueOf(10L);
        var notTriggeredChanges = Arrays.stream(FeedsUpdateStatus.values())
                .filter(status -> !Done.equals(status))
                .map(FeedsUpdateStatus::getLiteral)
                .map(status -> {
                    var change = new FeedsChange().withFeedId(feedId);
                    change.addChangedColumn(FEEDS.UPDATE_STATUS, Done.getLiteral(), status);
                    return change;
                })
                .collect(Collectors.toList());


        var binlogEvent = createFeedsEvent(notTriggeredChanges, Operation.UPDATE);
        addSystemFieldsToEvent(binlogEvent);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void deleteFeedTest() {
        BigInteger feedId = BigInteger.valueOf(10L);
        FeedsChange feedsChange = new FeedsChange().withFeedId(feedId);
        var binlogEvent = createFeedsEvent(List.of(feedsChange), Operation.DELETE);
        addSystemFieldsToEvent(binlogEvent);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportFeedsObject(feedId.longValue(), true);
        addSystemFieldsToObject(expectedObject);
        assertThat(objects).hasSize(1);
        assertThat(objects.get(0)).isEqualToComparingFieldByField(expectedObject);
    }

    private void addSystemFieldsToEvent(BinlogEvent binlogEvent) {
        binlogEvent.setTraceInfoMethod(METHOD);
        binlogEvent.setTraceInfoService(SERVICE);
        binlogEvent.setTraceInfoReqId(REQID);
    }

    private void addSystemFieldsToObject(BsExportFeedsObject object) {
        object.setMethod(METHOD);
        object.setService(SERVICE);
        object.setReqid(REQID);
    }
}
