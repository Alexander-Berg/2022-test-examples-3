import { testSaga, expectSaga } from 'redux-saga-test-plan';

import { getTrustLink } from './getTrustLink';
import { setTrustLink, setError, TrustError } from '../slices';
import { getContext } from 'redux-saga/effects';
import { isMobile, isNewPaymentFormExp } from '../../../slices';

describe('Trust/Sagas/GetTrustLinnk', () => {
    const priceId = 'product-id';

    test('should put trust link to store', () => {
        const api = jest.fn();

        const response = {
            order_id: 'order_id',
            payment_form_url: 'http://fake.url',
        };

        testSaga(getTrustLink, priceId)
            .next()
            .getContext('api')
            .next(api)
            .select(isNewPaymentFormExp)
            .next(false)
            .select(isMobile)
            .next(false)
            .call(api, 'ps-blling/get-trust-link', {
                priceId, disableTrustHeader: false, productKey: '', priceKey: '', isUsdPaymentExp: false
            })
            .next(response)
            .put(setTrustLink(response))
            .next()
            .cancelled()
            .next()
            .isDone();
    });

    test('should set error to `linkFetchError`', () => {
        const api = jest.fn(() => Promise.reject('0_0'));

        return expectSaga(getTrustLink, priceId)
            .provide([[getContext('api'), api]])
            .put(setError(TrustError.linkFetchError))
            .run();
    });
});
