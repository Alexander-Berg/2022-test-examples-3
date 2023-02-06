import { countMetrika, reachGoalMetrika } from '../../../../utils/metrika';

import { expectSaga } from 'redux-saga-test-plan';
import { createMockTask } from '@redux-saga/testing-utils';
import { call, fork, race, take, select } from 'redux-saga/effects';

import { init } from './init';
import { getTransactionStatus } from './getTransactionStatus';
import { push } from 'connected-react-router';
import { getTrustLink } from './getTrustLink';
import { paymentAction, setDialogVisible, setError, setShowErrorPage, setTrustLink, } from '../slices';
import { getQueryString } from '../../../helpers/url';
import { Currency, IB2CTariff, PaymentPeriod } from '../../../@types/common';
import { getChosenEmail, getDomainStatus, DomainStatus } from '../../beautifulEmail/slices';
import { isMobile } from '../../../slices';
import { getCurrentWebSubscription } from '../../subscriptionManagement/slice';

jest.mock('../../../../utils/metrika', () => ({
    countMetrika: jest.fn(),
    reachGoalMetrika: jest.fn(),
}));

describe('Trust/Sagas/Init', () => {
    const priceId = 'price-id';
    const orderId = 'order_id';
    const tariff: IB2CTariff = {
        product_id: 'product-id',
        title: 'title',
        best_offer: false,
        features: [],
        prices: [{
            price_id: priceId,
            amount: 123,
            currency: Currency.RUB,
            period: PaymentPeriod.month
        }]
    };

    test('should redirect to `/subscriptions`', () => {
        const forkedTask = createMockTask();

        const trustLinkResponse = {
            payment_form_url: 'link',
            order_id: orderId,
        };

        return expectSaga(init, { priceId, tariff })
            .provide([
                [select(getChosenEmail), ''],
                [select(getDomainStatus), DomainStatus.shouldFetch],
                [select(getCurrentWebSubscription), undefined],
                [select(isMobile), false],
                [fork(getTrustLink, priceId), forkedTask],
                [call(getTransactionStatus, orderId), 'paid'],
                [call(getQueryString), '?uid=1234567890'],
                [
                    race({
                        cancelled: take(setDialogVisible.type),
                        success: take(setTrustLink.type),
                        error: take(setError.type),
                    }),
                    { success: { payload: trustLinkResponse } },
                ],
                [
                    race({
                        closed: take(setDialogVisible.type),
                        paid: take(paymentAction.type),
                        paymentError: take(setError.type),
                    }),
                    {},
                ],
            ])
            .call(countMetrika, 'popup billing', 'success')
            .call(reachGoalMetrika, 'purchase_done', { name: '', price: '' })
            .put(push('/subscriptions?uid=1234567890'))
            .run();
    });

    test('should return if payment form is closed', () => {
        const forkedTask = createMockTask();

        const trustLinkResponse = {
            payment_form_url: 'link',
            order_id: orderId,
        };

        return expectSaga(init, { priceId, tariff })
            .provide([
                [select(getChosenEmail), ''],
                [select(getDomainStatus), DomainStatus.shouldFetch],
                [select(isMobile), false],
                [fork(getTrustLink, priceId), forkedTask],
                [call(getTransactionStatus, orderId), 'paid'],
                [
                    race({
                        cancelled: take(setDialogVisible.type),
                        success: take(setTrustLink.type),
                        error: take(setError.type),
                    }),
                    { success: { payload: trustLinkResponse } },
                ],
                [
                    race({
                        closed: take(setDialogVisible.type),
                        paid: take(paymentAction.type),
                        paymentError: take(setError.type),
                    }),
                    { closed: true },
                ],
            ])
            .not.call(getTransactionStatus, trustLinkResponse)
            .not.put(push('/subscriptions'))
            .run();
    });

    test('should return if cancelled', () => {
        const forkedTask = createMockTask();

        return expectSaga(init, { priceId, tariff })
            .provide([
                [select(getChosenEmail), ''],
                [select(getDomainStatus), DomainStatus.shouldFetch],
                [select(isMobile), false],
                [fork(getTrustLink, priceId), forkedTask],
                [
                    race({
                        cancelled: take(setDialogVisible.type),
                        success: take(setTrustLink.type),
                        error: take(setError.type),
                    }),
                    { cancelled: {} },
                ],
            ])
            .not.race({
                closed: take(setDialogVisible.type),
                paid: take(paymentAction.type),
            })
            .not.put(push('/subscriptions'))
            .run();
    });

    test('should return if error', () => {
        const forkedTask = createMockTask();

        return expectSaga(init, { priceId, tariff })
            .provide([
                [select(getChosenEmail), ''],
                [select(getDomainStatus), DomainStatus.shouldFetch],
                [select(isMobile), false],
                [fork(getTrustLink, priceId), forkedTask],
                [
                    race({
                        cancelled: take(setDialogVisible.type),
                        success: take(setTrustLink.type),
                        error: take(setError.type),
                    }),
                    { error: {} },
                ],
            ])
            .not.race({
                closed: take(setDialogVisible.type),
                paid: take(paymentAction.type),
                paymentError: take(setError.type),
            })
            .not.put(push('/subscriptions'))
            .run();
    });

    test('should show error page and count metrika on error', () => {
        const forkedTask = createMockTask();

        const trustLinkResponse = {
            payment_form_url: 'link',
            order_id: orderId,
        };
        const errorStatus = 'payment_error';

        return expectSaga(init, { priceId, tariff })
            .provide([
                [select(getChosenEmail), ''],
                [select(getDomainStatus), DomainStatus.shouldFetch],
                [select(getCurrentWebSubscription), undefined],
                [select(isMobile), false],
                [fork(getTrustLink, priceId), forkedTask],
                [
                    race({
                        cancelled: take(setDialogVisible.type),
                        success: take(setTrustLink.type),
                        error: take(setError.type),
                    }),
                    { success: { payload: trustLinkResponse } },
                ],
                [
                    race({
                        closed: take(setDialogVisible.type),
                        paid: take(paymentAction.type),
                        paymentError: take(setError.type),
                    }),
                    {},
                ],
                [call(getTransactionStatus, orderId), errorStatus],
            ])
            .put(setShowErrorPage(errorStatus))
            .call(countMetrika, 'popup billing', 'fail', errorStatus)
            .run();
    });
});
