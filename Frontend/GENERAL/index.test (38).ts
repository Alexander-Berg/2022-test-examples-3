import thunk from 'redux-thunk';
import createMockStore from 'redux-mock-store';

import { logError } from '../../../lib/rum';
import { fetchOrderDelivery as fetchOrderDeliveryApi } from '../../../lib/api/order-delivery';
import { AppDispatch, RootReducer } from '../..';
import * as mocks from './index.mocks';
import reducer, {
    startOrderDeliveryFetch,
    successOrderDeliveryFetch,
    failedOrderDeliveryFetch,
    orderDeliverySelector,
    orderDeliveryErrorSelector,
    orderDeliveryLoadingSelector,
    fetchOrderDelivery,
} from '.';

// Мокаем хэлпер для вызова АПИ
jest.mock('../../helpers/api', () => ({ callAPI: value => value }));

// Мокаем rum и АПИ, чтобы отследить их вызовы, сохраняя остальные экспорты актуальными
jest.mock('../../../lib/rum', () => ({ logError: jest.fn(), logWarning: jest.fn() }));
jest.mock('../../../lib/api/order-delivery', () => {
    const actualOrderDelivery = jest.requireActual('../../../lib/api/order-delivery');

    return { ...actualOrderDelivery, fetchOrderDelivery: jest.fn() };
});

const mockStore = createMockStore<Partial<RootReducer>, AppDispatch>([thunk]);

describe('order-delivery slice', () => {
    describe('reducer and selectors', () => {
        const prepareRootState = (slice: RootReducer['orderDelivery']): RootReducer => {
            return { orderDelivery: slice } as RootReducer;
        };

        it('should return the initital state on initialize', () => {
            const nextSlice = reducer(undefined, { type: 'INIT_ACTION' });

            expect(nextSlice).toEqual({ ui: {} });
        });

        it('should return fetching state on start fetch', () => {
            const nextSlice = reducer(
                {
                    ui: {},
                },
                startOrderDeliveryFetch()
            );
            const rootState = prepareRootState(nextSlice);

            expect(nextSlice).toEqual({
                ui: {
                    isLoading: true,
                },
            });

            // Должен выставляться флаг загрузки
            expect(orderDeliveryLoadingSelector(rootState)).toBe(true);
        });

        it('should return fetching state on start fetch (remove old data)', () => {
            const nextSlice = reducer(
                {
                    data: mocks.orderDeliveryInfo,
                    ui: {
                        isLoading: false,
                    },
                },
                startOrderDeliveryFetch()
            );
            const rootState = prepareRootState(nextSlice);

            expect(nextSlice).toEqual({
                ui: {
                    isLoading: true,
                },
            });

            // Должны очищаться данные и выставляться флаг загрузки
            expect(orderDeliverySelector(rootState)).toBeUndefined();
            expect(orderDeliveryLoadingSelector(rootState)).toBe(true);
        });

        it('should return state with data on success fetch', () => {
            const nextSlice = reducer(
                {
                    ui: {
                        isLoading: true,
                    },
                },
                successOrderDeliveryFetch(mocks.orderDeliveryInfo)
            );
            const rootState = prepareRootState(nextSlice);

            expect(nextSlice).toEqual({
                data: mocks.orderDeliveryInfo,
                ui: {
                    isLoading: false,
                },
            });

            // В state складываются данные, флаг загрузки сбрасывается
            expect(orderDeliverySelector(rootState)).toEqual(mocks.orderDeliveryInfo);
            expect(orderDeliveryLoadingSelector(rootState)).toBe(false);
        });

        it('should return state with error on failed fetch', () => {
            const nextSlice = reducer(
                {
                    ui: {
                        isLoading: true,
                    },
                },
                failedOrderDeliveryFetch(mocks.orderDeliveryError)
            );
            const rootState = prepareRootState(nextSlice);

            expect(nextSlice).toEqual({
                ui: {
                    isLoading: false,
                    error: mocks.orderDeliveryError,
                },
            });

            // В state складывается ошибка, флаг загрузки сбрасывается
            expect(orderDeliveryErrorSelector(rootState)).toEqual(mocks.orderDeliveryError);
            expect(orderDeliveryLoadingSelector(rootState)).toBe(false);
        });
    });

    describe('thunks', () => {
        describe('fetchOrderDelivery()', () => {
            it('should throw an exception if user is not authenticated', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.anonymousUser,
                    session: mocks.session.userAuthorized,
                    orderDelivery: { ui: {} },
                });

                // Выполнение thunk'а должно закончиться exception'ом
                const thunk = store.dispatch(fetchOrderDelivery(mocks.orderId));
                await expect(thunk).rejects.toThrowError(new Error('Authentication required for fetchOrderDelivery()'));
                // Также проверяем, что не было выполнено ни одного экшена
                expect(store.getActions()).toEqual([]);
            });

            it('should throw an exception if the user id is not defined', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.userAuthorized,
                    session: mocks.session.notInitialized,
                    orderDelivery: { ui: {} },
                });

                // Выполнение thunk'а должно закончиться exception'ом
                const thunk = store.dispatch(fetchOrderDelivery(mocks.orderId));
                await expect(thunk).rejects.toThrowError(new Error('fetchOrderDelivery. userId missing'));
                // Также проверяем, что не было выполнено ни одного экшена
                expect(store.getActions()).toEqual([]);
            });

            it('should request order delivery data and save it', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.userAuthorized,
                    session: mocks.session.userAuthorized,
                    orderDelivery: { ui: {} },
                });

                // Меняем реализацию АПИ, чтобы ответ был 200 ОК
                (fetchOrderDeliveryApi as jest.Mock).mockImplementation(() =>
                    Promise.resolve({
                        data: mocks.orderDeliveryResponse,
                    })
                );

                await store.dispatch(fetchOrderDelivery(mocks.orderId));

                // Проверяем вызов экшенов и их порядок
                const expectedActions = [startOrderDeliveryFetch(), successOrderDeliveryFetch(mocks.orderDeliveryInfo)];

                expect(store.getActions()).toEqual(expectedActions);
            });

            it('should handle API error and save it', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.userAuthorized,
                    session: mocks.session.userAuthorized,
                    orderDelivery: { ui: {} },
                });
                // Мокаем ошибку АПИ
                const mockError = { data: mocks.orderDeliveryError };

                // Меняем реализацию АПИ, чтобы ответ был с ошибкой
                (fetchOrderDeliveryApi as jest.Mock).mockImplementation(() => Promise.reject(mockError));

                await store.dispatch(fetchOrderDelivery(mocks.orderId));

                // Проверяем вызов экшенов и их порядок, а также логирование
                const expectedActions = [startOrderDeliveryFetch(), failedOrderDeliveryFetch(mocks.orderDeliveryError)];

                expect(store.getActions()).toEqual(expectedActions);
                expect(logError).toHaveBeenCalled();
                expect(logError).toHaveBeenCalledWith(
                    {
                        type: 'network',
                        method: 'fetchOrderDelivery',
                        message: mockError.data.message,
                        additional: mockError,
                    },
                    mockError
                );
            });
        });
    });
});
