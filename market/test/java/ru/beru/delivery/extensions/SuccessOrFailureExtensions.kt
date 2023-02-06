package ru.yandex.market.tpl.courier.extensions

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.equalityMatcher
import ru.yandex.market.tpl.courier.arch.fp.Failure
import ru.yandex.market.tpl.courier.arch.fp.Success
import ru.yandex.market.tpl.courier.arch.fp.SuccessOrFailure

fun <S, F> successWith(successValueMatcher: Matcher<S>): Matcher<SuccessOrFailure<S, F>> {
    return SuccessMatcher(successValueMatcher)
}

fun <S, F> successWith(value: S): Matcher<SuccessOrFailure<S, F>> {
    return SuccessMatcher(equalityMatcher(value))
}

fun <S, F> failureWith(failureValueMatcher: Matcher<F>): Matcher<SuccessOrFailure<S, F>> {
    return FailureMatcher(failureValueMatcher)
}

fun <S, F> someFailure(): Matcher<SuccessOrFailure<S, F>> {
    return FailureMatcher(valueMatcher = null)
}

private class SuccessMatcher<S, F>(
    private val valueMatcher: Matcher<S>,
) : Matcher<SuccessOrFailure<S, F>> {

    override fun test(value: SuccessOrFailure<S, F>): MatcherResult {
        return when (value) {
            is Success -> valueMatcher.test(value.successValue)
            is Failure -> MatcherResult(
                passed = false,
                failureMessage = "Ожидался Success, но вернулся $value.",
                negatedFailureMessage = "Ожидался Failure, но вернулся $value."
            )
        }
    }
}

private class FailureMatcher<S, F>(
    private val valueMatcher: Matcher<F>?,
) : Matcher<SuccessOrFailure<S, F>> {

    override fun test(value: SuccessOrFailure<S, F>): MatcherResult {
        return when (value) {
            is Success -> MatcherResult(
                passed = false,
                failureMessage = "Ожидался Failure, но вернулся $value.",
                negatedFailureMessage = "Ожидался Success, но вернулся $value."
            )
            is Failure -> valueMatcher?.test(value.failureValue) ?: MatcherResult(
                passed = true,
                failureMessage = "Failure was expected, but value is $value.",
                negatedFailureMessage = "Success was expected, but value is $value.",
            )
        }
    }
}