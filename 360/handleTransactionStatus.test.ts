import { testSaga } from 'redux-saga-test-plan';
import { setProcessingPayment, setShowErrorPage } from '../slices';
import { setState as tariffsSetState } from '../../tariffs/slices';
import {
    setLoadingStateAction as subscriptionSetState,
    setFresh as subscriptionSetFresh
} from '../../../features/subscriptionManagement/slice';
import { getTrustTariff, getTrustPriceId } from '../slices';
import { getTransactionStatus } from './getTransactionStatus';
import { countMetrika, reachGoalMetrika } from '../../../../utils/metrika';
import { push } from 'connected-react-router';
import { getQueryString } from '../../../helpers/url';
import handleTransactionStatus from './handleTransactionStatus';
import { isLandingEntry } from '../../../slices';
import { sendPixelMessage, PixelTypes } from '@ps-int/ufo-rocks/lib/helpers/pixel';
import { getShouldShowOnboarding } from '../../onboarding/sagas/getShouldShowOnboarding';

jest.mock('../../../../utils/metrika', () => ({
    countMetrika: jest.fn(),
    reachGoalMetrika: jest.fn(),
}));
jest.mock('../../../helpers/url', () => ({
    getQueryString: jest.fn()
}));

describe('Trust/Sagas/HandleTransactionStatus', () => {
    const orderId = 'abc-abc';
    const query = '?query-abc';

    test("should move to subscriptions' page if transaction is successfull", () => {
        testSaga(handleTransactionStatus, orderId)
            .next()
            .select(getTrustTariff)
            .next({})
            .call(getShouldShowOnboarding)
            .next()
            .put(setProcessingPayment(true))
            .next()
            .call(getTransactionStatus, orderId)
            .next('paid')
            .call(countMetrika, 'popup billing', 'success')
            .next()
            .select(getTrustTariff)
            .next()
            .select(getTrustPriceId)
            .next()
            .call(reachGoalMetrika, 'purchase_done', { name: '', price: '' })
            .next()
            .call(sendPixelMessage, {
                pixelType: PixelTypes.vk,
                goal: 'purchase',
                value: undefined
            })
            .next()
            .call(sendPixelMessage, {
                pixelType: PixelTypes.mt,
                goal: 'purchase_done',
                value: undefined
            })
            .next()
            .put(tariffsSetState(undefined))
            .next()
            .put(subscriptionSetState(undefined))
            .next()
            .put(subscriptionSetFresh(true))
            .next()
            .call(getQueryString)
            .next(query)
            .select(isLandingEntry)
            .next(false)
            .put(push('/subscriptions?query-abc'))
            .next()
            .put(setProcessingPayment(false))
            .next()
            .isDone();
    });

    test('should show error page if transaction is unsuccessfull', () => {
        testSaga(handleTransactionStatus, orderId)
            .next()
            .select(getTrustTariff)
            .next({})
            .call(getShouldShowOnboarding)
            .next()
            .put(setProcessingPayment(true))
            .next()
            .call(getTransactionStatus, orderId)
            .next('payment_error')
            .put(setShowErrorPage('payment_error'))
            .next()
            .call(countMetrika, 'popup billing', 'fail', 'payment_error')
            .next()
            .put(setProcessingPayment(false))
            .next()
            .isDone();
    });

    test('should wait before move if upgrade', () => {
        testSaga(handleTransactionStatus, orderId, false, true)
            .next()
            .select(getTrustTariff)
            .next({})
            .call(getShouldShowOnboarding)
            .next()
            .put(setProcessingPayment(true))
            .next()
            .call(getTransactionStatus, orderId)
            .next('paid')
            .call(countMetrika, 'popup billing', 'success')
            .next()
            .select(getTrustTariff)
            .next()
            .select(getTrustPriceId)
            .next()
            .call(reachGoalMetrika, 'purchase_done', { name: '', price: '' })
            .next()
            .call(sendPixelMessage, {
                pixelType: PixelTypes.vk,
                goal: 'purchase',
                value: undefined
            })
            .next()
            .call(sendPixelMessage, {
                pixelType: PixelTypes.mt,
                goal: 'purchase_done',
                value: undefined
            })
            .next()
            .delay(3000)
            .next()
            .put(tariffsSetState(undefined))
            .next()
            .put(subscriptionSetState(undefined))
            .next()
            .put(subscriptionSetFresh(true))
            .next()
            .call(getQueryString)
            .next(query)
            .select(isLandingEntry)
            .next(false)
            .put(push('/subscriptions?query-abc'))
            .next()
            .put(setProcessingPayment(false))
            .next()
            .isDone();
    });
});
