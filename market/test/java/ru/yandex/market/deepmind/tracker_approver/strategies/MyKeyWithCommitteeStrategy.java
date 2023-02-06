package ru.yandex.market.deepmind.tracker_approver.strategies;

import ru.yandex.market.deepmind.tracker_approver.pojo.BaseStatusMeta;
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
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueCreate;

public class MyKeyWithCommitteeStrategy implements TrackerApproverStrategy<MyKey, MyMeta, MyMeta> {
    public static final String TYPE = "committee";

    private final Session session;

    public MyKeyWithCommitteeStrategy(Session session) {
        this.session = session;
    }

    @Override
    public StartResponse<MyKey, MyMeta, MyMeta> start(StartRequest<MyKey, MyMeta, MyMeta> request) {
        var issue = session.issues().create(IssueCreate.builder()
            .summary("Тикет на согласование с комитетом")
            .description("Согласуйте данные!")
            .type("task")
            .build()
        );
        return StartResponse.of(issue.getKey(), request.getMeta(), request.getKeyMetaMap());
    }

    @Override
    public EnrichResponse<MyKey, MyMeta, MyMeta> enrich(EnrichRequest<MyKey, MyMeta, MyMeta> request) {
        return EnrichResponse.of();
    }

    @Override
    public PreprocessResponse<MyKey, MyMeta, MyMeta> preprocess(PreprocessRequest<MyKey, MyMeta, MyMeta> request) {
        return PreprocessResponse.of(BaseStatusMeta.Status.NOT_READY);
    }

    @Override
    public ProcessResponse<MyKey, MyMeta, MyMeta> process(ProcessRequest<MyKey, MyMeta, MyMeta> request) {
        return ProcessResponse.of(BaseStatusMeta.Status.NOT_READY);
    }

    @Override
    public PostProcessResponse<MyKey, MyMeta, MyMeta> postprocess(PostProcessRequest<MyKey, MyMeta, MyMeta> request) {
        return PostProcessResponse.of(BaseStatusMeta.Status.NOT_READY);
    }

    @Override
    public CloseResponse<MyKey, MyMeta, MyMeta> close(CloseRequest<MyKey, MyMeta, MyMeta> request) {
        return CloseResponse.of();
    }

    @Override
    public ReopenResponse<MyKey, MyMeta, MyMeta> reopen(ReopenRequest<MyKey, MyMeta, MyMeta> request) {
        return ReopenResponse.of();
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
