const dispatch = jest.fn(action => action);
const requestCurrencyRates = jest.fn(currency => Promise.resolve(currency));

jest.setMock('../requestCurrencyRates', requestCurrencyRates);

const requestInitialCurrencyRates = require.requireActual(
    '../requestInitialCurrencyRates',
).default;

const getStateWithSameCurrencies = jest.fn(() => ({
    currencies: {
        nationalCurrency: 'RUB',
        preferredCurrency: 'RUB',
    },
}));

const getStateWithDifferentCurrencies = jest.fn(() => ({
    currencies: {
        nationalCurrency: 'RUB',
        preferredCurrency: 'UAH',
    },
}));

const request = {requestId: '42'};

describe('requestInitialCurrencyRates', () => {
    afterEach(() => {
        dispatch.mockClear();
        requestCurrencyRates.mockClear();
        getStateWithSameCurrencies.mockClear();
        getStateWithDifferentCurrencies.mockClear();
    });

    it('should return a function', () => {
        expect(typeof requestInitialCurrencyRates()).toBe('function');
    });

    describe('returned function', () => {
        it('should request national and prefferred currency rates if they differ', () => {
            const thunk = requestInitialCurrencyRates(request);

            thunk({dispatch, getState: getStateWithDifferentCurrencies});
            expect(dispatch.mock.calls.length).toBe(2);
            expect(requestCurrencyRates).toBeCalledWith('RUB', request);
            expect(requestCurrencyRates).toBeCalledWith('UAH', request);
        });

        it('should make only one request if national and preferred currencies are the same', () => {
            const thunk = requestInitialCurrencyRates(request);

            thunk({dispatch, getState: getStateWithSameCurrencies});
            expect(dispatch.mock.calls.length).toBe(1);
            expect(requestCurrencyRates).toBeCalledWith('RUB', request);
        });
    });
});
