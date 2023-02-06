import { ObjectDiffIterator } from '../ObjectDiffIterator';

describe('ObjectDiffIterator', () => {
    it('done should be true if objects is same', () => {
        const obj = {
            '1': 1,
            '2': { a: 1 },
            '3': 3,
        };

        expect([...new ObjectDiffIterator(obj, { ...obj })]).toStrictEqual([]);
        expect([...new ObjectDiffIterator(obj, obj)]).toStrictEqual([]);
    });

    it('should return all keys if obj1 is undefined', () => {
        const obj = {
            '1': 1,
            '2': { a: 1 },
            '3': 3,
        };

        const iterator = new ObjectDiffIterator(undefined, obj);

        expect([...iterator]).toStrictEqual(['1', '2', '3']);
    });

    it('should return all keys if obj2 is undefined', () => {
        const obj = {
            '1': 1,
            '2': { a: 1 },
            '3': 3,
        };

        const iterator = new ObjectDiffIterator(obj, undefined);

        expect([...iterator]).toStrictEqual(['1', '2', '3']);
    });

    it('should return all keys if obj1 is empty', () => {
        const obj = {
            '1': 1,
            '2': { a: 1 },
            '3': 3,
        };

        const iterator = new ObjectDiffIterator({}, obj);

        expect([...iterator]).toStrictEqual(['1', '2', '3']);
    });

    it('should return all keys if obj2 is empty', () => {
        const obj = {
            '1': 1,
            '2': { a: 1 },
            '3': 3,
        };

        const iterator = new ObjectDiffIterator(obj, {});

        expect([...iterator]).toStrictEqual(['1', '2', '3']);
    });

    it('iterator should goes over all changed keys and deleted keys', () => {
        const iterator = new ObjectDiffIterator(
            {
                '1': 3,
                '3': 3,
            },
            {
                '1': 1,
                '2': 2,
                '3': 3,
            },
        );

        expect([...iterator]).toStrictEqual(['1', '2']);
    });

    it('iterator should goes over all deleted keys and new keys', () => {
        const iterator = new ObjectDiffIterator(
            {
                '3': 3,
            },
            {
                '1': 1,
                '3': 3,
                '4': 4,
            },
        );

        expect([...iterator]).toStrictEqual(['1', '4']);
    });

    it('iterator should goes over all changed keys and new keys', () => {
        const iterator = new ObjectDiffIterator(
            {
                '1': 3,
                '3': 3,
                '2': 2,
            },
            {
                '1': 1,
                '3': 3,
                '4': 4,
            },
        );

        expect([...iterator]).toStrictEqual(['1', '2', '4']);
    });

    it('should return array by iterator', () => {
        const iterator = new ObjectDiffIterator(
            {
                '1': 3,
                '3': 3,
                '2': 2,
            },
            {
                '1': 1,
                '3': 3,
                '4': 4,
            },
        );

        expect([...iterator]).toStrictEqual(['1', '2', '4']);
    });
});
