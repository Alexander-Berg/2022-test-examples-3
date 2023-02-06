import { getCurrencyFromStr } from '../../../src/tools';

describe('Get currency value from text:', () => {
    const testData = [
        {
            actual: {},
            expected: undefined,
        },
        {
            actual: ['an array'],
            expected: undefined,
        },
        {
            actual: 'a string without currency',
            expected: undefined,
        },
        {
            actual: 1234567890,
            expected: undefined,
        },
        {
            actual: () => undefined,
            expected: undefined,
        },
        {
            actual: null,
            expected: undefined,
        },
        {
            actual: undefined,
            expected: undefined,
        },
        {
            actual: Infinity,
            expected: undefined,
        },
        {
            actual: new Set(),
            expected: undefined,
        },
        {
            actual: NaN,
            expected: undefined,
        },
        {
            actual: 'EUR',
            expected: 'EUR',
        },
        {
            actual: 'eUr',
            expected: 'EUR',
        },
        {
            actual: 'eur',
            expected: 'EUR',
        },
        {
            actual: 'euro',
            expected: 'EUR',
        },
        {
            actual: '€',
            expected: 'EUR',
        },
        {
            actual: 'UAH',
            expected: 'UAH',
        },
        {
            actual: 'Uah',
            expected: 'UAH',
        },
        {
            actual: 'uah',
            expected: 'UAH',
        },
        {
            actual: 'ГРН',
            expected: 'UAH',
        },
        {
            actual: 'Грн',
            expected: 'UAH',
        },
        {
            actual: 'грн',
            expected: 'UAH',
        },
        {
            actual: '₴',
            expected: 'UAH',
        },
        {
            actual: 'TENGE',
            expected: 'KZT',
        },
        {
            actual: 'Tenge',
            expected: 'KZT',
        },
        {
            actual: 'tenge',
            expected: 'KZT',
        },
        {
            actual: 'ТЕНГЕ',
            expected: 'KZT',
        },
        {
            actual: 'тенге',
            expected: 'KZT',
        },
        {
            actual: 'Тенге',
            expected: 'KZT',
        },
        {
            actual: 'ТГ',
            expected: 'KZT',
        },
        {
            actual: 'Тг',
            expected: 'KZT',
        },
        {
            actual: 'тг',
            expected: 'KZT',
        },
        {
            actual: 'ТҢГ',
            expected: 'KZT',
        },
        {
            actual: 'тҢг',
            expected: 'KZT',
        },
        {
            actual: 'тңг',
            expected: 'KZT',
        },
        {
            actual: 'KZT',
            expected: 'KZT',
        },
        {
            actual: 'Kzt',
            expected: 'KZT',
        },
        {
            actual: 'kzt',
            expected: 'KZT',
        },
        {
            actual: '₸',
            expected: 'KZT',
        },
        {
            actual: 'GBP',
            expected: 'GBP',
        },
        {
            actual: 'gBp',
            expected: 'GBP',
        },
        {
            actual: 'gbp',
            expected: 'GBP',
        },
        {
            actual: 'UKL',
            expected: 'GBP',
        },
        {
            actual: 'uKl',
            expected: 'GBP',
        },
        {
            actual: 'ukl',
            expected: 'GBP',
        },
        {
            actual: '£',
            expected: 'GBP',
        },
        {
            actual: 'RUB',
            expected: 'RUB',
        },
        {
            actual: 'rub',
            expected: 'RUB',
        },
        {
            actual: 'Rub',
            expected: 'RUB',
        },
        {
            actual: 'RUR',
            expected: 'RUB',
        },
        {
            actual: 'Р.',
            expected: 'RUB',
        },
        {
            actual: 'P.',
            expected: 'RUB',
        },
        {
            actual: 'Р',
            expected: 'RUB',
        },
        {
            actual: 'P',
            expected: 'RUB',
        },
        {
            actual: '₽',
            expected: 'RUB',
        },
        {
            actual: 'РUB.',
            expected: 'RUB',
        },
        {
            actual: 'PУB.',
            expected: 'RUB',
        },
        {
            actual: 'PYБ.',
            expected: 'RUB',
        },
        {
            actual: 'РYБ.',
            expected: 'RUB',
        },
        {
            actual: 'РУB.',
            expected: 'RUB',
        },
        {
            actual: 'PУБ.',
            expected: 'RUB',
        },
        {
            actual: 'USD',
            expected: 'USD',
        },
        {
            actual: 'Usd',
            expected: 'USD',
        },
        {
            actual: 'usd',
            expected: 'USD',
        },
        {
            actual: 'У.Е.',
            expected: 'USD',
        },
        {
            actual: 'У.е.',
            expected: 'USD',
        },
        {
            actual: 'у.е.',
            expected: 'USD',
        },
        {
            actual: 'Usd',
            expected: 'USD',
        },
        {
            actual: 'usd',
            expected: 'USD',
        },
        {
            actual: '$',
            expected: 'USD',
        },
    ];

    testData.forEach(({ actual, expected }) => {
        it(`actual value '${actual}' of type '${typeof actual}' => expected value '${expected}'`, () => {
            expect(getCurrencyFromStr(actual)).toEqual(expected);
        });
    });
});
