package ru.yandex.chemodan.app.queller.test;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.queller.celery.control.CeleryControl;
import ru.yandex.chemodan.queller.celery.control.callback.CeleryInspectActiveCallback;
import ru.yandex.chemodan.queller.celery.control.callback.CeleryInspectActiveQueuesCallback;
import ru.yandex.chemodan.queller.celery.control.callback.CeleryInspectStatsCallback;
import ru.yandex.chemodan.queller.celery.control.callback.CeleryReplyInfo;
import ru.yandex.chemodan.queller.celery.control.callback.replies.CeleryInspectActiveQueuesReply;
import ru.yandex.chemodan.queller.celery.control.callback.replies.CeleryInspectActiveReply;
import ru.yandex.chemodan.queller.celery.control.callback.replies.CeleryInspectStatsReply;
import ru.yandex.chemodan.queller.celery.worker.WorkerId;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;

/**
 * @author yashunsky
 */
public class CeleryApiTest extends QuellerTestSupport {

    @Autowired
    private CeleryControl celery;

    private static final Logger logger = LoggerFactory.getLogger(CeleryApiTest.class);

    @Ignore
    @Test
    public void sendCeleryCommand() throws InterruptedException {

        CeleryInspectStatsCallback statsCallback = new CeleryInspectStatsCallback() {
            public void onMessageGet(CeleryReplyInfo info, CeleryInspectStatsReply reply) {
                logger.info("Celery stats answer from worker \"{}\": {}", reply);
            }
        };

        CeleryInspectActiveCallback activeCallback = new CeleryInspectActiveCallback() {
            public void onMessageGet(CeleryReplyInfo info, ListF<CeleryInspectActiveReply> replies) {
                logger.info("Celery active answer from worker \"{}\": {}", replies);
            }
        };

        CeleryInspectActiveQueuesCallback aqCallback = new CeleryInspectActiveQueuesCallback() {
            public void onMessageGet(CeleryReplyInfo info, ListF<CeleryInspectActiveQueuesReply> replies) {
                logger.info("Celery active queues answer from worker \"{}\": {}", info.workerId, replies);
            }
        };

        celery.registerCallback(statsCallback);
        celery.registerCallback(activeCallback);
        celery.registerCallback(aqCallback);

        celery.inspectStats(Option.empty());
        celery.inspectActive(Option.empty());
        celery.inspectActiveQueues(Option.empty());

        ListF<WorkerId> destinations = Cf.list(WorkerId.parse("worker1"));

        celery.controlRateLimit(Option.empty(), "tasks.work_with_file", "5/s");

        celery.controlTimeLimit(Option.of(destinations), "tasks.work_with_file", 0, 0);

    }
}
