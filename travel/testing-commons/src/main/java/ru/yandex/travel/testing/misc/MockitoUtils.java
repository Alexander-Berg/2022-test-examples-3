package ru.yandex.travel.testing.misc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import org.mockito.Mockito;
import org.mockito.internal.invocation.InvocationsFinder;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

import ru.yandex.travel.testing.TestUtils;

public class MockitoUtils {
    public static Answer<Void> voidAnswer(Consumer<InvocationOnMock> answer) {
        return inv -> {
            answer.accept(inv);
            return null;
        };
    }

    public static <T> int getMockInvocations(T mock, Consumer<T> methodCall) {
        GetInvocationsHelper initialInvocations = new GetInvocationsHelper();
        methodCall.accept(Mockito.verify(mock, initialInvocations));
        return initialInvocations.invocations;
    }

    public static <T> void waitForMockCalls(T mock, Consumer<T> methodCall, int minCalls,
                                            Duration timeout, Duration retryDelay, String description) {
        int initialInvocations = getMockInvocations(mock, methodCall);
        waitForMockCalls(mock, methodCall, initialInvocations, minCalls, timeout, retryDelay, description);
    }

    public static <T> int waitForMockCalls(T mock, Consumer<T> methodCall, int initialInvocations, int minCalls,
                                           Duration timeout, Duration retryDelay, String description) {
        Instant deadline = Instant.now().plus(timeout);
        int currentInvocations = 0;
        while (Instant.now().isBefore(deadline)) {
            currentInvocations = getMockInvocations(mock, methodCall);
            if (currentInvocations >= initialInvocations + minCalls) {
                return currentInvocations;
            }
            TestUtils.sleep(retryDelay);
        }
        throw new RuntimeException(String.format(
                "The required amount (expected %s, actual %s) of mock calls hasn't happened: %s",
                minCalls, (currentInvocations - initialInvocations), description
        ));
    }

    private static class GetInvocationsHelper implements VerificationMode {
        private Integer invocations;

        @Override
        public void verify(VerificationData data) {
            Preconditions.checkArgument(invocations == null, "shouldn't be re-used");
            List<Invocation> relevantInvocations = InvocationsFinder
                    .findInvocations(data.getAllInvocations(), data.getTarget());
            invocations = relevantInvocations.size();
        }

        @Override
        public VerificationMode description(String description) {
            throw new UnsupportedOperationException();
        }
    }
}
