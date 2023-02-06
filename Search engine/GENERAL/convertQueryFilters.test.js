import {parseQueryFilters, serializeQueryFilters} from './convertQueryFilters';

describe('Parse graph line filter', () => {
    test('single country', () => {
        const base = {
            name: 'default',
            color: null,
            componentFilter: 'onlySearchResult',
            postFilters: [],

            aspectFilter: null,
            hostFilter: ['1st41'],
            metricFilter: null,
        };
        const filterModel = {
            ...base,

            queryFilters: ['country-AB'],
        };
        const parsedFilter = {
            ...base,

            countryExpression: [],
            dbFilters: [],
            queryFilters: [],
            countryFilters: ['AB'],
            regionFilters: [],
            labelsFilters: [],
        };
        expect(parseQueryFilters(filterModel)).toEqual(parsedFilter);
    });

    test('multiple countries', () => {
        const base = {
            name: 'default',
            color: null,
            postFilters: ['diff', 'diff-metric', 'nodiff-metric'],
            componentFilter: 'skipRightAlign',

            aspectFilter: ['actuality'],
            hostFilter: [],
            metricFilter: ['4CG-query-click-visitors'],
        };
        const filterModel = {
            ...base,

            queryFilters: [
                'db-1',
                'db-6',
                'query-100945',
                'query-100934',
                'country-AB-AT-AL',
                'region-122844',
            ],
        };
        const parsedFilter = {
            ...base,

            countryExpression: [],
            dbFilters: [1, 6],
            queryFilters: [100945, 100934],
            countryFilters: ['AB', 'AT', 'AL'],
            regionFilters: [122844],
            labelsFilters: [],
        };
        expect(parseQueryFilters(filterModel)).toEqual(parsedFilter);
    });

    test('country expression', () => {
        const base = {
            name: 'default',
            color: null,
            componentFilter: 'skipRightAlign',

            postFilters: ['diff', 'diff-metric', 'nodiff-metric'],
            aspectFilter: ['actuality'],
            hostFilter: [],
            metricFilter: ['4CG-query-click-visitors'],
        };
        const filterModel = {
            ...base,

            queryFilters: ['country-AB-AT-AL', 'country-RU'],
        };
        const parsedFilter = {
            ...base,

            countryExpression: ['AB-AT-AL', 'RU'],
            dbFilters: [],
            queryFilters: [],
            countryFilters: [],
            regionFilters: [],
            labelsFilters: [],
        };
        expect(parseQueryFilters(filterModel)).toEqual(parsedFilter);
    });

    test('labels', () => {
        const base = {
            name: 'default',
            color: null,
            componentFilter: 'onlySearchResult',
            postFilters: [],

            aspectFilter: null,
            hostFilter: ['1st41'],
            metricFilter: null,
        };
        const filterModel = {
            ...base,

            queryFilters: ['labels-music OR porno'],
        };
        const parsedFilter = {
            ...base,

            countryExpression: [],
            dbFilters: [],
            queryFilters: [],
            countryFilters: [],
            regionFilters: [],
            labelsFilters: ['music OR porno'],
        };
        expect(parseQueryFilters(filterModel)).toEqual(parsedFilter);
    });
});

describe('Serialize graph line filter', () => {
    test('single country', () => {
        const base = {
            name: 'default',
            color: null,
            postFilters: [],

            aspectFilter: null,
            hostFilter: ['1st41'],
            metricFilter: null,
            componentFilter: 'onlySearchResult',
        };
        const filterModel = {
            ...base,

            countryExpression: [],
            dbFilters: [],
            queryFilters: [],
            countryFilters: ['AB'],
            regionFilters: [],
            labelsFilters: [],
        };
        const serializedFilter = {
            ...base,

            queryFilters: ['country-AB'],
        };
        expect(serializeQueryFilters(filterModel)).toEqual(serializedFilter);
    });

    test('multiple countries', () => {
        const base = {
            name: 'default',
            color: null,
            postFilters: ['diff', 'diff-metric', 'nodiff-metric'],

            aspectFilter: ['actuality'],
            componentFilter: 'skipRightAlign',
            hostFilter: [],
            metricFilter: ['4CG-query-click-visitors'],
        };
        const filterModel = {
            ...base,

            countryExpression: [],
            dbFilters: [1, 6],
            queryFilters: [100945, 100934],
            countryFilters: ['AB', 'AT', 'AL'],
            regionFilters: [122844],
            labelsFilters: [],
        };
        const serializedFilter = {
            ...base,

            queryFilters: [
                'db-1',
                'db-6',
                'query-100945',
                'query-100934',
                'country-AB-AT-AL',
                'region-122844',
            ],
        };
        expect(serializeQueryFilters(filterModel)).toEqual(serializedFilter);
    });

    test('country expression', () => {
        const base = {
            name: 'default',
            color: null,
            postFilters: ['diff', 'diff-metric', 'nodiff-metric'],

            aspectFilter: ['actuality'],
            componentFilter: 'skipRightAlign',
            hostFilter: [],
            metricFilter: ['4CG-query-click-visitors'],
        };
        const filterModel = {
            ...base,

            countryExpression: ['AB-AT-AL', 'RU'],
            dbFilters: [],
            queryFilters: [],
            countryFilters: [],
            regionFilters: [],
            labelsFilters: [],
        };
        const serializedFilter = {
            ...base,

            queryFilters: ['country-AB-AT-AL', 'country-RU'],
        };
        expect(serializeQueryFilters(filterModel)).toEqual(serializedFilter);
    });

    test('labels', () => {
        const base = {
            name: 'default',
            color: null,
            postFilters: [],

            aspectFilter: null,
            hostFilter: ['1st41'],
            metricFilter: null,
            componentFilter: 'onlySearchResult',
        };
        const filterModel = {
            ...base,

            countryExpression: [],
            dbFilters: [],
            queryFilters: [],
            countryFilters: [],
            regionFilters: [],
            labelsFilters: ['(NOT porno) AND (NOT music)'],
        };
        const serializedFilter = {
            ...base,

            queryFilters: ['labels-(NOT porno) AND (NOT music)'],
        };
        expect(serializeQueryFilters(filterModel)).toEqual(serializedFilter);
    });
});
