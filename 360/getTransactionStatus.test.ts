import { testSaga, expectSaga } from 'redux-saga-test-plan';
import { getContext } from 'redux-saga/effects';

import { getTransactionStatus } from './getTransactionStatus';

describe('Trust/Sagas/GetTransactionStatus', () => {
    const orderId = 'order-id';

    test.each(['paid', 'payment_error'])(
        'should return `%s` status',
        (status: string) => {
            const api = jest.fn();

            testSaga(getTransactionStatus, orderId)
                .next()
                .getContext('api')
                .next(api)
                .call(api, 'ps-billing/get-order-status', { orderId })
                .next({
                    status,
                })
                .returns(status)
                .next()
                .isDone();
        }
    );

    test('should return `timeout`', () => {
        // mocking the delay effect
        const provideDelay = ({ fn }: { fn:{ name: string } }, next: () => void) =>
            fn.name === 'delayP' ? null : next();

        const api = jest.fn(() =>
            Promise.resolve({
                status: 'init',
            })
        );

        return expectSaga(getTransactionStatus, orderId)
            .provide([{ call: provideDelay }, [getContext('api'), api]])
            .run()
            .then((result) => {
                expect(result.returnValue).toEqual('timeout');
            });
    });

    test('should return `network_error`', () => {
        testSaga(getTransactionStatus, orderId)
            .next()
            .throw(new Error())
            .returns('network_error');
    });
});
