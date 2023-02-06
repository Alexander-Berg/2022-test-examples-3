import { PersistedState, Persistor } from 'redux-persist/es/types';

import { RootReducer } from '../redux';
import { RestoreStatus } from '../redux/slices/general/types';

export const preparePersist = <T>(slice: T): Readonly<T & PersistedState> => {
    return {
        ...slice,
        _persist: {
            version: 1,
            rehydrated: true,
        },
    };
};

export const getPersistor = (): Persistor => {
    return {
        getState() { return { registry: [], bootstrapped: false } },
        dispatch(action) { return action },
        subscribe() { return () => {} },
        pause() {},
        persist() {},
        async purge() {},
        async flush() {},
    };
};

export const generateInitState = (): RootReducer => ({
    address: preparePersist({
        persist: {
            lastData: {},
        },
        nonpersist: {
            orders: {},
        },
    }),
    addressSuggest: { orders: {} },
    auth: preparePersist({
        persist: {},
        nonpersist: {
            checkStartAuth: false,
            isLoading: false
        },
    }),
    city: preparePersist({
        persist: {
            lastData: {},
        },
        nonpersist: {
            orders: {},
        },
    }),
    citySuggest: { orders: {} },
    commonAddresses: {
        data: {
            items: [],
        },
        ui: {
            isLoading: false,
        },
    },
    contacts: preparePersist({ data: {} }),
    delivery: preparePersist({
        persist: {},
        nonpersist: {
            orders: {},
        },
    }),
    pickup: preparePersist({
        persist: {},
        nonpersist: {
            orders: {},
        },
    }),
    extra: preparePersist({ data: {}, ui: {} }),
    general: preparePersist({
        persist: {
            isFirstEntry: true,
        },
        nonpersist: {
            isLoading: false,
            restoreStatus: RestoreStatus.None,
        },
    }),
    geolocation: preparePersist({
        data: {},
        ui: {
            isLoading: true,
        },
    }),
    latestOrder: preparePersist({ data: {} }),
    map: { ui: {} },
    merchant: {
        data: {
            id: '',
            title: '',
        },
        ui: {
            isMerchantResolved: false,
        },
    },
    orderSummary: { orders: [] },
    payment: {
        ui: {
            isLoading: false,
            isSuccess: false,
        },
    },
    paymentMethods: preparePersist({
        persist: {},
        nonpersist: {
            isLoading: false,
        },
    }),
    products: { orders: {} },
    productsSummary: { orders: {} },
    profileAddresses: preparePersist({
        persist: {
            internalItems: [],
        },
        nonpersist: {
            items: [],
            selected: {},
            isLoading: false,
            firstRequestCompleted: false,
        },
    }),
    validationErrors: {
        internal: {
            orders: {},
        },
        external: {
            orders: {},
        },
    },
});
