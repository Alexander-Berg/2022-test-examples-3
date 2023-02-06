import { expectSaga, testSaga } from 'redux-saga-test-plan';
import { getContext, call } from 'redux-saga/effects';
import { push } from 'connected-react-router';

import { getServices } from './getServices';
import subscriptionReducer, { setLoadingStateAction } from '../slice';
import { LoadingState } from '../../../@types/common';
import logSagaError from '../../../helpers/logSagaError';
import { getQueryString } from '../../../helpers/url';

describe('Subscription/Sagas/GetServices', () => {
    it('should fetch list of subscriptions', async() => {
        const mockResponse = {
            items: [
                {
                    status: 'ACTIVE',
                    billing_status: 'paid',
                    auto_prolong_enabled: true,
                    product: {
                        product_type: 'subscription',
                        features: [],
                        price: {},
                    },
                },
            ],
        };
        const api = jest.fn(() => Promise.resolve(mockResponse));

        const { storeState } = await expectSaga(getServices)
            .withReducer(subscriptionReducer)
            .provide([
                [getContext('api'), api],
                [call(getQueryString), '?uid=123'],
            ])
            .run();

        expect(storeState).toMatchSnapshot();
        expect(api).toHaveBeenCalledWith('ps-billing/services', {});
    });

    it('should redirect to main page if no personal subscriptions', () => {
        const mockResponse = {
            items: [
                {
                    status: 'ACTIVE',
                    billing_status: 'paid',
                    auto_prolong_enabled: true,
                    product: {
                        product_type: 'group',
                        features: [],
                        price: {},
                    },
                },
            ],
        };

        const api = jest.fn(() => Promise.resolve(mockResponse));

        return expectSaga(getServices)
            .withReducer(subscriptionReducer)
            .provide([
                [getContext('api'), api],
                [call(getQueryString), '?uid=123'],
            ])
            .put(push('/?uid=123'))
            .put(setLoadingStateAction(undefined))
            .run();
    });

    it('should set error state ang log saga error if invalid response', () => {
        const mockResponse = {
            error: 'invalid response',
        };
        const api = jest.fn();

        testSaga(getServices)
            .next()
            .getContext('api')
            .next(api)
            .call(api, 'ps-billing/services', {})
            .next(mockResponse)
            .put(setLoadingStateAction(LoadingState.error))
            .next()
            .spawn(
                logSagaError,
                'subscriptionManagement',
                'getServices',
                Error('invalid response')
            )
            .next()
            .isDone();
    });
});
