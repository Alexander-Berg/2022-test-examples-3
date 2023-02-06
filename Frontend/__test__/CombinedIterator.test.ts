import { CombinedIterator } from '../CombinedIterator';

describe('CombinedIterator', () => {
    it('should goues by all items', () => {
        expect([...new CombinedIterator([1, 2, 3])]).toStrictEqual([1, 2, 3]);
        expect([...new CombinedIterator([1, 2, 3], [3, 4, 5])]).toStrictEqual([1, 2, 3, 3, 4, 5]);
    });

    it('should goues by all items except excluded by predicate', () => {
        const predicateFactory = () => {
            const walked = new Set<number>();

            return (value: number) => {
                if (walked.has(value)) {
                    return false;
                }

                walked.add(value);

                return true;
            };
        };

        expect([...new CombinedIterator([1, 2, 3]).filter(predicateFactory())])
            .toStrictEqual([1, 2, 3]);

        expect([...new CombinedIterator([1, 2, 3], [3, 4, 5]).filter(predicateFactory())])
            .toStrictEqual([1, 2, 3, 4, 5]);
    });

    it('should return empty array', () => {
        expect([...new CombinedIterator([])]).toStrictEqual([]);
    });
});
