import { createDiff, getArrayDiff } from '../diff';

describe('diff', () => {
    describe('#getArrayDiff', () => {
        it('Same length test', () => {
            expect(getArrayDiff([1, 2, 3, 4], [0, 2, 5, 1]))
                .toMatchObject({
                    added: [0, 5],
                    removed: [3, 4],
                    unchanged: [1, 2],
                });
        });

        it('New array empty test', () => {
            expect(getArrayDiff([1, 2, 3, 4], []))
                .toMatchObject({
                    added: [],
                    removed: [1, 2, 3, 4],
                    unchanged: [],
                });
        });

        it('Old array empty test', () => {
            expect(getArrayDiff([], [1, 2, 3, 4]))
                .toMatchObject({
                    added: [1, 2, 3, 4],
                    removed: [],
                    unchanged: [],
                });
        });

        it('Old array bigger then new test', () => {
            expect(getArrayDiff([1, 2, 3, 4], [2, 3]))
                .toMatchObject({
                    added: [],
                    removed: [1, 4],
                    unchanged: [2, 3],
                });
        });

        it('New array bigger then old test', () => {
            expect(getArrayDiff([1, 4], [2, 3, 4, 5]))
                .toMatchObject({
                    added: [2, 3, 5],
                    removed: [1],
                    unchanged: [4],
                });
        });
    });

    describe('#changed', () => {
        it('Should be true if all props was changed', () => {
            const diff = createDiff({ a: 1, b: 2 }, { a: 2, b: 3 });

            expect(diff.changed()).toBeTruthy();
        });

        it('Should be true if one of props was changed', () => {
            const diff = createDiff({ a: 1, b: 2 }, { a: 1, b: 3 });

            expect(diff.changed()).toBeTruthy();
        });

        it('Should be false if no one of props was changed', () => {
            const diff = createDiff({ a: 1, b: 2 }, { a: 1, b: 2 });

            expect(diff.changed()).toBeFalsy();
        });
    });

    describe('#changedExclude', () => {
        it('Should be true if all props was changed', () => {
            const diff = createDiff({ a: 1, b: 2 }, { a: 2, b: 3 });

            expect(diff.changedExclude()).toBeTruthy();
        });

        it('Should be true if not excluded props was changed', () => {
            const diff = createDiff({ a: 1, b: 2, c: 1 }, { a: 2, b: 3, c: 1 });

            expect(diff.changedExclude('b')).toBeTruthy();
        });

        it('Should be false if no one of not excluded props was changed', () => {
            const diff = createDiff({ a: 1, b: 2 }, { a: 1, b: 2 });

            expect(diff.changedExclude('b')).toBeFalsy();
        });
    });

    describe('#changedOneOf', () => {
        it('Should be true if one of props was changed', () => {
            const diff = createDiff({ a: 1, b: 2, c: 1 }, { a: 2, b: 3, c: 1 });

            expect(diff.changedOneOf('a')).toBeTruthy();
            expect(diff.changedOneOf('a', 'c')).toBeTruthy();
        });

        it('Should be false if no one of props was changed', () => {
            const diff = createDiff({ a: 1, b: 2 }, { a: 1, b: 2 });

            expect(diff.changedOneOf('a')).toBeFalsy();
            expect(diff.changedOneOf('a', 'b')).toBeFalsy();
        });
    });

    describe('#changedFrom', () => {
        it('Should be true if prop was changed from value', () => {
            const diff = createDiff({ a: 1, b: 2, c: 1 }, { a: 2, b: 3, c: 1 });

            expect(diff.changedFrom('a', 2)).toBeTruthy();
            expect(diff.changedFrom('a', [1, 2])).toBeTruthy();
            expect(diff.changedFrom('a', 1)).toBeFalsy();
        });

        it('Should be true if prop was changed from value1 to value2', () => {
            const diff = createDiff({ a: 1, b: 2, c: 1 }, { a: 2, b: 3, c: 1 });

            expect(diff.changedFrom('a', 2, 1)).toBeTruthy();
            expect(diff.changedFrom('a', [1, 2], [3, 1])).toBeTruthy();
            expect(diff.changedFrom('a', 2, 3)).toBeFalsy();
            expect(diff.changedFrom('a', [1, 4], [3, 2])).toBeFalsy();
            expect(diff.changedFrom('b', 1, 1)).toBeFalsy();
            expect(diff.changedFrom('c', 3, 1)).toBeFalsy();
        });
    });

    describe('#changedTo', () => {
        it('Should be true if prop was changed to value', () => {
            const diff = createDiff({ a: 1, b: 2, c: 1 }, { a: 2, b: 3, c: 1 });

            expect(diff.changedTo('a', 1)).toBeTruthy();
            expect(diff.changedTo('a', 4)).toBeFalsy();
        });

        it('Should be true if prop was changed to value1 from value2', () => {
            const diff = createDiff({ a: 1, b: 2, c: 1 }, { a: 2, b: 3, c: 1 });

            expect(diff.changedTo('a', 1, 2)).toBeTruthy();
            expect(diff.changedTo('a', 2, 3)).toBeFalsy();
            expect(diff.changedTo('a', 1, 1)).toBeFalsy();
            expect(diff.changedTo('a', 3, 1)).toBeFalsy();
        });
    });

    describe('#value', () => {
        it('Should be should equal to new value', () => {
            const diff = createDiff({ a: 1, b: 2, c: 1 }, { a: 2, b: 3, c: 1 });

            expect(diff.value('a')).toBe(1);
        });
    });

    describe('#match', () => {
        it('Should be should true if all props matched', () => {
            const diff = createDiff({ a: 1, b: 2, c: 1 }, { a: 2, b: 3, c: 1 });

            expect(diff.match({
                a: 1,
                b: 2,
            })).toBeTruthy();

            expect(diff.match({
                a: 2,
                b: 2,
            })).toBeFalsy();

            expect(diff.match({
                a: 2,
                b: 2,
            })).toBeFalsy();
        });

        it('Should be true if all props matched and one of was changed', () => {
            const diff = createDiff({ a: 1, b: 2, c: 1 }, { a: 2, b: 3, c: 1 });

            expect(diff.match({
                a: 1,
                b: [2, 3],
            }, { wasChanged: true })).toBeTruthy();

            expect(diff.match({
                a: [1, 4],
                c: 1,
            }, { wasChanged: true })).toBeTruthy();

            expect(diff.match({
                c: 1,
            }, { wasChanged: true })).toBeFalsy();
        });
    });
});
