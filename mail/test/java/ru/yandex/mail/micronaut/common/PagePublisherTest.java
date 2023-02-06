package ru.yandex.mail.micronaut.common;

import lombok.val;
import one.util.streamex.LongStreamEx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.SkipException;

import java.util.List;

class SkipExceptionAdapter implements TestExecutionExceptionHandler {
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        if (!(throwable instanceof SkipException)) {
            throw throwable;
        }
    }
}

@ExtendWith(SkipExceptionAdapter.class)
public class PagePublisherTest extends PublisherVerification<List<Long>> {
    private static final int PAGE_SIZE = 10;
    private static final long MAX_ELEMENTS = PAGE_SIZE * 1000;

    public PagePublisherTest() {
        super(new TestEnvironment(true));
    }

    @Override
    public Publisher<List<Long>> createPublisher(long elements) {
        val values = LongStreamEx.range((elements * PAGE_SIZE) - 1)
            .boxed()
            .toImmutableList();
        return Async.fetchPagesRx(PAGE_SIZE, new TestPageFetcher(values)).log();
    }

    @Override
    public Publisher<List<Long>> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return MAX_ELEMENTS;
    }

    // ============ tests =============

    @Test
    @Override
    public void required_createPublisher1MustProduceAStreamOfExactly1Element() throws Throwable {
        super.required_createPublisher1MustProduceAStreamOfExactly1Element();
    }

    @Test
    @Override
    public void required_createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
        super.required_createPublisher3MustProduceAStreamOfExactly3Elements();
    }

    @Test
    @Override
    public void required_validate_maxElementsFromPublisher() throws Exception {
        super.required_validate_maxElementsFromPublisher();
    }

    @Test
    @Override
    public void required_validate_boundedDepthOfOnNextAndRequestRecursion() throws Exception {
        super.required_validate_boundedDepthOfOnNextAndRequestRecursion();
    }

    @Test
    @Override
    public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements() throws Throwable {
        super.required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements();
    }

    @Test
    @Override
    public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable {
        super.required_spec102_maySignalLessThanRequestedAndTerminateSubscription();
    }

    @Test
    @Override
    public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable {
        super.stochastic_spec103_mustSignalOnMethodsSequentially();
    }

    @Test
    @Override
    public void optional_spec104_mustSignalOnErrorWhenFails() throws Throwable {
        super.optional_spec104_mustSignalOnErrorWhenFails();
    }

    @Test
    @Override
    public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
        super.required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates();
    }

    @Test
    @Override
    public void optional_spec105_emptyStreamMustTerminateBySignallingOnComplete() throws Throwable {
        super.optional_spec105_emptyStreamMustTerminateBySignallingOnComplete();
    }

    @Test
    @Override
    public void untested_spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled() throws Throwable {
        super.untested_spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled();
    }

    @Test
    @Override
    public void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() throws Throwable {
        super.required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled();
    }

    @Test
    @Override
    public void untested_spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() throws Throwable {
        super.untested_spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled();
    }

    @Test
    @Override
    public void untested_spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() throws Throwable {
        super.untested_spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals();
    }

    @Test
    @Override
    public void untested_spec109_subscribeShouldNotThrowNonFatalThrowable() throws Throwable {
        super.untested_spec109_subscribeShouldNotThrowNonFatalThrowable();
    }

    @Test
    @Override
    public void required_spec109_subscribeThrowNPEOnNullSubscriber() throws Throwable {
        super.required_spec109_subscribeThrowNPEOnNullSubscriber();
    }

    @Test
    @Override
    public void required_spec109_mustIssueOnSubscribeForNonNullSubscriber() throws Throwable {
        super.required_spec109_mustIssueOnSubscribeForNonNullSubscriber();
    }

    @Test
    @Override
    public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe() throws Throwable {
        super.required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe();
    }

    @Test
    @Override
    public void untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice() throws Throwable {
        super.untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice();
    }

    @Test
    @Override
    public void optional_spec111_maySupportMultiSubscribe() throws Throwable {
        super.optional_spec111_maySupportMultiSubscribe();
    }

    @Test
    @Override
    public void optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals() throws Throwable {
        super.optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals();
    }

    @Test
    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne() throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne();
    }

    @Test
    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront() throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront();
    }

    @Test
    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected() throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected();
    }

    @Test
    @Override
    public void required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe() throws Throwable {
        super.required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe();
    }

    @Test
    @Override
    public void required_spec303_mustNotAllowUnboundedRecursion() throws Throwable {
        super.required_spec303_mustNotAllowUnboundedRecursion();
    }

    @Test
    @Override
    public void untested_spec304_requestShouldNotPerformHeavyComputations() throws Exception {
        super.untested_spec304_requestShouldNotPerformHeavyComputations();
    }

    @Test
    @Override
    public void untested_spec305_cancelMustNotSynchronouslyPerformHeavyComputation() throws Exception {
        super.untested_spec305_cancelMustNotSynchronouslyPerformHeavyComputation();
    }

    @Test
    @Override
    public void required_spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable {
        super.required_spec306_afterSubscriptionIsCancelledRequestMustBeNops();
    }

    @Test
    @Override
    public void required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops() throws Throwable {
        super.required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops();
    }

    @Test
    @Override
    public void required_spec309_requestZeroMustSignalIllegalArgumentException() throws Throwable {
        super.required_spec309_requestZeroMustSignalIllegalArgumentException();
    }

    @Test
    @Override
    public void required_spec309_requestNegativeNumberMustSignalIllegalArgumentException() throws Throwable {
        super.required_spec309_requestNegativeNumberMustSignalIllegalArgumentException();
    }

    @Test
    @Override
    public void optional_spec309_requestNegativeNumberMaySignalIllegalArgumentExceptionWithSpecificMessage() throws Throwable {
        super.optional_spec309_requestNegativeNumberMaySignalIllegalArgumentExceptionWithSpecificMessage();
    }

    @Test
    @Override
    public void required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling() throws Throwable {
        super.required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling();
    }

    @Test
    @Override
    public void required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber() throws Throwable {
        super.required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber();
    }

    @Test
    @Override
    public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
        super.required_spec317_mustSupportAPendingElementCountUpToLongMaxValue();
    }

    @Test
    @Override
    public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() throws Throwable {
        super.required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue();
    }

    @Test
    @Override
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
        super.required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue();
    }
}
