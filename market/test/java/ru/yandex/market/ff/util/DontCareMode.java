package ru.yandex.market.ff.util;

import java.util.List;

import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.MatchableInvocation;
import org.mockito.verification.VerificationMode;

import static org.mockito.internal.invocation.InvocationMarker.markVerified;
import static org.mockito.internal.invocation.InvocationsFinder.findInvocations;

public class DontCareMode implements VerificationMode {
    public DontCareMode() {
    }

    @Override
    public void verify(VerificationData data) {
        List<Invocation> invocations = data.getAllInvocations();
        MatchableInvocation wanted = data.getTarget();

        List<Invocation> found = findInvocations(invocations, wanted);
        removeAlreadyVerified(found);
        markVerified(found, wanted);
    }

    private void removeAlreadyVerified(List<Invocation> invocations) {
        invocations.removeIf(Invocation::isVerified);
    }

    @Override
    public VerificationMode description(String description) {
        return VerificationModeFactory.description(this, description);
    }
}
