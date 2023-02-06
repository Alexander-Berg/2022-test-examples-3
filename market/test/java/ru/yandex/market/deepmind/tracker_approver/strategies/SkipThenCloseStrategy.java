package ru.yandex.market.deepmind.tracker_approver.strategies;

import ru.yandex.market.deepmind.tracker_approver.pojo.BaseStatusMeta;
import ru.yandex.market.deepmind.tracker_approver.pojo.CloseRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.CloseResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.EnrichRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.EnrichResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyMeta;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.ReopenRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ReopenResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartResponse;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverStrategy;

/**
 * Стратегия, которая всегда при первом прогоне переоткрывает тикет, а при последующем закрывает.
 */
public class SkipThenCloseStrategy implements TrackerApproverStrategy<MyKey, MyMeta, MyMeta> {
    public static final String TYPE = "skip_than_close";
    private boolean skipped = false;

    @Override
    public StartResponse<MyKey, MyMeta, MyMeta> start(StartRequest<MyKey, MyMeta, MyMeta> request) {
        return StartResponse.of("TICKET-1");
    }

    @Override
    public EnrichResponse<MyKey, MyMeta, MyMeta> enrich(EnrichRequest<MyKey, MyMeta, MyMeta> request) {
        return EnrichResponse.of();
    }

    @Override
    public ProcessResponse<MyKey, MyMeta, MyMeta> process(ProcessRequest<MyKey, MyMeta, MyMeta> request) {
        var status = skipped ? BaseStatusMeta.Status.OK : BaseStatusMeta.Status.NOT_READY;
        skipped = true;
        return ProcessResponse.of(status);
    }

    @Override
    public ReopenResponse<MyKey, MyMeta, MyMeta> reopen(ReopenRequest<MyKey, MyMeta, MyMeta> request) {
        return ReopenResponse.of();
    }

    @Override
    public CloseResponse<MyKey, MyMeta, MyMeta> close(CloseRequest<MyKey, MyMeta, MyMeta> request) {
        return CloseResponse.of();
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
