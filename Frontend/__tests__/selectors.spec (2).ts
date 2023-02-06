import {
    selectFilterCollections,
    selectFilterById,
    selectFiltersByIds,
    selectSelectedFilterValuesIds,
    selectSelectedFilters,
    selectFilterQueryValue,
    selectFiltersQueryValues,
} from '../selectors';
import * as radioGetters from '../radioFilter/getters';
import * as enumFilter from '../enumFilter/getters';
import * as booleanFilter from '../booleanFilter/getters';
import * as rangeFilter from '../rangeFilter/getters';
import { FilterType } from '../types';

describe('Селектор:', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let state: any;

    function makeFiltersNotSelected(state: object) {
        const filter = state['beru.ru'].collections.filter;

        filter['1'].selectedValuesIds.length = 0;
        filter['2'].valuesMap['22'].value = '';
        filter['3'].selectedValuesIds.length = 0;
        filter['4'].selectedValuesIds.length = 0;
    }

    beforeEach(() => {
        state = {
            'beru.ru': {
                collections: {
                    filter: {
                        '1': {
                            id: '1',
                            type: FilterType.BOOLEAN,
                            selectedValuesIds: ['11'],
                            valuesMap: {
                                '11': { id: '11', checked: true },
                            },
                        },
                        '2': {
                            id: '2',
                            type: FilterType.RANGE,
                            valuesIds: ['22', '22-1'],
                            valuesMap: {
                                '22': { id: '22', value: 'test' },
                                '22-1': { id: '22-1' },
                            },
                        },
                        '3': {
                            id: '3',
                            type: FilterType.ENUM,
                            selectedValuesIds: ['33', '33-1'],
                        },
                        '4': {
                            id: '4',
                            type: FilterType.RADIO,
                            selectedValuesIds: ['44'],
                            valuesMap: {
                                '44': { id: '44' },
                            },
                        },
                        '5': { id: '5', type: FilterType.UNKNOWN, selectedValuesIds: ['123'] },
                    },
                },
            },
        };
    });

    describe('selectFilterCollections', () => {
        it('возвращает коллекцию фильтров', () => {
            expect(selectFilterCollections(state)).toEqual(state['beru.ru'].collections.filter);
        });
    });

    describe('selectFilterById', () => {
        it('возвращает фильтр по id', () => {
            expect(selectFilterById(state, '1')).toEqual(state['beru.ru'].collections.filter['1']);
        });
    });

    describe('selectFiltersByIds', () => {
        it('возвращает список фильтров по переданным индентификаторам', () => {
            const filter = state['beru.ru'].collections.filter;

            expect(selectFiltersByIds(state, ['1', '3'])).toEqual([filter['1'], filter['3']]);
        });
    });

    describe('selectSelectedFilterValuesIds', () => {
        describe('Boolean фильтр', () => {
            it('возвращает идентификатор выбранного значения', () => {
                expect(selectSelectedFilterValuesIds(state, '1')).toEqual(['11']);
            });

            it('возвращает пустой массив, если значение не выбрано', () => {
                const boolFilter = state['beru.ru'].collections.filter['1'];

                boolFilter.valuesMap['11'].checked = false;

                expect(selectSelectedFilterValuesIds(state, '1')).toEqual([]);

                boolFilter.valuesMap['11'].checked = true;
                boolFilter.selectedValuesIds.length = 0;

                expect(selectSelectedFilterValuesIds(state, '1')).toEqual([]);
            });
        });

        describe('Radio фильтр', () => {
            it('возвращает идентификатор выбранного значения', () => {
                expect(selectSelectedFilterValuesIds(state, '4')).toEqual(['44']);
            });

            it('возвращает пустой массив если значение не выбрано или выбранное значение равно "Не важно"', () => {
                const radioFilter = state['beru.ru'].collections.filter['4'];

                radioFilter.selectedValuesIds.length = 0;

                expect(selectSelectedFilterValuesIds(state, '4')).toEqual([]);

                radioFilter.selectedValuesIds = ['id_no_matter'];
                radioFilter.valuesMap.id_no_matter = { id: 'id_no_matter' };

                expect(selectSelectedFilterValuesIds(state, '4')).toEqual([]);
            });
        });

        describe('Range фильтр', () => {
            it('возвращает идентификатор выбранного значения', () => {
                expect(selectSelectedFilterValuesIds(state, '2')).toEqual(['22']);
            });
        });

        describe('Enum фильтр', () => {
            it('возвращает идентификаторы выбранных значений', () => {
                expect(selectSelectedFilterValuesIds(state, '3')).toEqual(['33', '33-1']);
            });
        });

        it('возвращает пустой массив если переданный фильтр не известен', () => {
            expect(selectSelectedFilterValuesIds(state, '5')).toEqual([]);
        });
    });

    describe('selectSelectedFilters', () => {
        it('возвращает индентификаторы измененных фильтров со списком идентификаторов измененных значений', () => {
            expect(selectSelectedFilters(state)).toEqual([
                { filterId: '1', changedValuesIds: ['11'] },
                { filterId: '2', changedValuesIds: ['22'] },
                { filterId: '3', changedValuesIds: ['33', '33-1'] },
                { filterId: '4', changedValuesIds: ['44'] },
            ]);
        });

        it('возвращает пустой массив если нет изменных фильтров', () => {
            makeFiltersNotSelected(state);

            expect(selectSelectedFilters(state)).toEqual([]);
        });
    });

    describe('selectFilterQueryValue', () => {
        it('возвращает хэшмап только выбранных значений искомого фильтра ', () => {
            const filter = state['beru.ru'].collections.filter;
            const getRangeFilterQueryValue = jest.spyOn(rangeFilter, 'getRangeFilterQueryValue').mockReturnValue([]);
            const getEnumFilterQueryValue = jest.spyOn(enumFilter, 'getEnumFilterQueryValue').mockReturnValue([]);
            const getRadioFilterQueryValue = jest.spyOn(radioGetters, 'getRadioFilterQueryValue').mockReturnValue([]);
            const getBooleanFilterQueryValue = jest.spyOn(booleanFilter, 'getBooleanFilterQueryValue').mockReturnValue([]);

            selectFilterQueryValue(state, '1');
            selectFilterQueryValue(state, '2');
            selectFilterQueryValue(state, '3');
            selectFilterQueryValue(state, '4');

            expect(getBooleanFilterQueryValue).toHaveBeenCalledWith(filter['1']);
            expect(getRangeFilterQueryValue).toHaveBeenCalledWith(filter['2']);
            expect(getEnumFilterQueryValue).toHaveBeenCalledWith(filter['3']);
            expect(getRadioFilterQueryValue).toHaveBeenCalledWith(filter['4']);
        });

        it('если искомый фильтр не поддерживаетя, то возвращает пустой массив', () => {
            expect(selectFilterQueryValue(state, '5')).toEqual([]);
        });
    });

    describe('selectFiltersQueryValues', () => {
        it('возвращает хэшмап только выбранных значений по каждому фильтру', () => {
            jest.spyOn(rangeFilter, 'getRangeFilterQueryValue').mockReturnValue([['id-1', '1']]);
            jest.spyOn(enumFilter, 'getEnumFilterQueryValue').mockReturnValue([['id-2', '2'], ['id-2', '22']]);
            jest.spyOn(radioGetters, 'getRadioFilterQueryValue').mockReturnValue([['glfilter', '3']]);
            jest.spyOn(booleanFilter, 'getBooleanFilterQueryValue').mockReturnValue([['glfilter', '4']]);

            expect(selectFiltersQueryValues(state)).toEqual({
                glfilter: ['4', '3'],
                'id-1': '1',
                'id-2': ['2', '22'],
            });
        });

        it('если выбранных значений нет, возвращается пустой объект', () => {
            jest.spyOn(rangeFilter, 'getRangeFilterQueryValue').mockReturnValue([]);
            jest.spyOn(enumFilter, 'getEnumFilterQueryValue').mockReturnValue([]);
            jest.spyOn(radioGetters, 'getRadioFilterQueryValue').mockReturnValue([]);
            jest.spyOn(booleanFilter, 'getBooleanFilterQueryValue').mockReturnValue([]);

            expect(selectFiltersQueryValues(state)).toEqual({});
        });
    });
});
