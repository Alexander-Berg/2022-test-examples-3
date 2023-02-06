import { getBooleanFilterValuesSplitByStatus, getBooleanFilterQueryValue, getBooleanFilterSelectedValue } from '../getters';

describe('Функция:', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let filter: any;

    beforeEach(() => {
        filter = {
            selectedValuesIds: ['2'],
            valuesIds: ['1', '2'],
            valuesMap: {
                '1': { value: '0' },
                '2': { value: '1', checked: true },
            },
        };
    });

    describe('getBooleanFilterSelectedValue', () => {
        it('возвращает выбранное значение', () => {
            expect(getBooleanFilterSelectedValue(filter)).toEqual({ value: '1', checked: true });
        });
    });

    describe('getBooleanFilterValuesSplitByStatus', () => {
        it('возвращает идентификаторы выбранного значения и не выбранного', () => {
            expect(getBooleanFilterValuesSplitByStatus(filter)).toEqual({
                checkedValueId: '2',
                notCheckedValueId: '1',
            });

            filter.selectedValuesIds = ['1'];

            expect(getBooleanFilterValuesSplitByStatus(filter)).toEqual({
                checkedValueId: '1',
                notCheckedValueId: '2',
            });
        });
    });

    describe('getBooleanFilterQueryValue', () => {
        it('если нет выбранных значений, то возвращает пустой массив', () => {
            filter.selectedValuesIds = [];

            expect(getBooleanFilterQueryValue(filter)).toEqual([]);
        });

        it('для gl фильтра возвращает массив из одного кортежа с выбранным значением', () => {
            filter.id = '444';

            expect(getBooleanFilterQueryValue(filter)).toEqual([['glfilter', '444:1']]);
        });

        it('возвращает массив из одого кортежа c выбранным значением', () => {
            filter.id = 'any-id';

            expect(getBooleanFilterQueryValue(filter)).toEqual([['any-id', '1']]);
        });
    });
});
