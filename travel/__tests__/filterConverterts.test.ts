import {
    EAviaSearchTimeFilter,
    EAviaSearchTransferFilter,
} from 'reducers/avia/search/results/filters/reducer';

import {CURRENCY_RUR} from 'utilities/currency/codes';
import {join, createFlags} from 'utilities/flags/flags';

import {baggageFilterConverter} from '../baggageFilterConverter';
import {airportsFilterConverter} from '../airportsFilterConverter';
import {companyFilterConverter} from '../companyFilterConverter';
import {partnersFilterConverter} from '../partnersFilterConverter';
import {priceFilterConverter} from '../priceFilterConverter';
import {timeFilterConverter} from '../timeFilterConverter';
import {transferFilterConverter} from '../transferFilterConverter';

describe('Avia filter value converters', () => {
    test('convert airports filter value to string', () => {
        expect(
            airportsFilterConverter.toString([
                {arrival: [1, 2], departure: [3, 4], transfers: [5, 6]},
                {arrival: [3, 4], departure: [1, 2], transfers: [6, 5]},
            ]),
        ).toBe('f3,4|1,2|5,6b1,2|3,4|6,5');
        expect(
            airportsFilterConverter.toString([
                {arrival: [], departure: [], transfers: [5, 6]},
                {arrival: [], departure: [3, 4], transfers: []},
            ]),
        ).toBe('f||5,6b3,4||');

        expect(airportsFilterConverter.toString([])).toBe('f||b||');
    });

    test('convert string to airports filter value', () => {
        expect(
            airportsFilterConverter.fromString('f3,4|1,2|5,6b1,2|3,4|6,5'),
        ).toEqual([
            {arrival: [1, 2], departure: [3, 4], transfers: [5, 6]},
            {arrival: [3, 4], departure: [1, 2], transfers: [6, 5]},
        ]);
        expect(airportsFilterConverter.fromString('f||5,6b3,4||')).toEqual([
            {arrival: [], departure: [], transfers: [5, 6]},
            {arrival: [], departure: [3, 4], transfers: []},
        ]);

        expect(airportsFilterConverter.fromString('')).toEqual([]);
    });

    test('convert baggage filter value to string', () => {
        expect(baggageFilterConverter.toString({enabled: false})).toBe('0');
        expect(baggageFilterConverter.toString({enabled: true})).toBe('1');
    });

    test('convert string to baggage filter value', () => {
        expect(baggageFilterConverter.fromString('0')).toEqual({
            enabled: false,
        });
        expect(baggageFilterConverter.fromString('1')).toEqual({enabled: true});
    });

    test('convert company filter value to string', () => {
        expect(
            companyFilterConverter.toString({
                combinationsAreEnabled: false,
                companiesIds: [0, 1, 2],
            }),
        ).toBe('0,0,1,2');
        expect(
            companyFilterConverter.toString({
                combinationsAreEnabled: true,
                companiesIds: [0, 1, 2],
            }),
        ).toBe('1,0,1,2');
        expect(
            companyFilterConverter.toString({
                combinationsAreEnabled: false,
                companiesIds: [],
            }),
        ).toBe('0');
        expect(
            companyFilterConverter.toString({
                combinationsAreEnabled: true,
                companiesIds: [],
            }),
        ).toBe('1');
    });

    test('convert string to company filter value', () => {
        expect(companyFilterConverter.fromString('0,1,2')).toEqual({
            combinationsAreEnabled: false,
            companiesIds: [1, 2],
        });
        expect(companyFilterConverter.fromString('1,1,2')).toEqual({
            combinationsAreEnabled: true,
            companiesIds: [1, 2],
        });
        expect(companyFilterConverter.fromString('0')).toEqual({
            combinationsAreEnabled: false,
            companiesIds: [],
        });
        expect(companyFilterConverter.fromString('1')).toEqual({
            combinationsAreEnabled: true,
            companiesIds: [],
        });
    });

    test('convert partners filter value to string', () => {
        expect(partnersFilterConverter.toString(['0', '1', '2'])).toBe('0,1,2');
        expect(partnersFilterConverter.toString([])).toBe('');
    });

    test('convert string to partners filter value', () => {
        expect(partnersFilterConverter.fromString('0,1')).toEqual(['0', '1']);
        expect(partnersFilterConverter.fromString('')).toEqual([]);
    });

    test('convert price filter value to string', () => {
        expect(
            priceFilterConverter.toString({
                value: {currency: CURRENCY_RUR, value: 123},
            }),
        ).toBe('123');
        expect(priceFilterConverter.toString({value: null})).toBe('');
    });

    test('convert string to price filter value', () => {
        expect(priceFilterConverter.fromString('123')).toEqual({
            value: {currency: CURRENCY_RUR, value: 123},
        });
        expect(priceFilterConverter.fromString('')).toEqual({value: null});
    });

    test('convert time filter value to string', () => {
        expect(
            timeFilterConverter.toString([
                {
                    arrival: createFlags(0),
                    departure: join<EAviaSearchTimeFilter>([
                        createFlags(EAviaSearchTimeFilter.NIGHT),
                        createFlags(EAviaSearchTimeFilter.DAY),
                    ]),
                },
                {
                    arrival: createFlags(0),
                    departure: createFlags(0),
                },
            ]),
        ).toBe('f|d|3,1b|');
        expect(timeFilterConverter.toString([])).toBe('');
    });

    test('convert string to time filter value', () => {
        expect(timeFilterConverter.fromString('f|d|3,1b|d|')).toEqual([
            {
                arrival: createFlags(0),
                departure: join<EAviaSearchTimeFilter>([
                    createFlags(EAviaSearchTimeFilter.NIGHT),
                    createFlags(EAviaSearchTimeFilter.DAY),
                ]),
            },
            {
                arrival: createFlags(0),
                departure: createFlags(0),
            },
        ]);
        expect(timeFilterConverter.fromString('')).toEqual([]);
    });

    test('convert transfer filter value to string', () => {
        expect(
            transferFilterConverter.toString({
                value: createFlags(EAviaSearchTransferFilter.ONE_CHANGE),
                range: null,
            }),
        ).toBe('0,0,1,0,0,0');

        expect(
            transferFilterConverter.toString({
                value: join([
                    createFlags(EAviaSearchTransferFilter.ONE_CHANGE),
                    createFlags(EAviaSearchTransferFilter.EXCLUDE_NIGHTLY),
                    createFlags(EAviaSearchTransferFilter.NO_AIRPORT_CHANGE),
                    createFlags(EAviaSearchTransferFilter.NO_TRANSFERS),
                ]),
                range: null,
            }),
        ).toBe('1,1,1,0,0,1');

        expect(
            transferFilterConverter.toString({
                value: createFlags(0),
                range: [3600000, 7200000],
            }),
        ).toBe('0,0,0,1,2,0');
    });

    test('convert string to transfer filter value', () => {
        expect(transferFilterConverter.fromString('1,1,1,0,0,1')).toEqual({
            value: join<EAviaSearchTransferFilter>([
                createFlags(EAviaSearchTransferFilter.ONE_CHANGE),
                createFlags(EAviaSearchTransferFilter.EXCLUDE_NIGHTLY),
                createFlags(EAviaSearchTransferFilter.NO_AIRPORT_CHANGE),
                createFlags(EAviaSearchTransferFilter.NO_TRANSFERS),
            ]),
            range: null,
        });
        expect(transferFilterConverter.fromString('0,0,0,1,2,0')).toEqual({
            value: createFlags(0),
            range: [3600000, 7200000],
        });
    });
});
