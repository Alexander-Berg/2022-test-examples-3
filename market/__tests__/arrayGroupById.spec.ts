import {arrayGroupBy} from '../arrayGroupBy';

describe('arrayGroupBy', () => {
    it('массив объектов с field:string', () => {
        const arr = [{id: '1'}, {id: '1'}, {id: '2'}, {id: '2'}];
        const result = arrayGroupBy(arr, 'id');

        expect(result).toHaveLength(2);
        expect(result[0]).toHaveLength(2);
        expect(result[1]).toHaveLength(2);
    });

    it('массив объектов с типом значения field, отличным от string', () => {
        const arr = [{id: 1}, {id: 1}, {id: 2}, {id: 2}];
        // as добавлен чтобы на типы не ругался, а тест выполнился.
        const result = arrayGroupBy((arr as unknown) as {id: string}[], 'id');

        expect(result).toHaveLength(0);
    });

    it('пустой массив', () => {
        const arr = [];
        // as добавлен чтобы на типы не ругался, а тест выполнился.
        const result = arrayGroupBy((arr as unknown) as {id: string}[], 'id');

        expect(result).toHaveLength(0);
    });

    it('массив объектов с смешанным типом значения field', () => {
        const arr = [{id: 1}, {id: '1'}, {id: 2}, {id: 2}];
        const result = arrayGroupBy(arr, 'id');

        expect(result).toHaveLength(1);
        expect(result[0]).toHaveLength(1);
    });
});
