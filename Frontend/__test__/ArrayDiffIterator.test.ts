import { ArrayDiffIterator } from '../ArrayDiffIterator';

describe('ArrayDiffIterator', () => {
    it('done should be true if arrs is same', () => {
        const arr = ['1', '2', '3'];

        expect([...new ArrayDiffIterator(arr, [...arr])]).toStrictEqual([]);
        expect([...new ArrayDiffIterator(arr, arr)]).toStrictEqual([]);
    });

    it('should return all keys if arr1 is undefined', () => {
        const arr = ['1', '2', '3'];

        const iterator = new ArrayDiffIterator(undefined, arr);

        expect([...iterator]).toStrictEqual(['1', '2', '3']);
    });

    it('should return all keys if arr2 is undefined', () => {
        const arr = ['1', '2', '3'];

        const iterator = new ArrayDiffIterator(arr, undefined);

        expect([...iterator]).toStrictEqual(['1', '2', '3']);
    });

    it('should return all keys if arr1 is empty', () => {
        const arr = ['1', '2', '3'];

        const iterator = new ArrayDiffIterator([], arr);

        expect([...iterator]).toStrictEqual(['1', '2', '3']);
    });

    it('should return all keys if arr2 is empty', () => {
        const arr = ['1', '2', '3'];

        const iterator = new ArrayDiffIterator(arr, []);

        expect([...iterator]).toStrictEqual(['1', '2', '3']);
    });

    it('iterator should goes over all changed keys and deleted keys', () => {
        const iterator = new ArrayDiffIterator(
            ['3'],
            ['1', '2', '3'],
        );

        expect([...iterator]).toStrictEqual(['1', '2']);
    });

    it('iterator should goes over all changed keys and new keys', () => {
        const iterator = new ArrayDiffIterator(
            ['1', '3', '5'],
            ['2', '3', '4'],
        );

        expect([...iterator]).toStrictEqual(['1', '5', '2', '4']);
    });

    it('should return array by iterator', () => {
        const iterator = new ArrayDiffIterator(
            ['1', '3'],
            ['2', '3', '4'],
        );

        expect([...iterator]).toStrictEqual(['1', '2', '4']);
    });
});
