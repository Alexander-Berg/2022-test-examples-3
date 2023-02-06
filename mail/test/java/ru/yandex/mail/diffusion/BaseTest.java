package ru.yandex.mail.diffusion;

import ru.yandex.mail.diffusion.patch.PatchApplier;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BaseTest {
    protected static final Diffusion DIFFUSION = new Diffusion();
    protected static final FieldMatcher<IncrementalObject> MATCHER = DIFFUSION.fieldMatcherFor(IncrementalObject.class);
    protected static final PatchApplier<IncrementalObject> APPLIER = DIFFUSION.patchApplierFor(IncrementalObject.class);

    protected static <T> T verifyOnce(T mock) {
        return verify(mock, times(1));
    }
}
