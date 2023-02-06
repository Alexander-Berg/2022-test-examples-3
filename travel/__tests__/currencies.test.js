const getNationalCurrency = jest.fn(() => 'UAH');

jest.setMock('../../lib/currencies/getNationalCurrency', getNationalCurrency);

jest.dontMock('../../actions/nationalVersion');
jest.dontMock('../../actions/currencies');
jest.dontMock('../../actions/currencies/basicActions');

const {setNationalVersion} = require.requireActual(
    '../../actions/nationalVersion',
);

const {setPreferredCurrency, setCurrencyRates} = require.requireActual(
    '../../actions/currencies',
);

const currencies = require.requireActual('../currencies').default;

describe('currencies reducer', () => {
    describe('handling SET_NATIONAL_VERSION action', () => {
        it('should set appropriate `nationalCurrency` and `preferredCurrency`', () => {
            const initialState = {
                nationalCurrency: 'RUR',
                preferredCurrency: 'RUR',
                currencyRates: {},
            };

            const action = setNationalVersion('ua');

            const actualResultState = currencies(initialState, action);

            const expectedResultState = {
                nationalCurrency: 'UAH',
                preferredCurrency: 'UAH',
                currencyRates: {},
            };

            expect(getNationalCurrency).toBeCalledWith('ua');
            expect(actualResultState).not.toBe(initialState);
            expect(actualResultState).toEqual(expectedResultState);
        });
    });

    describe('handling SET_PREFERRED_CURRENCY action', () => {
        it('should set `preferredCurrency` field', () => {
            const initialState = {
                nationalCurrency: 'RUR',
                preferredCurrency: 'RUR',
                currencyRates: {},
            };

            const action = setPreferredCurrency('UAH');

            const actualResultState = currencies(initialState, action);

            const expectedResultState = {
                nationalCurrency: 'RUR',
                preferredCurrency: 'UAH',
                currencyRates: {},
            };

            expect(actualResultState).not.toBe(initialState);
            expect(actualResultState).toEqual(expectedResultState);
        });
    });

    describe('handling SET_CURRENCY_RATES action', () => {
        it('should store given currency currencyRates in a `currencyRates` field', () => {
            const initialState = {
                nationalCurrency: 'RUR',
                preferredCurrency: 'RUR',
                currencyRates: {
                    RUR: {
                        UAH: 10,
                    },
                },
            };

            const currencyRatesData = {
                UAH: {
                    RUR: 0.1,
                },
            };

            const action = setCurrencyRates(currencyRatesData);

            const actualResultState = currencies(initialState, action);

            const expectedResultState = {
                nationalCurrency: 'RUR',
                preferredCurrency: 'RUR',
                currencyRates: {
                    RUR: {
                        UAH: 10,
                    },
                    UAH: {
                        RUR: 0.1,
                    },
                },
            };

            expect(actualResultState).toEqual(expectedResultState);
            expect(actualResultState).not.toBe(initialState);
        });
    });
});
