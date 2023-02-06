package ru.yandex.market.core.cutoff;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.TestingService;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.testing.TestingType;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "CutoffShouldNotAffectSandboxStateFunctionalTest.csv")
class CutoffShouldNotAffectSandboxStateFunctionalTest extends FunctionalTest {
    public static final long ACTION_ID = 456L;
    private static final long SHOP_ID = 123L;
    @Autowired
    CutoffService cutoffService;
    @Autowired
    TestingService testingService;
    @Autowired
    TransactionTemplate transactionTemplate;

    static Stream<Arguments> testArgs() {
        List<TestingState> parameters = new ArrayList<>();

        // Different combinations of TestingStatus and TestingType
        for (TestingType testingType : new TestingType[]{
                TestingType.GENERAL_LITE_CHECK,
                TestingType.CPA_PREMODERATION,
                TestingType.CPA_CHECK,
                TestingType.CPC_LITE_CHECK,
                TestingType.CPC_PREMODERATION
        }) {
            for (TestingStatus status : TestingStatus.values()) {
                TestingState state = createDefaultTestingState();
                state.setTestingType(testingType);
                state.setStatus(status);
                parameters.add(state);
            }
        }
        // Different combinations of isReady, isInProgress and isCanceled
        for (boolean isReady : new boolean[]{true, false}) {
            for (boolean isInProgress : new boolean[]{true, false}) {
                for (boolean isCanceled : new boolean[]{true, false}) {
                    TestingState state = createDefaultTestingState();
                    state.setReady(isReady);
                    state.setInProgress(isInProgress);
                    state.setCancelled(isCanceled);
                    parameters.add(state);
                }
            }
        }
        TestingState state = createDefaultTestingState();
        state.setFatalCancelled(true);
        parameters.add(state);

        for (Date startDate : new Date[]{new Date(), null}) {
            state = createDefaultTestingState();
            state.setStartDate(startDate);
            parameters.add(state);
        }
        return parameters.stream().map(Arguments::of);
    }

    private static TestingState createDefaultTestingState() {
        TestingState state = new TestingState();
        state.setDatasourceId(SHOP_ID);
        state.setTestingType(TestingType.CPC_PREMODERATION);
        state.setStatus(TestingStatus.READY_FOR_CHECK);
        state.setReady(false);
        state.setInProgress(true);
        state.setCancelled(false);
        state.setFatalCancelled(false);
        state.setStartDate(new Date());
        state.setAttemptNum(0);
        state.setPushReadyButtonCount(0);
        return state;
    }

    private static void assertSameTestingState(TestingState expected, TestingState actual) {
        assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
        assertThat(actual.getTestingType()).isEqualTo(expected.getTestingType());
        assertThat(actual.isReady()).isEqualTo(expected.isReady());
        assertThat(actual.isInProgress()).isEqualTo(expected.isInProgress());
        assertThat(actual.isCancelled()).isEqualTo(expected.isCancelled());
        assertThat(actual.isFatalCancelled()).isEqualTo(expected.isFatalCancelled());
        assertThat(actual.getStartDate() != null).isEqualTo(expected.getStartDate() != null);
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("testArgs")
    void testOpeningTECHNICAL_YMLShouldNotAffectSandboxState(TestingState testingState) {
        testCutoffTypeShouldNotAffectSandboxStateOnOpen(CutoffType.TECHNICAL_YML, testingState);
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("testArgs")
    void testClosingTECHNICAL_YMLShouldNotAffectSandboxState(TestingState testingState) {
        testCutoffTypeShouldNotAffectSandboxStateOnClose(CutoffType.TECHNICAL_YML, testingState);
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("testArgs")
    void testOpeningYAMANAGERShouldNotAffectSandboxState(TestingState testingState) {
        testCutoffTypeShouldNotAffectSandboxStateOnOpen(CutoffType.YAMANAGER, testingState);
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("testArgs")
    void testClosingYAMANAGERShouldNotAffectSandboxState(TestingState testingState) {
        testCutoffTypeShouldNotAffectSandboxStateOnClose(CutoffType.YAMANAGER, testingState);
    }

    private void testCutoffTypeShouldNotAffectSandboxStateOnOpen(CutoffType cutoffType, TestingState testingState) {
        ShopActionContext shopActionContext = new ShopActionContext(ACTION_ID, SHOP_ID);
        shouldNotAffectSandboxState(shopActionContext, () -> cutoffService.openCutoff(shopActionContext, cutoffType), testingState);
    }

    private void testCutoffTypeShouldNotAffectSandboxStateOnClose(CutoffType cutoffType, TestingState testingState) {
        ShopActionContext shopActionContext = new ShopActionContext(ACTION_ID, SHOP_ID);
        shouldNotAffectSandboxState(shopActionContext, () -> cutoffService.closeCutoff(shopActionContext, cutoffType), testingState);
    }

    private void shouldNotAffectSandboxState(
            ShopActionContext shopActionContext,
            Runnable runnable,
            TestingState testingState
    ) {
        transactionTemplate.execute(status -> {
            status.setRollbackOnly(); // не комитим чтобы не транкейтить после каждого теста
            testingService.insertState(shopActionContext, new TestingState(testingState));

            runnable.run();

            assertSameTestingState(testingState, testingService.getTestingStatus(
                    shopActionContext.getShopId(),
                    testingState.getTestingType().getShopProgram()
            ));
            return null;
        });
    }
}
