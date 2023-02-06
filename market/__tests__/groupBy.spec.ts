import {groupBy} from '../groupBy';

describe('groupBy', () => {
    it('группирует массив объектов по заданному полю', () => {
        const array = [{id: '1'}, {id: '1'}, {id: '2'}, {id: '2'}];
        const result = groupBy(array, 'id');

        expect(result).toEqual({
            '1': [{id: '1'}, {id: '1'}],
            '2': [{id: '2'}, {id: '2'}],
        });
    });

    it('возвращает пустой объект для пустого массива', () => {
        const result = groupBy([], 'id');

        expect(result).toEqual({});
    });

    it('корректно группирует массив объектов с типом заданного поля, отличным от string', () => {
        const array = [{id: 1}, {id: 1}, {id: '2'}, {id: 2}];
        const result = groupBy(array, 'id');

        expect(result).toEqual({
            '1': [{id: 1}, {id: 1}],
            '2': [{id: '2'}, {id: 2}],
        });
    });
});
