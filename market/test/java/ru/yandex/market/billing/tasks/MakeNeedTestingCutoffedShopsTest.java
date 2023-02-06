package ru.yandex.market.billing.tasks;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.moderation.ModerationService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ProtocolOperation;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.request.trace.Module;

/**
 * @author m-bazhenov
 */
public class MakeNeedTestingCutoffedShopsTest extends FunctionalTest {

    @Autowired
    public JdbcTemplate jdbcTemplate;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired

    private Module sourceModule;

    @Mock
    private ProtocolService protocolService;

    @Mock
    private ModerationService moderationService;

    @Mock
    private CutoffService cutoffService;

    @Mock
    private JobExecutionContext context;

    private MakeNeedTestingCutoffedShops executor;


    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(invocation -> {
                    ProtocolOperation operation = invocation.getArgument(1);
                    operation.run(null, 1L);
                    return null;
                })
                .when(protocolService).operationInTransaction(Mockito.any(), Mockito.any(ProtocolOperation.class));

        executor = new MakeNeedTestingCutoffedShops(
                jdbcTemplate,
                protocolService,
                moderationService,
                cutoffService,
                sourceModule,
                environmentService
        );
    }

    @Test
    @DbUnitDataSet(
            type = DataSetType.SINGLE_CSV,
            before = "makeNeedTestingCpcCutoffedShops.csv"
    )
    void shouldOpenCpcCutoffsAndRequestModeration() {
        executor.doJob(context);

        ShopActionContext shopActionContext = new ShopActionContext(1L, 774L);
        CutoffType cutoffType = CutoffType.FORTESTING;
        Mockito.verify(cutoffService).openCutoff(Mockito.eq(shopActionContext), Mockito.eq(cutoffType));

        ShopProgram shopProgram = ShopProgram.CPC;
        TestingType testingType = TestingType.CPC_PREMODERATION;
        Mockito.verify(moderationService).setModerationType(Mockito.eq(shopActionContext),
                Mockito.eq(shopProgram), Mockito.eq(testingType));
        Mockito.verifyNoMoreInteractions(moderationService);
    }

    @Test
    @DbUnitDataSet(
            type = DataSetType.SINGLE_CSV,
            before = "makeNeedTestingCpcCutoffedShops.csv"
    )
    void shouldThrowExceptionInCaseOfTransactionFailure() {
        Assertions.assertThrows(
                RuntimeException.class,
                () -> {
                    Mockito.doThrow(RuntimeException.class)
                            .when(protocolService).operationInTransaction(Mockito.any(),
                                    Mockito.any(ProtocolOperation.class));
                    executor.doJob(context);
                }
        );
    }

}
