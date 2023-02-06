package ru.yandex.market.psku.postprocessor.service.markup;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.ir.http.MarkupService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PairState;
import ru.yandex.market.psku.postprocessor.config.TrackerTestConfig;
import ru.yandex.market.psku.postprocessor.service.exception.MarkupServiceException;
import ru.yandex.market.psku.postprocessor.service.tracker.PskuTrackerService;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.CategoryTrackerInfoProducerMock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mboc.http.SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.ACCEPTED;
import static ru.yandex.market.mboc.http.SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.NEED_INFO;
import static ru.yandex.market.mboc.http.SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.REJECTED;
import static ru.yandex.market.mboc.http.SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.UNDEFINED;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
@ContextConfiguration(classes = {
    TrackerTestConfig.class
})
public class MarkupServiceTest extends BaseDBTest {
    private static int i = 0;

    @Mock
    MarkupService markupServiceClient;

    @Mock
    PskuTrackerService pskuTrackerService;
    @Autowired
    CategoryTrackerInfoProducerMock categoryTrackerInfoProducerMock;
    @Autowired
    PskuKnowledgeDao pskuKnowledgeDao;

    private PskuMappingValidationTask pskuMappingValidationTask;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        pskuMappingValidationTask = new PskuMappingValidationTask(markupServiceClient, pskuTrackerService,
            categoryTrackerInfoProducerMock, pskuKnowledgeDao);
    }

    @Test
    public void whenReceiveAcceptedShouldSetValidTrue() {
        mockMarkupWithResult(ACCEPTED);
        List<PairValidationResult> result = pskuMappingValidationTask.getResult(1);
        assertStateAndValid(result, PairState.RECEIVED, true);
    }

    @Test
    public void whenReceiveRejectedShouldSetValidFalse() {
        mockMarkupWithResult(REJECTED);
        List<PairValidationResult> result = pskuMappingValidationTask.getResult(1);
        assertStateAndValid(result, PairState.RECEIVED, false);
    }

    @Test
    public void whenReceiveNeedInfoShouldSetValidFalse() {
        mockMarkupWithResult(NEED_INFO);
        List<PairValidationResult> result = pskuMappingValidationTask.getResult(1);
        assertStateAndValid(result, PairState.NEED_INFO, false);
    }

    @Test
    public void whenReceiveUndefinedAndNotDeletedShouldError() {
        mockMarkupWithResult(UNDEFINED);
        assertThatExceptionOfType(MarkupServiceException.class)
                .isThrownBy(() -> pskuMappingValidationTask.getResult(1));
    }

    @Test
    public void whenReceiveUndefinedAndDeleted() {
        mockMarkupWithResultDeleted();
        List<PairValidationResult> result = pskuMappingValidationTask.getResult(1);
        assertStateAndValid(result, PairState.ALREADY_DELETED, false);
    }

    private void assertStateAndValid(List<PairValidationResult> result, PairState state, boolean isValid) {
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsValid()).isEqualTo(isValid);
        assertThat(result.get(0).getState()).isEqualTo(state);
    }

    private void mockMarkupWithResult(
            SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult moderationResult
    ) {
        when(markupServiceClient.getPartnerMappingValidationTaskResult(any(Markup.GetByConfigRequest.class)))
                .thenReturn(Markup.PartnerMappingValidationTaskResponse.newBuilder()
                        .addTaskResult(createTaskResult(nextInt(), nextInt(), false, moderationResult))
                        .build());
    }

    private void mockMarkupWithResultDeleted() {
        when(markupServiceClient.getPartnerMappingValidationTaskResult(any(Markup.GetByConfigRequest.class)))
                .thenReturn(Markup.PartnerMappingValidationTaskResponse.newBuilder()
                        .addTaskResult(createTaskResult(nextInt(), nextInt(), true, UNDEFINED))
                        .build());
    }

    private Markup.PartnerMappingValidationTaskResult createTaskResult(
            long partnerSkuId, long mskuId, boolean deleted,
            SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult moderationResult
    ) {
        Markup.MappingValidationPair.Builder mappingValidation = Markup.MappingValidationPair.newBuilder()
                .setPartnerSku(
                        Markup.PartnerValidationSku.newBuilder().setSkuId(partnerSkuId).build());
        if (!deleted) {
            mappingValidation.setMarketSku(
                    Markup.PartnerValidationSku.newBuilder().setSkuId(mskuId).build());
        }
        return Markup.PartnerMappingValidationTaskResult.newBuilder()
                .setModerationResult(moderationResult)
                .setMappingValidationPair(mappingValidation)
                .setDeleted(deleted)
                .build();
    }

    private static int nextInt() {
        return ++i;
    }
}
