package ru.yandex.market.deepmind.common.assertions.custom;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Iterables;
import org.assertj.core.internal.Objects;
import org.assertj.core.internal.Strings;

import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusResult;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusResult.Status;
import ru.yandex.market.deepmind.common.services.statuses.pojo.StatusWarning;

public class SskuMskuStatusResultAssertions
    extends AbstractAssert<SskuMskuStatusResultAssertions, SskuMskuStatusResult> {

    private final Objects objects = Objects.instance();
    private final Iterables iterables = Iterables.instance();
    private final Strings strings = Strings.instance();

    public SskuMskuStatusResultAssertions(SskuMskuStatusResult actual) {
        super(actual, SskuMskuStatusResultAssertions.class);
    }

    public SskuMskuStatusResultAssertions isFailed() {
        super.isNotNull();
        if (actual.getStatus() != Status.FAILED) {
            failWithMessage("Expected result to be <%s>, but actually is <%s>\nresult: <%s>",
                Status.FAILED, actual.getStatus(), actual);
        }
        return myself;
    }

    public SskuMskuStatusResultAssertions isFailedWithMessageContaining(String message) {
        super.isNotNull();
        if (actual.getStatus() != Status.FAILED) {
            failWithMessage("Expected result to be <%s> with message containing <%s>, " +
                "but actually is <%s>\nresult: <%s>", Status.FAILED, message, actual.getStatus(), actual);
        }
        strings.assertContains(info, actual.getErrorMessage(), message);
        return myself;
    }

    public SskuMskuStatusResultAssertions isPartialOk() {
        super.isNotNull();
        if (actual.getStatus() != Status.PARTIAL_OK) {
            failWithMessage("Expected result to be <%s>, but actually is <%s>\nresult: <%s>",
                Status.PARTIAL_OK, actual.getStatus(), actual);
        }
        return myself;
    }

    public SskuMskuStatusResultAssertions isOk() {
        super.isNotNull();
        if (actual.getStatus() != Status.OK) {
            failWithMessage("Expected result to be <%s>, but actually is <%s>\nresult: <%s>",
                Status.OK, actual.getStatus(), actual);
        }
        return myself;
    }

    public SskuMskuStatusResultAssertions totalSaved(int expectedCount) {
        super.isNotNull();
        objects.assertEqual(info, actual.getSavedMskuIds().size() + actual.getSavedSskuKeys().size(), expectedCount);
        return myself;
    }

    public SskuMskuStatusResultAssertions savedMskuIds(Long... mskuIds) {
        super.isNotNull();
        iterables.assertContainsExactlyInAnyOrder(info, actual.getSavedMskuIds(), mskuIds);
        return myself;
    }

    public SskuMskuStatusResultAssertions savedSskuIds(int supplierId, String shopSku) {
        return savedSskuIds(new ServiceOfferKey(supplierId, shopSku));
    }

    public SskuMskuStatusResultAssertions savedSskuIds(int supplierId1, String shopSku1,
                                                       int supplierId2, String shopSku2) {
        return savedSskuIds(new ServiceOfferKey(supplierId1, shopSku1), new ServiceOfferKey(supplierId2, shopSku2));
    }

    public SskuMskuStatusResultAssertions savedSskuIds(ServiceOfferKey... shopSkuKeys) {
        super.isNotNull();
        iterables.assertContainsExactlyInAnyOrder(info, actual.getSavedSskuKeys(), shopSkuKeys);
        return myself;
    }

    public SskuMskuStatusResultAssertions containsWarningsExactlyInAnyOrder(StatusWarning<?>... warnings) {
        super.isNotNull();
        iterables.assertContainsExactlyInAnyOrder(info, actual.getWarnings(), warnings);
        return myself;
    }

    public SskuMskuStatusResultAssertions containsErrorsExactlyInAnyOrder(StatusWarning<?>... warnings) {
        super.isNotNull();
        iterables.assertContainsExactlyInAnyOrder(info, actual.getErrors(), warnings);
        return myself;
    }
}
