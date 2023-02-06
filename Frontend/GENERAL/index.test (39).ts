import thunk from 'redux-thunk';
import createMockStore from 'redux-mock-store';

import { logError } from '../../../lib/rum';
import { trackOrder } from '../../../lib/api/order-tracking';
import { prepareSlice } from '../../../lib/tests';
import { reachGoal, LavkaMetrikaGoals } from '../../../lib/metrika';
import { AppDispatch, RootReducer } from '../..';
import * as mocks from './index.mocks';
import reducer, {
    startOrderTrackingFetch,
    successOrderTrackingFetch,
    failedOrderTrackingFetch,
    startOrderTracking,
    clearOrderTrackingData,
    orderSelector,
    orderIdSelector,
    isLoadingSelector,
    errorSelector,
    fetchOrderTracking,
} from '.';

// Мокаем хэлпер для вызова АПИ
jest.mock('../../helpers/api', () => ({ callAPI: value => value }));

// Мокаем rum, метрику и АПИ, чтобы отследить их вызовы, сохраняя остальные экспорты актуальными
jest.mock('../../../lib/rum', () => ({ logError: jest.fn(), logWarning: jest.fn() }));
jest.mock('../../../lib/metrika', () => {
    const actualMetrika = jest.requireActual('../../../lib/metrika');

    return { ...actualMetrika, reachGoal: jest.fn() };
});
jest.mock('../../../lib/api/order-tracking', () => {
    const actualOrderTracking = jest.requireActual('../../../lib/api/order-tracking');

    return { ...actualOrderTracking, trackOrder: jest.fn() };
});

const mockStore = createMockStore<Partial<RootReducer>, AppDispatch>([thunk]);

describe('order-tracking slice', () => {
    describe('reducer and selectors', () => {
        const prepareRootState = (slice: RootReducer['orderTracking']): RootReducer => {
            return { orderTracking: slice } as RootReducer;
        };

        it('should return the initial state on initialize', () => {
            const nextSlice = reducer(undefined, { type: 'INIT_ACTION' });

            expect(nextSlice).toEqual({ ui: {} });
        });

        it('should return fetching state on start fetch', () => {
            const nextSlice = reducer(
                prepareSlice({
                    orderId: mocks.orderId,
                    ui: {},
                }),
                startOrderTrackingFetch()
            );
            const rootState = prepareRootState(nextSlice);

            expect(nextSlice).toEqual(
                prepareSlice({
                    orderId: mocks.orderId,
                    ui: {
                        isLoading: true,
                    },
                })
            );

            // Должен сохраняться orderId и выставляться флаг загрузки
            expect(orderIdSelector(rootState)).toBe(mocks.orderId);
            expect(isLoadingSelector(rootState)).toBe(true);
        });

        it('should return state with data on success fetch', () => {
            const nextSlice = reducer(
                prepareSlice({
                    orderId: mocks.orderId,
                    ui: {
                        isLoading: true,
                    },
                }),
                successOrderTrackingFetch(mocks.orderTrackingData)
            );
            const rootState = prepareRootState(nextSlice);

            expect(nextSlice).toEqual(
                prepareSlice({
                    orderId: mocks.orderId,
                    data: mocks.orderTrackingData,
                    ui: {
                        isLoading: false,
                    },
                })
            );

            // В state складываются данные, должнен сохраняться orderId, флаг загрузки сбрасывается
            expect(orderSelector(rootState)).toEqual(mocks.orderTrackingData);
            expect(orderIdSelector(rootState)).toBe(mocks.orderId);
            expect(isLoadingSelector(rootState)).toBe(false);
        });

        it('should return state with error on failed fetch', () => {
            const nextSlice = reducer(
                prepareSlice({
                    orderId: mocks.orderId,
                    ui: {
                        isLoading: true,
                    },
                }),
                failedOrderTrackingFetch(mocks.orderTrackingError)
            );
            const rootState = prepareRootState(nextSlice);

            expect(nextSlice).toEqual(
                prepareSlice({
                    orderId: mocks.orderId,
                    ui: {
                        isLoading: false,
                        error: mocks.orderTrackingError,
                    },
                })
            );

            // В state складывается ошибка, должнен сохраняться orderId, флаг загрузки сбрасывается
            expect(orderIdSelector(rootState)).toBe(mocks.orderId);
            expect(isLoadingSelector(rootState)).toBe(false);
            expect(errorSelector(rootState)).toEqual(mocks.orderTrackingError);
        });

        it('should return state with order id on start tracking', () => {
            const nextSlice = reducer(prepareSlice({ ui: {} }), startOrderTracking({ orderId: mocks.orderId }));
            const rootState = prepareRootState(nextSlice);

            expect(nextSlice).toEqual(
                prepareSlice({
                    orderId: mocks.orderId,
                    ui: {},
                })
            );

            // В state складывается orderId
            expect(orderIdSelector(rootState)).toBe(mocks.orderId);
        });

        it('should return clean state on clear tracking data', () => {
            const nextSlice = reducer(
                prepareSlice({
                    orderId: mocks.orderId,
                    data: mocks.orderTrackingData,
                    ui: {
                        isLoading: false,
                    },
                }),
                clearOrderTrackingData()
            );

            expect(nextSlice).toEqual(prepareSlice({ ui: {} }));
        });
    });

    describe('thunks', () => {
        describe('fetchOrderTracking()', () => {
            beforeEach(() => {
                jest.useFakeTimers();
            });

            afterEach(() => {
                jest.clearAllMocks();
            });

            it('should throw an exception if user is not authenticated', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.anonymousUser,
                    session: mocks.session.userAuthorized,
                    orderTracking: prepareSlice({ ui: {} }),
                });

                // Выполнение thunk'а должно закончиться exception'ом
                const thunk = store.dispatch(fetchOrderTracking());
                await expect(thunk).rejects.toThrowError(new Error('Authentication required for fetchOrderTracking()'));
                // Также проверяем, что не было выполнено ни одного экшена
                expect(store.getActions()).toEqual([]);
            });

            it('should throw an exception if the user id is not defined', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.userAuthorized,
                    session: mocks.session.notInitialized,
                    orderTracking: prepareSlice({ ui: {} }),
                });

                // Выполнение thunk'а должно закончиться exception'ом
                const thunk = store.dispatch(fetchOrderTracking());
                await expect(thunk).rejects.toThrowError(new Error('fetchOrderTracking. userId missing'));
                // Также проверяем, что не было выполнено ни одного экшена
                expect(store.getActions()).toEqual([]);
            });

            it('should request order tracking data and save it (status created)', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.userAuthorized,
                    session: mocks.session.userAuthorized,
                    orderTracking: prepareSlice({
                        orderId: mocks.orderId,
                        ui: {},
                    }),
                });

                // Меняем реализацию АПИ, чтобы ответ был 200 ОК со статусом - "Создано"
                (trackOrder as jest.Mock).mockImplementation(() =>
                    Promise.resolve({
                        data: mocks.orderTrackingResponse,
                    })
                );

                await store.dispatch(fetchOrderTracking());

                // Проверяем вызов экшенов и их порядок, а также логирование в метрику
                const expectedActions = [startOrderTrackingFetch(), successOrderTrackingFetch(mocks.orderTrackingData)];

                expect(store.getActions()).toEqual(expectedActions);
                expect(reachGoal).toHaveBeenCalled();
                expect(reachGoal).toHaveBeenCalledWith(LavkaMetrikaGoals.OrderTracking, {
                    state: mocks.orderTrackingData.status,
                    order_id: mocks.orderId,
                });

                // Очищаем уже протестированные экшены
                store.clearActions();
                // Запускаем ожидающие таймеры, чтобы выполнить следующий запрос. Убираем mock с setTimeout
                jest.runOnlyPendingTimers();
                jest.useRealTimers();

                // Проверяем, что экшены выполнились по второму циклу. Делаем это в другой макротаске,
                // чтобы сравнить после выполнения thunk'а
                await new Promise(resolve => setTimeout(resolve));
                expect(store.getActions()).toEqual(expectedActions);
            });

            it('should request order tracking data and save it (status delivered)', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.userAuthorized,
                    session: mocks.session.userAuthorized,
                    orderTracking: prepareSlice({
                        orderId: mocks.orderId,
                        ui: {},
                    }),
                });

                // Меняем реализацию АПИ, чтобы ответ был 200 ОК со статусом - "Доставлено"
                (trackOrder as jest.Mock).mockImplementation(() =>
                    Promise.resolve({
                        data: mocks.orderTrackingDeliveredResponse,
                    })
                );

                await store.dispatch(fetchOrderTracking());

                // Проверяем вызов экшенов и их порядок, а также логирование в метрику
                const expectedActions = [
                    startOrderTrackingFetch(),
                    successOrderTrackingFetch(mocks.orderTrackingDeliveredData),
                ];

                expect(store.getActions()).toEqual(expectedActions);
                expect(reachGoal).toHaveBeenCalled();
                expect(reachGoal).toHaveBeenCalledWith(LavkaMetrikaGoals.OrderTracking, {
                    state: mocks.orderTrackingDeliveredData.status,
                    order_id: mocks.orderId,
                });
                expect(setTimeout).not.toHaveBeenCalled();
            });

            it('should request order tracking data and clear store', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.userAuthorized,
                    session: mocks.session.userAuthorized,
                    orderTracking: prepareSlice({
                        orderId: mocks.orderId,
                        ui: {},
                    }),
                });

                // Меняем реализацию АПИ, чтобы ответ был с пустым списком заказов
                (trackOrder as jest.Mock).mockImplementation(() =>
                    Promise.resolve({
                        data: mocks.emptyOrderTrackingResponse,
                    })
                );

                await store.dispatch(fetchOrderTracking());

                // Проверяем вызов экшенов и их порядок, а также, что не был вызван таймер на повторный запрос
                const expectedActions = [startOrderTrackingFetch(), clearOrderTrackingData()];

                expect(store.getActions()).toEqual(expectedActions);
                expect(setTimeout).not.toHaveBeenCalled();
            });

            it('should handle API error and save it', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.userAuthorized,
                    session: mocks.session.userAuthorized,
                    orderTracking: prepareSlice({
                        orderId: mocks.orderId,
                        ui: {},
                    }),
                });
                // Мокаем ошибку АПИ
                const mockError = { data: mocks.orderTrackingError };

                // Меняем реализацию АПИ, чтобы ответ был с телом ошибки
                (trackOrder as jest.Mock).mockImplementation(() => Promise.reject(mockError));

                await store.dispatch(fetchOrderTracking());

                // Проверяем вызов экшенов и их порядок, а также, что ошибка была залогирована
                const expectedActions = [startOrderTrackingFetch(), failedOrderTrackingFetch(mocks.orderTrackingError)];

                expect(store.getActions()).toEqual(expectedActions);
                expect(logError).toHaveBeenCalled();
                expect(logError).toHaveBeenCalledWith(
                    {
                        type: 'network',
                        method: 'fetchOrderTracking',
                        message: mockError.data.message,
                        additional: mockError,
                    },
                    mockError
                );

                // Очищаем уже протестированные экшены
                store.clearActions();
                // Запускаем ожидающие таймеры, чтобы выполнить следующий запрос. Убираем mock с setTimeout
                jest.runOnlyPendingTimers();
                jest.useRealTimers();

                // Проверяем, что экшены выполнились по второму циклу. Делаем это в другой макротаске,
                // чтобы сравнить после выполнения thunk'а
                await new Promise(resolve => setTimeout(resolve));
                expect(store.getActions()).toEqual(expectedActions);
            });

            it('should handle AbortError error and save it', async() => {
                // Мокаем необходимые ветки store для тестирования
                const store = mockStore({
                    auth: mocks.auth.userAuthorized,
                    session: mocks.session.userAuthorized,
                    orderTracking: prepareSlice({
                        orderId: mocks.orderId,
                        ui: {},
                    }),
                });

                // Меняем реализацию АПИ, чтобы ответ был симуляцией AbortError
                (trackOrder as jest.Mock).mockImplementation(() =>
                    Promise.reject({
                        name: 'AbortError',
                        message: 'The operation was aborted.',
                    })
                );

                await store.dispatch(fetchOrderTracking());

                // Проверяем вызов экшенов, а также, что логирования в метрику и ошибки не было
                const expectedActions = [startOrderTrackingFetch()];

                expect(store.getActions()).toEqual(expectedActions);
                expect(logError).not.toHaveBeenCalled();
                expect(reachGoal).not.toHaveBeenCalled();
                expect(setTimeout).not.toHaveBeenCalled();
            });
        });
    });
});
