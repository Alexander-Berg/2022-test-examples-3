import thunk from 'redux-thunk';
import createMockStore from 'redux-mock-store';
import { YandexCheckoutState } from '@yandex-int/tap-checkout-types';

import { Currency } from '../../../lib/api/types';
import { preparePersist, generateInitState, getPersistor } from '../../../lib/tests';
import * as di from '../../../lib/di';
import { restoreAddress } from '../../shared-actions';
import { RestoreStatus } from '../../slices/general/types';
import { manualSelectDatetimeOption, manualSelectShippingOption } from '../../slices/delivery';
import { triggerEvent } from '../triggerEvent';
import { AppDispatch, RootReducer, AppStore } from '../..';
import { restoreCheckoutState } from '.';

const mockStore = createMockStore<RootReducer, AppDispatch>([thunk]);

jest.mock('../../../lib/rum', () => ({ logError: jest.fn(), logWarning: jest.fn() }));

function init(store: AppStore) {
    di.setStore(store);
    di.setStorePersistor(getPersistor());
}

describe('redux. shared-thunks. restoreCheckoutState', () => {
    it('не должен отсылать событие restoreState для первого заказа', async() => {
        const store = mockStore(generateInitState());

        const thunkArgs = { merchantId: 'merchant-id' };

        init(store);

        await store.dispatch(restoreCheckoutState(thunkArgs));

        const expectedActions = [
            restoreCheckoutState.pending(expect.any(String), thunkArgs),
            restoreCheckoutState.fulfilled(null, expect.any(String), thunkArgs),
        ];

        expect(store.getActions()).toEqual(expectedActions);
    });

    it('должен выбрать первые значения из списков методов и дат доставки в качестве дефолтных', async() => {
        const state = generateInitState();

        state.delivery = preparePersist({
            persist: {},
            nonpersist: {
                orders: {
                    'order-id': {
                        shippingOptions: [
                            { id: 'courier', label: 'Курьер', amount: { value: 0, currency: Currency.Rub } },
                            { id: 'post', label: 'Почта', amount: { value: 0, currency: Currency.Rub } },
                        ],
                        datetimeOptions: [
                            {
                                id: '2000-01-01',
                                date: '2000-01-01',
                                timeOptions: [
                                    { id: '9.00', label: '9.00', amount: { value: 0, currency: Currency.Rub } },
                                    { id: '10.00', label: '10.00', amount: { value: 0, currency: Currency.Rub } },
                                ],
                            },
                            { id: '2000-02-02', date: '2000-02-02', timeOptions: [] },
                        ]
                    },
                },
            },
        });
        state.orderSummary = { orders: ['order-id'] };
        state.general = preparePersist({
            persist: {
                isFirstEntry: true,
            },
            nonpersist: {
                isLoading: false,
                restoreStatus: RestoreStatus.None,
            }
        });
        const store = mockStore(state);

        init(store);

        const thunkArgs = { merchantId: 'merchant-id' };

        await store.dispatch(restoreCheckoutState(thunkArgs));

        const expectedActions = [
            restoreCheckoutState.pending(expect.any(String), thunkArgs),

            manualSelectShippingOption({ orderId: 'order-id', methodId: 'courier' }),
            triggerEvent.pending(expect.any(String), { name: 'shippingOptionChange' }),
            triggerEvent.fulfilled(undefined, expect.any(String), { name: 'shippingOptionChange' }),

            manualSelectDatetimeOption({
                orderId: 'order-id',
                selectedDatetime: {
                    id: '2000-01-01',
                    timeOption: { id: '9.00' },
                },
            }),
            triggerEvent.pending(expect.any(String), { name: 'datetimeOptionChange' }),
            triggerEvent.fulfilled(undefined, expect.any(String), { name: 'datetimeOptionChange' }),

            restoreCheckoutState.fulfilled(null, expect.any(String), thunkArgs),
        ];

        expect(store.getActions()).toEqual(expectedActions);
    });

    it('должен выбрать значения из списков методов и дат доставки для последнего заказа', async() => {
        const state = generateInitState();

        state.delivery = preparePersist({
            persist: {},
            nonpersist: {
                orders: {
                    'order-id': {
                        shippingOptions: [
                            { id: 'courier', label: 'Курьер', amount: { value: 0, currency: Currency.Rub } },
                            { id: 'post', label: 'Почта', amount: { value: 0, currency: Currency.Rub } },
                        ],
                        datetimeOptions: [
                            { id: '2000-01-01', date: '2000-01-01', timeOptions: [] },
                            { id: '2000-02-02', date: '2000-02-02', timeOptions: [] },
                        ],
                    },
                },
            },
        });
        state.latestOrder = preparePersist({
            data: {
                'merchant-id': {
                    wasContactsSent: false,
                    orders: {
                        'order-id': {
                            id: 'order-id',
                            shippingOption: { id: 'post' },
                            datetimeOption: { id: '2000-02-02' },
                        },
                    },
                },
            },
        });
        state.orderSummary = { orders: ['order-id'] };
        state.general = preparePersist({
            persist: {
                isFirstEntry: false,
            },
            nonpersist: {
                isLoading: false,
                restoreStatus: RestoreStatus.None,
            }
        });
        const store = mockStore(state);

        init(store);

        const thunkArgs = { merchantId: 'merchant-id' };

        await store.dispatch(restoreCheckoutState(thunkArgs));

        const expectedCheckoutState: YandexCheckoutState = {
            comment: undefined,
            paymentOption: undefined,
            promoCode: undefined,
            orders: [{
                id: 'order-id',
                city: undefined,
                shippingAddress: undefined,
                shippingOption: { id: 'post' },
                datetimeOption: { id: '2000-02-02' },
            }],
        };
        const expectedActions = [
            restoreCheckoutState.pending(expect.any(String), thunkArgs),
            restoreAddress(),

            triggerEvent.pending(expect.any(String), {
                name: 'restoreState',
                checkoutState: expectedCheckoutState,
            }),
            triggerEvent.fulfilled(undefined, expect.any(String), {
                name: 'restoreState',
                checkoutState: expectedCheckoutState,
            }),

            manualSelectShippingOption({ orderId: 'order-id', methodId: 'post' }),
            manualSelectDatetimeOption({ orderId: 'order-id', selectedDatetime: { id: '2000-02-02' } }),

            restoreCheckoutState.fulfilled({
                wasContactsSent: false,
                comment: undefined,
                paymentOption: undefined,
                promoCode: undefined,
                orders: {
                    'order-id': {
                        id: 'order-id',
                        city: undefined,
                        shippingAddress: undefined,
                        shippingOption: { id: 'post' },
                        datetimeOption: { id: '2000-02-02' },
                    }
                }
            }, expect.any(String), thunkArgs),
        ];

        expect(store.getActions()).toEqual(expectedActions);
    });
});
