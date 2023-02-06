package ru.yandex.market.deepmind.tracker_approver.strategies;

import java.util.Map;

import ru.yandex.market.deepmind.tracker_approver.pojo.BaseStatusMeta;
import ru.yandex.market.deepmind.tracker_approver.pojo.CloseRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.CloseResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.EnrichRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.EnrichResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyMeta;
import ru.yandex.market.deepmind.tracker_approver.pojo.PreprocessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.PreprocessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.ReopenRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ReopenResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartResponse;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverStrategy;

/**
 * Стратегия, которая будем менять meta, но не будет продолжать сохранение.
 */
public class ChangeKeyMetaAndNotReadyStrategy implements TrackerApproverStrategy<MyKey, MyMeta, MyMeta> {
    public static final String TYPE = "change_key_and_not_ready";

    @Override
    public StartResponse<MyKey, MyMeta, MyMeta> start(StartRequest<MyKey, MyMeta, MyMeta> request) {
        return StartResponse.of("TICKET-1", request.getMeta());
    }

    @Override
    public EnrichResponse<MyKey, MyMeta, MyMeta> enrich(EnrichRequest<MyKey, MyMeta, MyMeta> request) {
        return EnrichResponse.of(request.getMeta(), Map.of(request.getKeys().get(0), new MyMeta("enrich")));
    }

    @Override
    public PreprocessResponse<MyKey, MyMeta, MyMeta> preprocess(PreprocessRequest<MyKey, MyMeta, MyMeta> request) {
        return PreprocessResponse.of(BaseStatusMeta.Status.OK, request.getMeta(),
            Map.of(request.getKeys().get(0), new MyMeta("check")));
    }

    @Override
    public ProcessResponse<MyKey, MyMeta, MyMeta> process(ProcessRequest<MyKey, MyMeta, MyMeta> request) {
        return ProcessResponse.of(BaseStatusMeta.Status.NOT_READY,
            Map.of(request.getKeys().get(0), new MyMeta("process")));
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
