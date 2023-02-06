package ru.yandex.direct.bsexport;

import java.io.IOException;
import java.util.UUID;

import javax.xml.soap.SOAPException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import ru.yandex.direct.bsexport.configuration.BsExportConfiguration;
import ru.yandex.direct.bsexport.iteration.BsExportIterationContext;
import ru.yandex.direct.bsexport.iteration.BsExportIterationFactory;
import ru.yandex.direct.bsexport.iteration.container.ExportCandidatesSelectionCriteria;
import ru.yandex.direct.bsexport.messaging.SoapSerializer;
import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.model.UpdateData2Request;
import ru.yandex.direct.bsexport.model.UpdateData2RequestSoapMessage;
import ru.yandex.direct.bsexport.query.order.OrderDataFactory;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshot;
import ru.yandex.direct.bsexport.snapshot.SnapshotDataFactory;
import ru.yandex.direct.bsexport.util.QueryComposer;
import ru.yandex.direct.core.entity.bs.export.model.WorkerSpec;
import ru.yandex.direct.logging.LoggingInitializer;
import ru.yandex.direct.tracing.TraceGuard;
import ru.yandex.direct.tracing.TraceHelper;

/**
 * Простой класс для разработческой отладки кода экспорта.
 */
@Component
public class Debug {
    private static final Logger logger = LoggingInitializer.getLogger(Debug.class);

    @Autowired
    private BsExportIterationFactory iterationFactory;

    @Autowired
    private SnapshotDataFactory snapshotDataFetcher;


    public void debug() {
        ExportCandidatesSelectionCriteria selectionCriteria = ExportCandidatesSelectionCriteria.builder()
                .setWorkerSpec(WorkerSpec.STD_1)
                .setLimit(30)
                .setLockNewCampaigns(true)
                .setShard(21)
                .build();

        try (BsExportIterationContext context = iterationFactory.lockCampaignsInQueue(selectionCriteria)) {
            iteration(context);
        }

        logger.debug("test");
    }

    void iteration(BsExportIterationContext iterationContext) {
        BsExportSnapshot snapshot = snapshotDataFetcher.getSnapshot(iterationContext);
        logger.debug("snapshot fetched");

        UpdateData2Request.Builder builder = UpdateData2Request.newBuilder()
                .setEngineID(7)
                .setRequestUUID(UUID.randomUUID().toString());
        OrderDataFactory orderDataFactory = new OrderDataFactory(snapshot);
        for (Long campaignId : iterationContext.getDataCampaignIds()) {
            Order.Builder order = orderDataFactory.getOrder(campaignId);
            QueryComposer.putOrder(builder, order.build());
        }
        UpdateData2RequestSoapMessage message = UpdateData2RequestSoapMessage.newBuilder()
                .setRequest(builder)
                .setWorkerID(777)
                .build();

        try {
            String result = new SoapSerializer().serializeRoot(message);
            logger.info("serialized soap");
        } catch (SOAPException | IOException e) {
            throw new RuntimeException(e);
        }
        logger.debug("test");
    }

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(BsExportConfiguration.class)) {
            LoggingInitializer.initializeFromClasspath();
            Debug bean = ctx.getBean(Debug.class);
            try (TraceGuard ignored = ctx.getBean(TraceHelper.class).guard("debug")) {
                bean.debug();
            }
        }
    }
}
