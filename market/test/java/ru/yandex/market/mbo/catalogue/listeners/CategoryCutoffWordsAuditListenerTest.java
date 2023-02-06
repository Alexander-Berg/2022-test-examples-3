package ru.yandex.market.mbo.catalogue.listeners;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.catalogue.CategoryMatcherParamService;
import ru.yandex.market.mbo.catalogue.model.UpdateAttributesEventParams;
import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.core.kdepot.api.KnownEntityTypes;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.params.guru.GuruService;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.tt.events.AuditEventAction;

import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author galaev@yandex-team.ru
 * @since 28/11/2018.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategoryCutoffWordsAuditListenerTest {

    private CategoryCutoffWordsAuditListener listener;

    @Before
    public void setUp() throws Exception {
        GuruService guruService = Mockito.mock(GuruService.class);
        Mockito.when(guruService.getHid(anyLong())).thenReturn(1L);
        TovarTreeService tovarTreeService = Mockito.mock(TovarTreeService.class);
        Mockito.when(tovarTreeService.getCategoryByHid(anyLong())).thenReturn(new TovarCategory());
        AuditService auditService = new AuditService(null, null, "unit test");
        listener = new CategoryCutoffWordsAuditListener();
        listener.setGuruService(guruService);
        listener.setTovarTreeService(tovarTreeService);
        listener.setAuditService(auditService);
    }

    @Test
    public void testHandleAuditEvent() {
        Map<String, List<String>> before = Collections.singletonMap(CategoryMatcherParamService.CUT_OFF_WORD,
            ImmutableList.of("a"));
        Map<String, List<String>> update = Collections.singletonMap(CategoryMatcherParamService.CUT_OFF_WORD,
            ImmutableList.of("b", "c"));
        UpdateAttributesEventParams eventParams =
            new UpdateAttributesEventParams(1, KnownEntityTypes.MARKET_CATEGORY, 1, before, update);
        Collection<AuditEventAction> actions = listener.handleAuditEvent(eventParams);
        Assertions.assertThat(actions).hasSize(3);
        Assertions.assertThat(actions).allSatisfy(action ->
            Assertions.assertThat(action.getAuditAction().getBillingMode())
                .isEqualTo(AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM));
    }

    @Test
    public void testHandleAuditEventNoBilling() {
        Map<String, List<String>> before = Collections.singletonMap(CategoryMatcherParamService.CUT_OFF_WORD,
            ImmutableList.of("a"));
        Map<String, List<String>> update = Collections.singletonMap(CategoryMatcherParamService.CUT_OFF_WORD,
            ImmutableList.of("b", "c"));
        UpdateAttributesEventParams eventParams =
            new UpdateAttributesEventParams(1, KnownEntityTypes.MARKET_CATEGORY, 1, before, update);
        eventParams.setAuditOptions(new AuditAction.BillingOptions() {
            @Override
            public AuditAction.BillingMode getBillingMode() {
                return AuditAction.BillingMode.BILLING_MODE_FILL;
            }
            @Override
            public AuditAction.Source getSource() {
                return AuditAction.Source.YANG_TASK;
            }
            @Override
            public String getSourceId() {
                return "sourceId";
            }
        });
        Collection<AuditEventAction> actions = listener.handleAuditEvent(eventParams);
        Assertions.assertThat(actions).hasSize(3);
        Assertions.assertThat(actions).allSatisfy(action -> {
                Assertions.assertThat(action.getAuditAction().getBillingMode())
                    .isEqualTo(AuditAction.BillingMode.BILLING_MODE_FILL);
                Assertions.assertThat(action.getAuditAction().getSource())
                    .isEqualTo(AuditAction.Source.YANG_TASK);
                Assertions.assertThat(action.getAuditAction().getSourceId())
                    .isEqualTo("sourceId");
        });
    }
}
