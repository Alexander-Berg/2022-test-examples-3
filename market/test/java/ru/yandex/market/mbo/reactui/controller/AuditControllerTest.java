package ru.yandex.market.mbo.reactui.controller;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction.Source;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboAuditService;
import ru.yandex.market.mbo.reactui.dto.DataPage;
import ru.yandex.market.mbo.reactui.dto.NamedItem;
import ru.yandex.market.mbo.reactui.dto.OffsetLimit;
import ru.yandex.market.mbo.reactui.service.audit.AuditCsvService;

/**
 * @author yuramalinov
 * @created 15.10.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class AuditControllerTest {

    private AuditController auditController;
    private MboAuditService protoAuditService;

    @Before
    public void setupController() {
        protoAuditService = Mockito.mock(MboAuditService.class);
        AuditService auditService = new AuditService(
            protoAuditService, protoAuditService, "testing");
        auditController = new AuditController(auditService, Mockito.mock(AuditCsvService.class));

        Mockito.when(protoAuditService.countActions(Mockito.any()))
            .thenReturn(MboAudit.CountActionsResponse.newBuilder().addCount(42).build());
    }

    @Test
    public void testConfig() {
        AuditController.AuditConfig config = auditController.getAuditConfig();
        Assertions.assertThat(config.getSources())
            .extracting(NamedItem::getItem, NamedItem::getName)
            .containsExactlyInAnyOrder(
                Tuple.tuple(Source.CONTENT_LAB, Source.CONTENT_LAB.getRusName()),
                Tuple.tuple(Source.MBO, Source.MBO.getRusName()),
                Tuple.tuple(Source.YANG_TASK, Source.YANG_TASK.getRusName()),
                Tuple.tuple(Source.MDM, Source.MDM.getRusName()),
                Tuple.tuple(Source.AUTOGENERATION, Source.AUTOGENERATION.getRusName()),
                Tuple.tuple(Source.PARTNER_BETTER, Source.PARTNER_BETTER.getRusName()),
                Tuple.tuple(Source.PARTNER_GOOD, Source.PARTNER_GOOD.getRusName()),
                Tuple.tuple(Source.PARTNER_PSKU2, Source.PARTNER_PSKU2.getRusName()),
                Tuple.tuple(Source.VENDOR, Source.VENDOR.getRusName())
            );
    }

    @Test
    public void testRequest() {
        Mockito.when(protoAuditService.findActions(Mockito.any()))
            .thenReturn(MboAudit.FindActionsResponse.newBuilder()
                .addActions(MboAudit.MboAction.newBuilder().build())
                .setNextPageKey("test")
                .build());

        DataPage<AuditAction> result = auditController.getActions(new AuditFilter(), new OffsetLimit());
        Assertions.assertThat(result.getTotalCount()).isEqualTo(42);
        Assertions.assertThat(result.getItems()).hasSize(1);
    }

    @Test
    public void testPager() {
        Mockito.when(protoAuditService.findActions(Mockito.any()))
            .thenReturn(MboAudit.FindActionsResponse.newBuilder()
                .addActions(MboAudit.MboAction.newBuilder().build())
                .setNextPageKey("test")
                .build());

        auditController.getActions(
            new AuditFilter(),
            new OffsetLimit().setOffset(10).setLimit(100));

        ArgumentCaptor<MboAudit.FindActionsRequest> request =
            ArgumentCaptor.forClass(MboAudit.FindActionsRequest.class);

        Mockito.verify(protoAuditService).findActions(request.capture());
        Assertions.assertThat(request.getValue().getOffset()).isEqualTo(10);
        Assertions.assertThat(request.getValue().getLength()).isEqualTo(100);
    }

    @Test
    public void testKey() {
        Mockito.when(protoAuditService.findActions(Mockito.any()))
            .thenReturn(MboAudit.FindActionsResponse.newBuilder()
                .addActions(MboAudit.MboAction.newBuilder().build())
                .setNextPageKey("test")
                .build());

        auditController.getActions(
            new AuditFilter()
                .setRequestType(AuditFilter.RequestType.GROUP_BY_EVENT_ENTITY_USER)
                .setNextPageKey("some"),
            new OffsetLimit());

        ArgumentCaptor<MboAudit.FindActionsRequest> request =
            ArgumentCaptor.forClass(MboAudit.FindActionsRequest.class);

        Mockito.verify(protoAuditService).findActions(request.capture());
        Assertions.assertThat(request.getValue().getNextPageKey()).isEqualTo("some");
    }

}
