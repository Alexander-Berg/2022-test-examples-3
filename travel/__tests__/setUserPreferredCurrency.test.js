const SET_PREFERRED_CURRENCY_ACTION = {type: 'SET_PREFERRED_CURRENCY'};
const setPreferredCurrency = jest.fn(() => SET_PREFERRED_CURRENCY_ACTION);

jest.setMock('../basicActions', {setPreferredCurrency});

const REQUEST_CURRENCY_RATES_RESULT = Promise.resolve();
const requestCurrencyRates = jest.fn(() => REQUEST_CURRENCY_RATES_RESULT);

jest.setMock('../requestCurrencyRates', requestCurrencyRates);

const dispatch = jest.fn(action => action);

const cookies = {
    set: jest.fn(),
};

const currency = 'RUB';

const setUserPreferredCurrency = require.requireActual(
    '../setUserPreferredCurrency',
).default;

describe('setUserPreferredCurrency', () => {
    it('should dispatch `SET_PREFERRED_CURRENCY` action', () => {
        const thunk = setUserPreferredCurrency(currency);

        thunk({dispatch, cookies});

        expect(setPreferredCurrency).toBeCalledWith(currency);
        expect(dispatch).toBeCalledWith(SET_PREFERRED_CURRENCY_ACTION);
    });

    it('should set `preferredcurrency` cookie', () => {
        const thunk = setUserPreferredCurrency(currency);

        thunk({dispatch, cookies});

        expect(cookies.set).toBeCalledWith('preferredcurrency', currency);
    });

    it('should dispatch `requestCurrencyRates` action and return the result', () => {
        const thunk = setUserPreferredCurrency(currency);

        return thunk({dispatch, cookies}).then(() => {
            expect(requestCurrencyRates).toBeCalledWith(currency);
            expect(dispatch).toBeCalledWith(REQUEST_CURRENCY_RATES_RESULT);
        });
    });
});
