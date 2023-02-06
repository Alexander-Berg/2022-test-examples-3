const SET_CURRENCY_RATES_ACTION = {type: 'SET_CURRENCY_RATES'};
const setCurrencyRates = jest.fn(() => SET_CURRENCY_RATES_ACTION);

const SET_AVAILABLE_CURRENCIES_ACTION = {type: 'SET_AVAILABLE_CURRENCIES'};
const setAvailableCurrencies = jest.fn(() => SET_AVAILABLE_CURRENCIES_ACTION);

jest.setMock('../basicActions', {
    setCurrencyRates,
    setAvailableCurrencies,
});

const requestCurrencyRates = require.requireActual(
    '../requestCurrencyRates',
).default;

const getState = jest.fn(() => ({
    language: 'ru',
    nationalVersion: 'ua',
    currencies: {
        nationalCurrency: 'RUB',
        preferredCurrency: 'UAH',
        avalialbeCurrencies: [],
        currencyRates: {},
    },
}));

const dispatch = jest.fn();

const apiMethodResult = {
    availableCurrencies: [],
    currencyRates: {},
};

const resolvingApi = {
    exec: jest.fn(() => Promise.resolve(apiMethodResult)),
};

const API_ERROR = new Error();

const rejectingApi = {
    exec: jest.fn(() => Promise.reject(API_ERROR)),
};

const request = {requestId: '42'};

describe('requestCurrencyRates', () => {
    afterEach(() => {
        getState.mockClear();
        dispatch.mockClear();

        resolvingApi.exec.mockClear();
        rejectingApi.exec.mockClear();

        setCurrencyRates.mockClear();
        setAvailableCurrencies.mockClear();
    });

    it('should return a function', () => {
        const thunk = requestCurrencyRates('RUB');

        expect(typeof thunk).toBe('function');
    });

    describe('returned function', () => {
        it('should call `currencies` api method', () => {
            const thunk = requestCurrencyRates('RUB', request);
            const api = resolvingApi;

            thunk({getState, dispatch, api});

            expect(api.exec).toBeCalledWith(
                'currencies',
                {
                    language: 'ru',
                    nationalVersion: 'ua',
                    baseCurrency: 'RUB',
                },
                request,
            );
        });

        it('should dispatch `SET_CURRENCY_RATES` action', () => {
            const thunk = requestCurrencyRates('RUB');
            const api = resolvingApi;

            return thunk({getState, dispatch, api}).then(() => {
                expect(setCurrencyRates).toBeCalledWith(
                    apiMethodResult.currencyRates,
                );
                expect(dispatch).toBeCalledWith(SET_CURRENCY_RATES_ACTION);
            });
        });

        it('should dispatch `SET_AVAILABLE_CURRENCIES` action if `baseCurrency` parameter equals to `nationalCurrency`', () => {
            const thunk = requestCurrencyRates('RUB');
            const api = resolvingApi;

            return thunk({getState, dispatch, api}).then(() => {
                expect(setAvailableCurrencies).toBeCalledWith(
                    apiMethodResult.availableCurrencies,
                );
                expect(dispatch).toBeCalledWith(
                    SET_AVAILABLE_CURRENCIES_ACTION,
                );
            });
        });

        it('should not dispatch `SET_AVAILABLE_CURRENCIES` action if `baseCurrency` parameter differs from `nationalCurrency`', () => {
            const thunk = requestCurrencyRates('UAH');
            const api = resolvingApi;

            return thunk({getState, dispatch, api}).then(() => {
                expect(setAvailableCurrencies).not.toBeCalled();
            });
        });

        it('should reject with an api error if api request fails', () => {
            const thunk = requestCurrencyRates('RUB');
            const api = rejectingApi;

            return thunk({getState, dispatch, api}).catch(error => {
                expect(error).toBe(API_ERROR);
            });
        });
    });
});
