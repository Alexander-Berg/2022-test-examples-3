import { Key } from '@yandex-turbo/types/jobs';

import { getStableFacetValue, getStableFilters, IReduxStore } from '../selectors';

describe('jobs selectors', () => {
    describe('getStableFacetValue', () => {
        it('should stable sort same filters', () => {
            const rawFilters1: Key[] = ['back', 'assessor', 'front'];
            const rawFilters2: Key[] = ['back', 'front', 'assessor'];

            const expected = 'assessor,back,front';
            expect(getStableFacetValue(rawFilters1)).toBe(expected);
            expect(getStableFacetValue(rawFilters2)).toBe(expected);
        });

        it('should sort number filters as strings', () => {
            const rawFilters: Key[] = [2, 1, 32, 11];

            const expected = '1,11,2,32';
            expect(getStableFacetValue(rawFilters)).toBe(expected);
        });
    });

    describe('getStableFilters', () => {
        it('should convert facet values to strings', () => {
            const state = {
                jobs: {
                    filters: {
                        value: {
                            cities: ['moscow'],
                            pro_levels: ['intern', 'middle'],
                            employment_types: ['remote', 'intern', 'office'],
                            public_professions: ['front', 'back'],
                            skills: [28, 31],
                            services: ['cloud'],
                        },
                        valueFixed: {
                            services: ['hr-tech'],
                        },
                    }
                },
            } as IReduxStore;

            const result = getStableFilters(state);

            const expected = {
                cities: 'moscow',
                pro_levels: 'intern,middle',
                employment_types: 'intern,office,remote',
                professions: 'back,front',
                skills: '28,31',
                services: 'cloud,hr-tech',
            };

            expect(result).toStrictEqual(expected);
        });

        it('should use "-" as value for facets without accepted filters', () => {
            const state = {
                jobs: {
                    filters: {
                        value: {
                            cities: [],
                            public_professions: [],
                            skills: [],
                        },
                        valueFixed: {},

                    }
                },
            } as IReduxStore;

            const result = getStableFilters(state);

            const expected = {
                cities: '-',
                pro_levels: '-',
                employment_types: '-',
                professions: '-',
                skills: '-',
                services: '-',
            };

            expect(result).toStrictEqual(expected);
        });
    });
});
