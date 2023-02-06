import { testSaga } from 'redux-saga-test-plan';
import { getTariffs } from './getTariffs';
import {
    setTariffs,
    setState,
    setDiscountUntil,
    setOrderStatus,
    setActivePromo
} from '../slices';
import {
    LoadingState,
    OrderStatus,
} from '../../../@types/common';
import logSagaError from '../../../helpers/logSagaError';
import {
    isUSDPaymentExp,
    isUSDPaymentTRExp,
    isUSDPaymentBYExp
} from '../../../slices';

describe('Tariffs/Sagas/GetTariffs', () => {
    it('should pass mail pro experiment flag and fetch available tariffs', () => {
        const api = jest.fn();
        const response = {
            items: [],
            order_status: OrderStatus.paid
        };

        const isUsdPaymentExp = false;
        const isUsdPaymentTRExp = false;
        const isUsdPaymentBYExp = false;

        testSaga(getTariffs)
            .next()
            .getContext('api')
            .next(api)
            .select(isUSDPaymentExp)
            .next(isUsdPaymentExp)
            .select(isUSDPaymentTRExp)
            .next(isUsdPaymentTRExp)
            .select(isUSDPaymentBYExp)
            .next(isUsdPaymentBYExp)
            .call(api, 'ps-billing/tariffs', { isUsdPaymentExp })
            .next(response)
            .put(setTariffs(response.items))
            .next(response)
            .put(setOrderStatus(response.order_status))
            .next()
            .put(setDiscountUntil(undefined))
            .next()
            .put(setActivePromo(undefined))
            .next()
            .put(setState(LoadingState.loaded))
            .next()
            .isDone();
    });

    it('should handle error', () => {
        const error = new Error();

        testSaga(getTariffs)
            .next()
            .throw(error)
            .put(setState(LoadingState.error))
            .next()
            .spawn(logSagaError, 'tariffs', 'getTariffs', error)
            .next()
            .isDone();
    });
});
