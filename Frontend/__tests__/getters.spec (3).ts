import { getRadioFilterHumanReadableValue, getRadioFilterSelectedValue, isNoMatterValueId, getRadioFilterQueryValue } from '../getters';

describe('Функция:', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let filter: any;

    beforeEach(() => {
        filter = {
            selectedValuesIds: ['2'],
            valuesMap: {
                '1': { value: 'one' },
                '2': { value: 'two' },
            },
        };
    });

    describe('getRadioFilterSelectedValue', () => {
        it('возвращает выбранное значение', () => {
            expect(getRadioFilterSelectedValue(filter)).toEqual({ value: 'two' });
        });
    });

    describe('getRadioFilterHumanReadableValue', () => {
        it('возвращает понятное для человека значение фильтра', () => {
            expect(getRadioFilterHumanReadableValue('1')).toEqual('Да');
            expect(getRadioFilterHumanReadableValue('0')).toEqual('Нет');
            expect(getRadioFilterHumanReadableValue('delivery')).toEqual('Курьером');
            expect(getRadioFilterHumanReadableValue('pickup')).toEqual('Самовывозом');
            expect(getRadioFilterHumanReadableValue('post')).toEqual('Почтой');
        });
    });

    describe('isNoMatterValueId', () => {
        it('должна определять является ли переданный идентификатор значения равным идентификатору значения "Не важно"', () => {
            expect(isNoMatterValueId('444_one')).toEqual(false);
            expect(isNoMatterValueId('444_2')).toEqual(false);
            expect(isNoMatterValueId('444_no_matter')).toEqual(true);
        });
    });

    describe('getRadioFilterQueryValue', () => {
        it('если нет выбранных значений, то возвращает пустой массив', () => {
            filter.selectedValuesIds = [];

            expect(getRadioFilterQueryValue(filter)).toEqual([]);
        });

        it('если выбранное значение является значением "Не важно", то возвращает пустой массив', () => {
            filter.id = '444';
            filter.selectedValuesIds = ['444_no_matter'];
            filter.valuesMap['444_no_matter'] = { id: '444_no_matter', value: 'no_matter' };

            expect(getRadioFilterQueryValue(filter)).toEqual([]);
        });

        it('возвращает массив из одого кортежа c выбранным значением', () => {
            filter.id = 'any-id';

            expect(getRadioFilterQueryValue(filter)).toEqual([['any-id', 'two']]);
        });

        it('для gl фильтра возвращает массив из одного кортежа с выбранным значением', () => {
            filter.id = '444';

            expect(getRadioFilterQueryValue(filter)).toEqual([['glfilter', '444:two']]);
        });
    });
});
