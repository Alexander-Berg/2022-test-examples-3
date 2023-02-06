package ru.yandex.market.core.feature.precondition;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.feature.precondition.model.PreconditionResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class FeaturePreconditionsTest {
    public static final long SHOP_ID = 0L;
    FeaturePrecondition ok = FeaturePreconditions.ok();
    FeaturePrecondition failedA = makeFailed("A");
    FeaturePrecondition failedB = makeFailed("B");
    FeaturePrecondition failedC = makeFailed("C");

    @Test
    void orOkSingle() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.or(ok);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result);
    }

    @Test
    void orOkSomewhere() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.or(failedA, failedB, ok, failedC);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result);
    }

    @Test
    void orFailedSingle() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.or(failedA);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result, "A");
    }

    @Test
    void orFailedAll() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.or(failedA, failedB, failedC);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result, "A", "B", "C");
    }

    @Test
    void andOkSingle() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.and(ok);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result);
    }

    @Test
    void andOkBeginning() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.and(ok, failedA, failedB, failedC);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result, "A", "B", "C");
    }

    @Test
    void andOkMiddle() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.and(failedA, failedB, ok, failedC);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result, "A", "B", "C");
    }

    @Test
    void andOkTail() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.and(failedA, failedB, failedC, ok);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result, "A", "B", "C");
    }

    @Test
    void andFailedSingle() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.and(failedA);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result, "A");
    }

    @Test
    void andFailedAll() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.and(failedA, failedB, failedC);

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result, "A", "B", "C");
    }

    @Test
    void notOk() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.not(ok, "OK");

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertThat(result.requiresModeration(), is(false));
        assertThat(result.restrictsIndexation(), is(true));
        assertResultContainsFailures(result, "!OK");
    }

    @Test
    void notFailed() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.not(failedA, "A");

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result);
    }

    @Test
    void mixedAndOr() {
        // given
        FeaturePrecondition precondition = FeaturePreconditions.or(
                failedA,
                FeaturePreconditions.and(
                        failedB,
                        failedC,
                        FeaturePreconditions.not(ok, "OK")
                )
        );

        // when
        PreconditionResult result = precondition.evaluate(SHOP_ID);

        // then
        assertResultContainsFailures(result, "A", "B", "C", "!OK");
    }

    private static void assertResultContainsFailures(PreconditionResult result, String... failure) {
        boolean ok = failure.length == 0;
        assertThat(result.canEnable(), equalTo(ok));
        if (ok) {
            assertThat(result.getFailedPreconditions(), is(empty()));
        } else {
            assertThat(
                    "результат должен содержать все потенциально упавшие проверки для ускорения отладки",
                    result.getFailedPreconditions(),
                    contains(failure)
            );
        }
    }

    private static FeaturePrecondition makeFailed(String name) {
        return new SimplePrecondition(name) {
            @Override
            public PreconditionResult evaluate(long shopId) {
                return makeFailedResultRequiresModeration();
            }
        };
    }
}
