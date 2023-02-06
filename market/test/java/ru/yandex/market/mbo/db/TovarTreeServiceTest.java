package ru.yandex.market.mbo.db;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.db.params.guru.GuruService;
import ru.yandex.market.mbo.db.recommendations.RecommendationValidationService;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 18.07.2017
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class TovarTreeServiceTest {
    private static final long UID = -1L;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private AuditService auditService;

    @Mock
    private TovarTreeDao tovarTreeDao;

    @Mock
    private RecommendationValidationService recommendationValidationService;

    @Mock
    private GuruService guruService;

    private TovarTreeService tovarTreeService;

    @Mock
    private TovarTreeTagService tovarTreeTagService;

    @Mock
    private CachedTreeService cachedTreeService;

    @Before
    public void setUp() throws Exception {
        tovarTreeService = new TovarTreeService(
            Collections.emptyList(),
            auditService, tovarTreeDao, null, transactionTemplate, recommendationValidationService,
            null, null, null, null, guruService, tovarTreeTagService, null, cachedTreeService, null);

        when(transactionTemplate.execute(any())).then(args -> {
            TransactionCallback<?> action = args.getArgument(0);
            action.doInTransaction(new SimpleTransactionStatus());
            return null;
        });
    }

    @Test(expected = OperationException.class)
    public void testDisableSku() {
        TovarCategory before = new TovarCategory();
        TovarCategory after = new TovarCategory();
        before.setSkuEnabled(true);
        after.setSkuEnabled(false);
        tovarTreeService.updateCategory(before, after, UID);
    }

    @Test
    public void testEnableSku() {
        TovarCategory before = new TovarCategory();
        TovarCategory after = new TovarCategory();
        before.setSkuEnabled(false);
        after.setSkuEnabled(true);
        tovarTreeService.updateCategory(before, after, UID);
    }
}
