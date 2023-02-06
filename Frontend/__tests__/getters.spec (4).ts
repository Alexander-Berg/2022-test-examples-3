import {
    getRangeFilterMinValueId,
    getRangeFilterMaxValueId,
    getRangeFilterMinValue,
    getRangeFilterMaxValue,
    getRangeFilterSelectedValues,
    getRangeFilterQueryValue,
} from '../getters';

describe('Функция: ', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let filter: any;

    beforeEach(() => {
        filter = {
            valuesIds: ['1', '2'],
            valuesMap: {
                '1': { id: '1', value: '1' },
                '2': { id: '2', value: '2' },
            },
        };
    });

    describe('getRangeFilterMinValueId', () => {
        it('возвращает id минимального значение', () => {
            expect(getRangeFilterMinValueId(filter)).toEqual('1');
        });
    });
    describe('getRangeFilterMaxValueId', () => {
        it('возвращает id максимального значения', () => {
            expect(getRangeFilterMaxValueId(filter)).toEqual('2');
        });
    });
    describe('getRangeFilterMinValue', () => {
        it('возвращает минимальное значение', () => {
            expect(getRangeFilterMinValue(filter)).toEqual('1');
        });
    });
    describe('getRangeFilterMaxValue', () => {
        it('возвращает минимальное значение', () => {
            expect(getRangeFilterMaxValue(filter)).toEqual('2');
        });
    });
    describe('getRangeFilterSelectedValues', () => {
        it('возвращает массив заполненных значений', () => {
            filter.valuesMap['1'].value = '';

            expect(getRangeFilterSelectedValues(filter)).toEqual(['2']);
        });
    });

    describe('getRangeFilterQueryValue', () => {
        it('если id фильтра равен glprice и заполнены оба значения, то возвращается кортеж из массивов с обоими значениями', () => {
            filter.id = 'glprice';

            expect(getRangeFilterQueryValue(filter)).toEqual([
                ['pricefrom', '1'],
                ['priceto', '2'],
            ]);
        });

        it('если id фильтра равен glprice и заполнено только одно значение "От", то возвращается массив с кортежем только этого значения', () => {
            filter.id = 'glprice';

            ['', null, undefined].forEach(value => {
                filter.valuesMap['2'].value = value;

                expect(getRangeFilterQueryValue(filter)).toEqual([
                    ['pricefrom', '1'],
                ]);
            });
        });

        it('если id фильтра равен glprice и заполнено только одно значение "До", то возвращается массив с кортежем только этого значения', () => {
            filter.id = 'glprice';

            ['', null, undefined].forEach(value => {
                filter.valuesMap['1'].value = value;

                expect(getRangeFilterQueryValue(filter)).toEqual([
                    ['priceto', '2'],
                ]);
            });
        });

        it('если заполнены оба значения, то возвращается кортеж из массивов с обоими значениями', () => {
            filter.id = '444';

            expect(getRangeFilterQueryValue(filter)).toEqual([
                ['glfilter', '444:1~2'],
            ]);
        });

        it('если заполнено только одно значение "От", то возвращается массив с кортежем только этого значения', () => {
            filter.id = '444';

            ['', null, undefined].forEach(value => {
                filter.valuesMap['2'].value = value;

                expect(getRangeFilterQueryValue(filter)).toEqual([
                    ['glfilter', '444:1~'],
                ]);
            });
        });

        it('если заполнено только одно значение "До", то возвращается массив с кортежем только этого значения', () => {
            filter.id = '444';

            ['', null, undefined].forEach(value => {
                filter.valuesMap['1'].value = value;

                expect(getRangeFilterQueryValue(filter)).toEqual([
                    ['glfilter', '444:~2'],
                ]);
            });
        });
    });
});
