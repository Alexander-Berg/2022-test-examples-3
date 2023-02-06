import { GET_STORE_PATH } from 'tools-access-react-redux-router/src/reducer';
import { getFilterMode, selectFilters, selectOpenNodes, selectLabels, defaultFilters } from './Catalogue.selectors';
import { FILTER_MODE } from './Catalogue.constants';

describe('Catalogue.selectors', () => {
    describe('selectFilters', () => {
        it('Should select filters', () => {
            let filtersFromUrl = {
                search: 'lookup',
                member: ['trshkv', 'uruz'],
                owner: ['ssav'],
                department: [42],
                states: ['develop'],
                isSuspicious: [true],
                hasExternalMembers: [false],
                tags: [146],
            };

            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'search=lookup',
                    'member=trshkv&member=uruz',
                    'owner=ssav',
                    'department=42',
                    'states=develop',
                    'isSuspicious=true',
                    'hasExternalMembers=false',
                    'tags=146',
                ].join('&'),
            }))
                .toEqual({
                    filters: filtersFromUrl,
                    filtersFromUrl,
                    isUrlEmpty: false,
                });
        });

        it('Should add default filters if they are not in url', () => {
            let filtersFromUrl = {
                search: 'lookup',
                member: ['trshkv', 'uruz'],
                owner: ['ssav'],
                department: [42],
                hasExternalMembers: [false],
                tags: [146],
            };

            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'search=lookup',
                    'member=trshkv&member=uruz',
                    'owner=ssav',
                    'department=42',
                    'hasExternalMembers=false',
                    'tags=146',
                ].join('&'),
            }))
                .toEqual({
                    filters: {
                        ...defaultFilters,
                        ...filtersFromUrl,
                    },
                    filtersFromUrl,
                    isUrlEmpty: false,
                });
        });

        it('Should not add default filters if they are in url', () => {
            let filtersFromUrl = {
                search: 'lookup',
                member: ['trshkv', 'uruz'],
                owner: ['ssav'],
                department: [42],
                states: [],
                isSuspicious: [],
                hasExternalMembers: [false],
                tags: [146],
            };

            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'search=lookup',
                    'member=trshkv&member=uruz',
                    'owner=ssav',
                    'department=42',
                    'states=',
                    'isSuspicious=',
                    'hasExternalMembers=false',
                    'tags=146',
                ].join('&'),
            }))
                .toEqual({
                    filters: {
                        ...defaultFilters,
                        ...filtersFromUrl,
                    },
                    filtersFromUrl,
                    isUrlEmpty: false,
                });
        });

        it('Should reject unexpected params', () => {
            const filtersFromUrl = {
                search: 'qwe',
            };

            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'search=qwe',
                    'warhammer40000=heresy',
                ].join('&'),
            }))
                .toEqual({
                    filters: {
                        ...defaultFilters,
                        ...filtersFromUrl,
                    },
                    filtersFromUrl,
                    isUrlEmpty: false,
                });
        });

        it('Should reject invalid values', () => {
            const filtersFromUrl = {
                states: ['develop'],
                hasExternalMembers: [false],
                tags: [88],
            };

            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'states=develop&states=overpowered',
                    'department=kenny',
                    'isSuspicious=omg',
                    'hasExternalMembers=007&hasExternalMembers=false',
                    'tags=hallelujah&tags=88',
                ].join('&'),
            }))
                .toEqual({
                    filters: {
                        ...defaultFilters,
                        ...filtersFromUrl,
                    },
                    filtersFromUrl,
                    isUrlEmpty: false,
                });
        });

        it('Should omit repeated values', () => {
            const filtersFromUrl = {
                isSuspicious: [true],
            };

            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'isSuspicious=true',
                    'isSuspicious=true',
                    'isSuspicious=true',
                ].join('&'),
            }))
                .toEqual({
                    filters: {
                        ...defaultFilters,
                        ...filtersFromUrl,
                    },
                    filtersFromUrl,
                    isUrlEmpty: false,
                });
        });

        it('Should allow empty values', () => {
            const filtersFromUrl = {
                search: undefined, // search выбирается как одиночное значение, а не список
                member: [],
                owner: [],
                department: [],
                states: [],
                isSuspicious: [],
                hasExternalMembers: [],
                tags: [],
            };

            const actual = selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'search=',
                    'member=',
                    'owner=',
                    'department=',
                    'states=',
                    'isSuspicious=',
                    'hasExternalMembers=',
                    'tags=',
                ].join('&'),
            });

            expect(actual).toEqual({
                filters: {
                    ...defaultFilters,
                    ...filtersFromUrl,
                },
                filtersFromUrl,
                isUrlEmpty: false,
            });

            // ключ должен быть, но значение должно быть undefined
            expect('search' in actual.filters).toBe(true);
        });

        it('Should memoize properly', () => {
            let initialParams;

            const resetInitialParams = () => {
                initialParams = selectFilters({
                    [GET_STORE_PATH]: '/?' + [
                        'search=lookup',
                        'department=42',
                    ].join('&'),
                });
            };

            resetInitialParams();
            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'search=lookup',
                    'department=42',
                    'isSuspicious=omg', // invalid value
                    'hello=world', // unexpected param
                ].join('&'),
            })).toBe(initialParams); // invalid params and unexpected params keep memoization

            resetInitialParams();
            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'search=lookup',
                    'department=42',
                    'department=42', // repeated value
                ].join('&'),
            })).not.toBe(initialParams); // repeating a value resets memoization

            resetInitialParams();
            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'search=lookup',
                    'department=42',
                    'tags=146', // added valid param
                ].join('&'),
            })).not.toBe(initialParams); // adding a valid param resets memoization

            resetInitialParams();
            expect(selectFilters({
                [GET_STORE_PATH]: '/?' + [
                    'search=lookup',
                ].join('&'),
            })).not.toBe(initialParams); // removing a param resets memoization
        });
    });

    describe('selectOpenNodes', () => {
        it('Should select all open nodes in tree', () => {
            const openNodes = selectOpenNodes({
                catalogue: {
                    tree: {
                        '0': {
                            isRoot: true,
                            children: [1, 2],
                        },
                        '1': {
                            data: { id: 1 },
                            isOpen: false,
                            children: [1.1],
                        },
                        '1.1': {
                            data: { id: 1.1 },
                            isOpen: true,
                            children: [],
                        },
                        '2': {
                            data: { id: 2 },
                            isOpen: true,
                            children: [],
                        },
                    },
                },
            }, 0);

            expect(openNodes).toEqual([2]);
        });
    });

    describe('getFilterMode', () => {
        it('shallow', () => {
            const filterMode = getFilterMode({
                catalogue: {
                    status: 'pending',
                    error: null,
                    tree: {},
                    filterMode: FILTER_MODE.SHALLOW,
                },
            });

            expect(filterMode).toEqual(FILTER_MODE.SHALLOW);
        });

        it('deep', () => {
            const filterMode = getFilterMode({
                catalogue: {
                    status: 'pending',
                    error: null,
                    tree: {},
                    filterMode: FILTER_MODE.DEEP,
                },
            });

            expect(filterMode).toEqual(FILTER_MODE.DEEP);
        });
    });

    describe('selectLabels', () => {
        it('Should select labels for passed keys', () => {
            const labels = selectLabels({
                catalogue: {
                    labels: {
                        member: { '123': 'some label', '23': 'another label' },
                    },
                },
            }, 'member', ['123']);

            expect(labels).toEqual(['some label']);
        });

        it('Should degrade to keys if labels are not loaded', () => {
            expect(selectLabels({
                catalogue: {
                    labels: {
                        member: {},
                    },
                },
            }, 'member', ['foo'])).toEqual(['foo']);

            expect(selectLabels({
                catalogue: {
                    labels: {},
                },
            }, 'member', ['foo'])).toEqual(['foo']);
        });

        it('Should return undefined without data', () => {
            expect(selectLabels()).toBeUndefined();
        });
    });
});
