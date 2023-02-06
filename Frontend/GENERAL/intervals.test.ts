import intersection from 'lodash/intersection';
import { makeSortedIntervals, selectItemsByInterval } from './intervals';

interface TestInterval {
    start: Date;
    end: Date;
}

function generateDateItemsGrid(count: number): TestInterval[] {
    const res: TestInterval[] = [];
    for (let left = 1; left < count; left++) {
        for (let right = left + 1; right <= count; right++) {
            res.push({
                start: new Date(2021, 8, left),
                end: new Date(2021, 8, right),
            });
        }
    }
    return res;
}

function doCmp(left: Date, op: '<' | '==' | '>', right: Date) {
    switch (op) {
        case '<': return left.valueOf() < right.valueOf();
        case '==': return left.valueOf() === right.valueOf();
        case '>': return left.valueOf() > right.valueOf();
    }
}

function cmp(...args: Array<Date | '<' | '==' | '>'>): boolean {
    if (args.length === 1) return true;

    // @ts-expect-error
    if (!doCmp(...args.slice(0, 3))) {
        return false;
    }

    return cmp(...args.slice(2));
}

const intervalSort = (a: TestInterval, b: TestInterval) =>
    (a.start.valueOf() - b.start.valueOf()) || (a.end.valueOf() - b.end.valueOf());

describe('Intervals utils', () => {
    describe('selectItemsByInterval', () => {
        const from = new Date(2021, 8, 3);
        const to = new Date(2021, 8, 7);

        const itemsGrid = generateDateItemsGrid(9);

        const earlierApart = itemsGrid.filter(({ start, end }) => cmp(start, '<', end, '<', from, '<', to));
        const earlierJoint = itemsGrid.filter(({ start, end }) => cmp(start, '<', end, '==', from, '<', to));
        const earlierOverlap = itemsGrid.filter(({ start, end }) => cmp(start, '<', from, '<', end, '<', to));
        const earlierAndIncludes = itemsGrid.filter(({ start, end }) => cmp(start, '<', from, '<', end, '==', to));

        const earlierAndLater = itemsGrid.filter(({ start, end }) => cmp(start, '<', from, '<', to, '<', end));
        const match = itemsGrid.filter(({ start, end }) => cmp(start, '==', from, '<', to, '==', end));

        const includedJointStart = itemsGrid.filter(({ start, end }) => cmp(from, '==', start, '<', end, '<', to));
        const included = itemsGrid.filter(({ start, end }) => cmp(from, '<', start, '<', end, '<', to));
        const includedJointEnd = itemsGrid.filter(({ start, end }) => cmp(from, '<', start, '<', end, '==', to));

        const laterAndIncludes = itemsGrid.filter(({ start, end }) => cmp(from, '==', start, '<', to, '<', end));
        const laterOverlap = itemsGrid.filter(({ start, end }) => cmp(from, '<', start, '<', to, '<', end));
        const laterJoint = itemsGrid.filter(({ start, end }) => cmp(from, '<', to, '==', start, '<', end));
        const laterApart = itemsGrid.filter(({ start, end }) => cmp(from, '<', to, '<', start, '<', end));

        it('test intervals distinct', () => {
            const testIntervals = [
                earlierApart,
                earlierJoint,
                earlierOverlap,
                earlierAndIncludes,
                earlierAndLater,
                match,
                includedJointStart,
                included,
                includedJointEnd,
                laterAndIncludes,
                laterOverlap,
                laterJoint,
                laterApart,
            ];

            const acc: TestInterval[] = [];
            testIntervals.forEach(ti => {
                expect(intersection(acc, ti)).toEqual([]);
                acc.push(...ti);
            });

            expect(itemsGrid).toEqual(acc.sort(intervalSort));
        });

        it('should select from all', () => {
            const sortedIntervals = makeSortedIntervals(itemsGrid);
            const expected: TestInterval[] = [
                ...earlierJoint,
                ...earlierOverlap,
                ...earlierAndIncludes,
                ...earlierAndLater,
                ...match,
                ...includedJointStart,
                ...included,
                ...includedJointEnd,
                ...laterAndIncludes,
                ...laterOverlap,
                ...laterJoint,
            ].sort(intervalSort);

            const actual = selectItemsByInterval(sortedIntervals, from, to).sort(intervalSort);
            expect(actual).toEqual(expected);
        });

        it('should select from nothing', () => {
            const sortedIntervals = makeSortedIntervals([]);
            const expected: TestInterval[] = [];

            const actual = selectItemsByInterval(sortedIntervals, from, to).sort(intervalSort);
            expect(actual).toEqual(expected);
        });

        it('should select from non-match', () => {
            const sortedIntervals = makeSortedIntervals([...earlierApart, ...laterApart]);
            const expected: TestInterval[] = [];

            const actual = selectItemsByInterval(sortedIntervals, from, to).sort(intervalSort);
            expect(actual).toEqual(expected);
        });

        it('should select from all-match', () => {
            const sortedIntervals = makeSortedIntervals([
                ...earlierJoint,
                ...earlierOverlap,
                ...earlierAndIncludes,
                ...earlierAndLater,
                ...match,
                ...includedJointStart,
                ...included,
                ...includedJointEnd,
                ...laterAndIncludes,
                ...laterOverlap,
                ...laterJoint,
            ]);
            const expected: TestInterval[] = [
                ...earlierJoint,
                ...earlierOverlap,
                ...earlierAndIncludes,
                ...earlierAndLater,
                ...match,
                ...includedJointStart,
                ...included,
                ...includedJointEnd,
                ...laterAndIncludes,
                ...laterOverlap,
                ...laterJoint,
            ].sort(intervalSort);

            const actual = selectItemsByInterval(sortedIntervals, from, to).sort(intervalSort);
            expect(actual).toEqual(expected);
        });

        it('should select from left-match right-no-match', () => {
            const sortedIntervals = makeSortedIntervals([
                ...included,
                ...laterApart,
            ]);
            const expected: TestInterval[] = included;

            const actual = selectItemsByInterval(sortedIntervals, from, to).sort(intervalSort);
            expect(actual).toEqual(expected);
        });

        it('should select from left-no-match right-match', () => {
            const sortedIntervals = makeSortedIntervals([
                ...earlierApart,
                ...included,
            ]);
            const expected: TestInterval[] = included;

            const actual = selectItemsByInterval(sortedIntervals, from, to).sort(intervalSort);
            expect(actual).toEqual(expected);
        });
    });
});
