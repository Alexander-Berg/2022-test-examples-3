package ru.yandex.market.api.util.parser2.resolver;

import org.junit.Test;

import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.util.parser2.resolver.errors.ResolverError;
import ru.yandex.market.api.util.parser2.resolver.errors.TestUnknownResolverError;

public class CompositeResolverTest {
    private Resolver2<Integer, Void> acceptFirstSuccessfullValue = new CompositeResolver<>(
        new AliasResolver<>("test", 1),
        new AliasResolver<>("test", 2),
        new FailedResolver());
    private Resolver2<Integer, Void> notResolved = new CompositeResolver<>(
        new AliasResolver<>("1", 100),
        new AliasResolver<>("2", 200));
    private Resolver2<Integer, Void> notResolvedWithError = new CompositeResolver<>(
        new AliasResolver<>("1", 100),
        new AliasResolver<>("2", 200),
        new FailedResolver());
    private Resolver2<Integer, Void> failWithAllErrors = new CompositeResolver<>(
        new FailedResolverWithMessage("error1"),
        new FailedResolverWithMessage("error2"));

    private static class FailedResolver implements Resolver2<Integer, Void> {
        @Override
        public Result<Maybe<Integer>, ResolverError> apply(String s, Void meta) {
            return Result.newError(new TestUnknownResolverError(s));
        }
    }

    private static class FailedResolverWithMessage implements Resolver2<Integer, Void> {
        private final String message;

        public FailedResolverWithMessage(String message) {
            this.message = message;
        }

        @Override
        public Result<Maybe<Integer>, ResolverError> apply(String s, Void meta) {
            return Result.newError(new TestUnknownResolverError(message));
        }
    }


    @Test
    public void acceptFirstSuccessfullValue() {
        ResolverTestUtils.assertResolve(acceptFirstSuccessfullValue, "test", 1);
    }

    @Test
    public void notResolvedWithoutErrors() {
        ResolverTestUtils.assertIgnore(notResolved, "3");
    }

    @Test
    public void notResolvedWithErrors() {
        ResolverTestUtils.assertUnknownError(notResolvedWithError, "3", "3");
    }

    @Test
    public void failWithAllErrors() {
        ResolverTestUtils.assertUnknownError(failWithAllErrors, "notused", "error1", "error2");
    }
}
