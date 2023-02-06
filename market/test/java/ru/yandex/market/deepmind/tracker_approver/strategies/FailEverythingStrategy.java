package ru.yandex.market.deepmind.tracker_approver.strategies;

import ru.yandex.market.deepmind.tracker_approver.pojo.CloseRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.CloseResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.EnrichRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.EnrichResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyMeta;
import ru.yandex.market.deepmind.tracker_approver.pojo.PostProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.PostProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.PreprocessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.PreprocessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.ReopenRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ReopenResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartResponse;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverStrategy;

public class FailEverythingStrategy implements TrackerApproverStrategy<MyKey, MyMeta, MyMeta> {
    public static final String TYPE = "fail_everithing";

    @Override
    public StartResponse<MyKey, MyMeta, MyMeta> start(StartRequest<MyKey, MyMeta, MyMeta> request) {
        return StartResponse.of("TEST-1");
    }

    @Override
    public EnrichResponse<MyKey, MyMeta, MyMeta> enrich(EnrichRequest<MyKey, MyMeta, MyMeta> request) {
        throw new RuntimeException("enrich is failed");
    }

    @Override
    public PreprocessResponse<MyKey, MyMeta, MyMeta> preprocess(PreprocessRequest<MyKey, MyMeta, MyMeta> request) {
        throw new RuntimeException("preprocess is failed");
    }

    @Override
    public ProcessResponse<MyKey, MyMeta, MyMeta> process(ProcessRequest<MyKey, MyMeta, MyMeta> request) {
        throw new RuntimeException("process is failed");
    }

    @Override
    public PostProcessResponse<MyKey, MyMeta, MyMeta> postprocess(PostProcessRequest<MyKey, MyMeta, MyMeta> request) {
        throw new RuntimeException("postprocess is failed");
    }

    @Override
    public ReopenResponse<MyKey, MyMeta, MyMeta> reopen(ReopenRequest<MyKey, MyMeta, MyMeta> request) {
        throw new RuntimeException("reopen is failed");
    }

    @Override
    public CloseResponse<MyKey, MyMeta, MyMeta> close(CloseRequest<MyKey, MyMeta, MyMeta> request) {
        throw new RuntimeException("close is failed");
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
