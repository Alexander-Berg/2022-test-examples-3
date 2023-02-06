import { testSaga } from 'redux-saga-test-plan';

import { unsubscribe } from './unsubscribe';
import {
    unsubscribeAction,
    setCancellingAction,
    CancellationStatus,
    setAutoProlong,
} from '../slice';
import { getTariffs } from '../../tariffs/sagas/getTariffs';
import { getServices } from './getServices';

describe('Subscription/Sagas/Unsubscribe', () => {
    const serviceId = 'service-id';
    const api = jest.fn();

    test('should unsubscribe', () => {
        testSaga(unsubscribe, unsubscribeAction(serviceId))
            .next()
            .put(setCancellingAction({ serviceId, status: CancellationStatus.inProgress }))
            .next()
            .getContext('api')
            .next(api)
            .call(api, 'ps-billing/unsubscribe', { serviceId })
            .next()
            .put(setAutoProlong({ serviceId, value: false }))
            .next()
            .put(setCancellingAction({ serviceId, status: undefined }))
            .next()
            .spawn(getServices)
            .next()
            .spawn(getTariffs)
            .next()
            .isDone();
    });

    test('should set error status', () => {
        testSaga(unsubscribe, unsubscribeAction(serviceId))
            .next()
            .throw(new Error())
            .put(setCancellingAction({ serviceId, status: CancellationStatus.error }))
            .next()
            .isDone();
    });
});
