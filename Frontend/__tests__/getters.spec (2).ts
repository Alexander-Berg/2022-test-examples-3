import { getEnumFilterAllValueIds, getEnumFilterTopValueIds, getEnumFilterQueryValue } from '../getters';

describe('Функция:', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let filter: any;

    beforeEach(() => {
        filter = {
            selectedValuesIds: ['1', '2'],
            valuesGroups: {
                top: ['1'],
                all: ['1', '2'],
            },
        };
    });

    describe('getEnumFilterTopValueIds', () => {
        it('возвращает top значение', () => {
            expect(getEnumFilterTopValueIds(filter)).toEqual(['1']);
        });
    });

    describe('getEnumFilterAllValueIds', () => {
        it('возвращает all значение', () => {
            expect(getEnumFilterAllValueIds(filter)).toEqual(['1', '2']);
        });
    });

    describe('getEnumFilterQueryValue', () => {
        it('если нет выбранных значений, то возвращает пустой массив', () => {
            filter.selectedValuesIds = [];

            expect(getEnumFilterQueryValue(filter)).toEqual([]);
        });

        it('возвращает массив из одого кортежа c выбранными значениями', () => {
            filter.id = 'any-id';

            expect(getEnumFilterQueryValue(filter)).toEqual([['any-id', '1,2']]);
        });

        it('для gl фильтра возвращает массив из одного кортежа с выбранными значениями', () => {
            filter.id = '444';

            expect(getEnumFilterQueryValue(filter)).toEqual([['glfilter', '444:1,2']]);
        });
    });
});
