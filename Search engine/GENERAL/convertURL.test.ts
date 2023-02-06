import {convertUrlParams, convertQJudgementParams} from './convertURL';

describe('convertUrlParams', () => {
    test('QJ to Compare', () => {
        const base = {
            regional: 'WORLD',
            evaluation: 'WEB',
            aspect: 'default',

            absolute: true,
        };
        const target = {
            ...base,

            serpset: [22053429, 22053430],
            'serpset-filter': ['onlySearchResult', 'wizardOnly'],

            'pre-filter': ['country-RU', 'query-131368'],
            'post-filter': 'diff-10',
        };
        const res = convertUrlParams({
            convertTo: 'compare',
            params: {
                ...base,

                leftSerpset: 22053429,
                rightSerpset: 22053430,
                leftComponentFilter: 'onlySearchResult',
                rightComponentFilter: 'wizardOnly',

                preFilter: ['country-RU', 'query-131368'],
                postFilter: 'diff-10',
            },
        });

        expect(res).toEqual(target);
    });
    test('QJ to Queries', () => {
        const base = {
            regional: 'WORLD',
            evaluation: 'WEB',
            aspect: 'default',

            metric: 'goal-wmean-5',

            absolute: true,
            offset: 3,
        };
        const target = {
            ...base,

            serpset: [22053429, 22053430],
            'serpset-filter': ['onlySearchResult', 'wizardOnly'],

            'pre-filter': ['country-RU', 'query-131368'],
            'post-filter': 'diff-10',

            'sort-field': 'diff',
            'sort-direction': 'asc',
            'page-size': 100,
        };
        const res = convertUrlParams({
            convertTo: 'queries',
            params: {
                ...base,

                leftSerpset: 22053429,
                rightSerpset: 22053430,
                leftComponentFilter: 'onlySearchResult',
                rightComponentFilter: 'wizardOnly',

                serpMetrics: ['empty-serp', 'proxima-gold-Z'],
                componentMetrics: ['empty-serp', 'proxima-gold-Z'],

                preFilter: ['country-RU', 'query-131368'],
                postFilter: 'diff-10',

                sortField: 'diff',
                sortDirection: 'asc',
                pageSize: 100,
            },
        });

        expect(res).toEqual(target);
    });
    test('Old QJ to QJ', () => {
        const base = {
            regional: 'WORLD',
            evaluation: 'WEB',
            aspect: 'default',

            metric: 'goal-wmean-5',
            absolute: true,

            query: 'test',
            device: 'DESKTOP',
            country: 'RU',
        };
        const target = {
            ...base,

            leftSerpset: 22053429,
            rightSerpset: 22053430,
            leftComponentFilter: 'onlySearchResult',
            rightComponentFilter: 'wizardOnly',

            preFilter: ['country-RU', 'query-131368'],
            postFilter: 'diff-10',

            sortField: 'diff',
            sortDirection: 'asc',
            pageSize: 100,

            regionId: 2,
            mapInfo: '',
            viewMode: 'expanded',
        };
        const res = convertUrlParams({
            convertTo: 'qjudgement',
            params: {
                ...base,

                serpset: [22053429, 22053430],
                'serpset-filter': ['onlySearchResult', 'wizardOnly'],

                'pre-filter': ['country-RU', 'query-131368'],
                'post-filter': 'diff-10',

                'sort-field': 'diff',
                'sort-direction': 'asc',
                'page-size': 100,
                offset: 0,

                'region-id': 2,
                'map-info': '',
                'left-serp-set': '22053429',
                'left-serp-set-filter': 'onlySearchResult',
                'right-serp-set': '22053430',
                'right-serp-set-filter': 'wizardOnly',
                position: 1,
                show: 'Snippets',
                view: 'default',
            },
        });

        expect(res).toEqual(target);
    });
    test('QJ to old QJ', () => {
        const base = {
            regional: 'WORLD',
            evaluation: 'WEB',
            aspect: 'default',

            metric: 'goal-wmean-5',
            absolute: true,

            query: 'test',
            device: 'DESKTOP',
            country: 'RU',
        };
        const target = {
            ...base,

            serpset: [22053429, 22053430],
            'serpset-filter': ['onlySearchResult', 'wizardOnly'],

            'pre-filter': ['country-RU', 'query-131368'],
            'post-filter': 'diff-10',

            'sort-field': 'diff',
            'sort-direction': 'asc',
            'page-size': 100,

            'region-id': 2,
            'map-info': '',

            show: 'URLs',
        };
        const res = convertUrlParams({
            convertTo: 'oldQJ',
            params: {
                ...base,

                leftSerpset: 22053429,
                rightSerpset: 22053430,
                leftComponentFilter: 'onlySearchResult',
                rightComponentFilter: 'wizardOnly',

                serpMetrics: ['goal-wmean-5'],
                componentMetrics: ['goal-wmean-5'],

                preFilter: ['country-RU', 'query-131368'],
                postFilter: 'diff-10',

                sortField: 'diff',
                sortDirection: 'asc',
                pageSize: 100,

                regionId: 2,
                mapInfo: '',

                viewMode: 'collapsed',
            },
        });

        expect(res).toEqual(target);
    });
});

describe('convertQJudgementParams', () => {
    test('QJ to Details', () => {
        const base = {
            regional: 'WORLD',
            evaluation: 'WEB',
            aspect: 'default',

            metric: 'goal-wmean-5',
            serpMetrics: ['goal-wmean-5', 'proxima-gold-Z'],
            componentMetrics: ['goal-wmean-5', 'proxima-gold-Z'],

            absolute: true,

            preFilter: ['country-RU', 'query-131368'],
            postFilter: 'diff-10',

            sortField: 'diff',
            sortDirection: 'asc',
            pageSize: 100,

            query: 'test',
            device: 'DESKTOP',
            country: 'RU',
            mapInfo: '',
            regionId: 2,

            side: 'left',
            index: 3,
            sequenceNumber: 25,

            serpScales: ['type', 'text.requestId'],
            componentScales: ['DUPLICATE', 'DUPLICATE_FULL'],
            siteLinkScales: ['type', 'test'],
        };
        const target = {
            ...base,

            serpset: 22053429,
            secondarySerpset: 22053430,
            componentFilter: 'onlySearchResult',
            secondaryComponentFilter: 'wizardOnly',
        };
        const res = convertQJudgementParams({
            convertTo: 'details',
            params: {
                ...base,

                leftSerpset: 22053429,
                rightSerpset: 22053430,
                leftComponentFilter: 'onlySearchResult',
                rightComponentFilter: 'wizardOnly',
            },
        });

        expect(res).toEqual(target);
    });
    test('Details to QJ', () => {
        const base = {
            regional: 'WORLD',
            evaluation: 'WEB',
            aspect: 'default',

            metric: 'goal-wmean-5',
            serpMetrics: ['goal-wmean-5', 'proxima-gold-Z'],
            componentMetrics: ['goal-wmean-5', 'proxima-gold-Z'],

            absolute: true,

            preFilter: ['country-RU', 'query-131368'],
            postFilter: 'diff-10',

            sortField: 'diff',
            sortDirection: 'asc',
            pageSize: 100,

            query: 'test',
            device: 'DESKTOP',
            country: 'RU',
            mapInfo: '',
            regionId: 2,

            serpScales: ['type'],
            componentScales: ['DUPLICATE', 'DUPLICATE_FULL'],
            siteLinkScales: ['type', 'test'],
        };
        const target = {
            ...base,

            leftSerpset: 22053429,
            rightSerpset: 22053430,
            leftComponentFilter: 'onlySearchResult',
            rightComponentFilter: 'wizardOnly',
        };
        const res = convertQJudgementParams({
            convertTo: 'qjudgement',
            params: {
                ...base,

                serpset: 22053429,
                secondarySerpset: 22053430,
                componentFilter: 'onlySearchResult',
                secondaryComponentFilter: 'wizardOnly',

                index: 3,
                sequenceNumber: 25,
                side: 'left',
            },
        });

        expect(res).toEqual(target);
    });
});

describe('composition', () => {
    test('Details to Queries', () => {
        const base = {
            regional: 'WORLD',
            evaluation: 'WEB',
            aspect: 'default',

            metric: 'goal-wmean-5',

            absolute: true,
        };
        const target = {
            ...base,
            serpset: [22053429, 22053430],
            'serpset-filter': ['onlySearchResult', 'wizardOnly'],

            'pre-filter': ['country-RU', 'query-131368'],
            'post-filter': 'diff-10',

            'sort-field': 'diff',
            'sort-direction': 'asc',
            'page-size': 100,
        };
        const qjudgementUrl = convertQJudgementParams({
            convertTo: 'qjudgement',
            params: {
                ...base,

                serpset: 22053429,
                secondarySerpset: 22053430,
                componentFilter: 'onlySearchResult',
                secondaryComponentFilter: 'wizardOnly',

                serpMetrics: ['goal-wmean-5', 'proxima-gold-Z'],
                componentMetrics: ['goal-wmean-5', 'proxima-gold-Z'],

                preFilter: ['country-RU', 'query-131368'],
                postFilter: 'diff-10',

                sortField: 'diff',
                sortDirection: 'asc',
                offset: 0,
                pageSize: 100,

                query: 'test',
                device: 'DESKTOP',
                country: 'RU',
                mapInfo: '',

                regionId: 2,

                index: 3,
                sequenceNumber: 25,

                serpScales: ['type', 'text.requestId'],
                componentScales: ['DUPLICATE', 'DUPLICATE_FULL'],
                siteLinkScales: ['type', 'test'],
            },
        });
        const qjudgementObj = {
            convertTo: 'queries',
            params: qjudgementUrl,
        };
        const res = convertUrlParams(qjudgementObj);

        expect(res).toEqual(target);
    });
    test('Details to Compare', () => {
        const base = {
            regional: 'WORLD',
            evaluation: 'WEB',
            aspect: 'default',

            absolute: true,
        };
        const target = {
            ...base,
            serpset: [22053429, 22053430],
            'serpset-filter': ['onlySearchResult', 'wizardOnly'],

            'pre-filter': ['country-RU', 'query-131368'],
            'post-filter': 'diff-10',
        };
        const qjudgementUrl = convertQJudgementParams({
            convertTo: 'qjudgement',
            params: {
                ...base,

                serpset: 22053429,
                secondarySerpset: 22053430,
                componentFilter: 'onlySearchResult',
                secondaryComponentFilter: 'wizardOnly',

                metric: 'goal-wmean-5',
                serpMetrics: ['goal-wmean-5', 'proxima-gold-Z'],
                componentMetrics: ['goal-wmean-5', 'proxima-gold-Z'],

                preFilter: ['country-RU', 'query-131368'],
                postFilter: 'diff-10',

                sortField: 'diff',
                sortDirection: 'asc',
                offset: 0,
                pageSize: 100,

                query: 'test',
                device: 'DESKTOP',
                country: 'RU',
                mapInfo: '',

                regionId: 2,

                index: 3,
                sequenceNumber: 25,

                serpScales: ['type', 'text.requestId'],
                componentScales: ['DUPLICATE', 'DUPLICATE_FULL'],
                siteLinkScales: ['type', 'test'],
            },
        });
        const qjudgementObj = {
            convertTo: 'compare',
            params: qjudgementUrl,
        };
        const res = convertUrlParams(qjudgementObj);

        expect(res).toEqual(target);
    });
});
