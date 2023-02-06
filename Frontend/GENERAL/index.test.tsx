import {
    findIndexFromHead,
    findIndexFromTail,
    setChoicesSubset,
    delChoicesSubset,
    addChoicesSubset,
    hasChoicesSubset,
    findPrevPicked,
    findNextPicked,
    eventHandler,
    scrollToElem,
} from '.';

describe('ToolsSuggest.utils', () => {
    describe('scrollToElem()', () => {
        it('Should not fail on falsey elem', () => {
            expect(() => {
                scrollToElem(null);
            }).not.toThrow();
        });

        it('Should scroll to elem', () => {
            jest.useFakeTimers();
            const elem = document.createElement('div');

            elem.scrollIntoView = jest.fn();

            scrollToElem(elem);
            jest.runAllTimers();

            expect(elem.scrollIntoView).toHaveBeenCalledWith({
                behavior: 'auto',
                block: 'nearest',
                inline: 'nearest',
            });
        });
    });

    describe('eventHandler()', () => {
        it('Should not fail', () => {
            expect(() => {
                eventHandler();
            }).not.toThrow();
        });
    });

    describe('findIndexFromHead()', () => {
        it('Should find first matched choice', () => {
            expect(findIndexFromHead([{ id: '1' }, { id: '2' }, { id: '1' }, { id: '2' }], { id: '1' })).toBe(0);
        });

        it('Should support undefined choices', () => {
            expect(findIndexFromHead(undefined, { id: '1' })).toBe(-1);
        });
    });

    describe('findIndexFromTail()', () => {
        it('Should find last matched choice', () => {
            expect(findIndexFromTail([{ id: '1' }, { id: '2' }, { id: '1' }, { id: '2' }], { id: '1' })).toBe(2);
        });

        it('Should support undefined choices', () => {
            expect(findIndexFromTail(undefined, { id: '1' })).toBe(-1);
        });
    });

    describe('setChoicesSubset()', () => {
        it('Should replace choices', () => {
            expect(setChoicesSubset([{ id: '1' }, { id: '2' }], [{ id: '1' }], Infinity)).toEqual([{ id: '1' }]);
        });

        it('Should crop choices to maxChoices from head', () => {
            expect(setChoicesSubset([], [{ id: '1' }, { id: '2' }], 1)).toEqual([{ id: '2' }]);
        });

        it('Should support corner cases for maxChoices', () => {
            expect(setChoicesSubset([], [{ id: '1' }, { id: '2' }], 0)).toEqual([]);
            expect(setChoicesSubset([], [{ id: '1' }, { id: '2' }], -1)).toEqual([]);
        });

        it('Should remove duplicates', () => {
            expect(setChoicesSubset([], [{ id: '1' }, { id: '2' }, { id: '1' }], Infinity)).toEqual([
                { id: '1' },
                { id: '2' },
            ]);
        });

        it('Should not modify object of nothing changed', () => {
            const choices = [{ id: '1' }, { id: '2' }];

            const subset = [{ id: '1' }, { id: '2' }];

            const actual = setChoicesSubset(choices, subset, Infinity);

            expect(actual).toEqual(subset);
            expect(actual).toBe(choices);
        });
    });

    describe('hasChoicesSubset()', () => {
        it('Should check if subset exists in choices', () => {
            expect(hasChoicesSubset([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '1' }, { id: '3' }])).toBe(true);
        });

        it('Should check if every choice from subset exists in choices', () => {
            expect(
                hasChoicesSubset([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '1' }, { id: '3' }, { id: '4' }]),
            ).toBe(false);
        });
    });

    describe('delChoicesSubset()', () => {
        it('Should remove subset', () => {
            expect(delChoicesSubset([{ id: '1' }, { id: '2' }], [{ id: '1' }], Infinity)).toEqual([{ id: '2' }]);
        });

        it('Should remove all', () => {
            expect(delChoicesSubset([{ id: '1' }, { id: '2' }], [{ id: '1' }, { id: '2' }], Infinity)).toEqual([]);
        });

        it('Should ignore non existing choices', () => {
            expect(delChoicesSubset([{ id: '1' }, { id: '2' }], [{ id: '3' }], Infinity)).toEqual([
                { id: '1' },
                { id: '2' },
            ]);
        });

        it('Should remove duplicates', () => {
            expect(delChoicesSubset([{ id: '1' }, { id: '1' }, { id: '2' }], [{ id: '1' }], Infinity)).toEqual([
                { id: '2' },
            ]);
        });

        it('Should always remove duplicates', () => {
            expect(delChoicesSubset([{ id: '1' }, { id: '1' }, { id: '2' }], [], Infinity)).toEqual([
                { id: '1' },
                { id: '2' },
            ]);
        });

        it('Should not modify choices if nothing changes', () => {
            const actual = delChoicesSubset([{ id: '1' }], [], Infinity);

            expect(actual).toEqual([{ id: '1' }]);

            expect(actual).toBe(actual);
        });

        it('Should also always crop choices to maxChoices from head', () => {
            expect(delChoicesSubset([{ id: '1' }, { id: '2' }, { id: '3' }], [], 1)).toEqual([{ id: '3' }]);
        });
    });

    describe('addChoicesSubset()', () => {
        it('Should add choices to subset', () => {
            expect(addChoicesSubset([], [{ id: '1' }, { id: '2' }], Infinity)).toEqual([{ id: '1' }, { id: '2' }]);
        });

        it('Should remove duplicates', () => {
            expect(addChoicesSubset([], [{ id: '1' }, { id: '2' }, { id: '1' }], Infinity)).toEqual([
                { id: '1' },
                { id: '2' },
            ]);
        });

        it('Should always remove duplicates', () => {
            expect(addChoicesSubset([{ id: '1' }, { id: '2' }, { id: '1' }], [], Infinity)).toEqual([
                { id: '1' },
                { id: '2' },
            ]);
        });

        it('Should crop choices to maxChoices from head', () => {
            expect(addChoicesSubset([], [{ id: '1' }, { id: '2' }, { id: '3' }], 1)).toEqual([{ id: '3' }]);
        });

        it('Should always crop choices to maxChoices from head', () => {
            expect(addChoicesSubset([{ id: '1' }, { id: '2' }, { id: '3' }], [], 1)).toEqual([{ id: '3' }]);
        });

        it('Should not modify choices if nothing changes', () => {
            const choices = [{ id: '1' }];

            const actual = addChoicesSubset(choices, [], Infinity);

            expect(actual).toEqual(choices);
            expect(actual).toBe(choices);
        });
    });

    describe('findPrevPicked()', () => {
        it('Should support undefined choices', () => {
            expect(findPrevPicked(undefined, [], false)).toEqual([]);
        });

        it('Should find prev picked choices', () => {
            expect(findPrevPicked([{ id: '1' }, { id: '2' }], [{ id: '2' }], false)).toEqual([{ id: '1' }]);
        });

        it('Should move from last picked choice', () => {
            expect(
                findPrevPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '1' }, { id: '2' }, { id: '3' }], false),
            ).toEqual([{ id: '2' }]);
        });

        it('Should support unsorted picked', () => {
            expect(
                findPrevPicked(
                    [{ id: '1' }, { id: '2' }, { id: '3' }, { id: '4' }],
                    [{ id: '4' }, { id: '2' }, { id: '3' }],
                    false,
                ),
            ).toEqual([{ id: '2' }]);
        });

        it('Should support top unsorted picked', () => {
            expect(
                findPrevPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '3' }, { id: '1' }, { id: '2' }], false),
            ).toEqual([{ id: '1' }]);
        });

        it('Should support top unsorted corner picked', () => {
            expect(
                findPrevPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '3' }, { id: '2' }, { id: '1' }], false),
            ).toEqual([]);
        });

        it('Should support empty picked', () => {
            expect(findPrevPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [], false)).toEqual([{ id: '3' }]);
        });

        it('Should support top corner', () => {
            expect(findPrevPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '1' }], false)).toEqual([]);
        });

        it('Should support appending', () => {
            expect(findPrevPicked([{ id: '1' }, { id: '2' }], [{ id: '2' }], true)).toEqual([{ id: '2' }, { id: '1' }]);
        });

        it('Should support top corner appending', () => {
            expect(findPrevPicked([{ id: '1' }, { id: '2' }], [{ id: '2' }, { id: '1' }], true)).toEqual([
                { id: '2' },
                { id: '1' },
            ]);
        });

        it('Should support range reducing', () => {
            expect(
                findPrevPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '1' }, { id: '2' }, { id: '3' }], true),
            ).toEqual([{ id: '1' }, { id: '2' }]);
        });

        it('Should support range filling', () => {
            expect(
                findPrevPicked(
                    [{ id: '1' }, { id: '2' }, { id: '3' }, { id: '4' }],
                    [{ id: '1' }, { id: '4' }, { id: '3' }],
                    true,
                ),
            ).toEqual([{ id: '1' }, { id: '4' }, { id: '3' }, { id: '2' }]);
        });

        it('Should support range skip filling', () => {
            expect(
                findPrevPicked(
                    [{ id: '1' }, { id: '2' }, { id: '3' }, { id: '4' }, { id: '5' }],
                    [{ id: '1' }, { id: '5' }, { id: '3' }, { id: '4' }],
                    true,
                ),
            ).toEqual([{ id: '1' }, { id: '5' }, { id: '3' }, { id: '4' }, { id: '2' }]);
        });
    });

    describe('findNextPicked()', () => {
        it('Should support undefined choices', () => {
            expect(findNextPicked(undefined, [], false)).toEqual([]);
        });

        it('Should find next picked choices', () => {
            expect(findNextPicked([{ id: '1' }, { id: '2' }], [{ id: '1' }], false)).toEqual([{ id: '2' }]);
        });

        it('Should move from last picked choice', () => {
            expect(
                findNextPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '3' }, { id: '2' }, { id: '1' }], false),
            ).toEqual([{ id: '2' }]);
        });

        it('Should support unsorted picked', () => {
            expect(
                findNextPicked(
                    [{ id: '1' }, { id: '2' }, { id: '3' }, { id: '4' }],
                    [{ id: '1' }, { id: '3' }, { id: '2' }],
                    false,
                ),
            ).toEqual([{ id: '3' }]);
        });

        it('Should support bottom unsorted picked', () => {
            expect(
                findNextPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '1' }, { id: '3' }, { id: '2' }], false),
            ).toEqual([{ id: '3' }]);
        });

        it('Should support bottom unsorted corner picked', () => {
            expect(
                findNextPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '1' }, { id: '2' }, { id: '3' }], false),
            ).toEqual([]);
        });

        it('Should support empty picked', () => {
            expect(findNextPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [], false)).toEqual([{ id: '1' }]);
        });

        it('Should support bottom corner', () => {
            expect(findNextPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '3' }], false)).toEqual([]);
        });

        it('Should support appending', () => {
            expect(findNextPicked([{ id: '1' }, { id: '2' }], [{ id: '1' }], true)).toEqual([{ id: '1' }, { id: '2' }]);
        });

        it('Should support bottom corner appending', () => {
            expect(findNextPicked([{ id: '1' }, { id: '2' }], [{ id: '1' }, { id: '2' }], true)).toEqual([
                { id: '1' },
                { id: '2' },
            ]);
        });

        it('Should support range reducing', () => {
            expect(
                findNextPicked([{ id: '1' }, { id: '2' }, { id: '3' }], [{ id: '3' }, { id: '2' }, { id: '1' }], true),
            ).toEqual([{ id: '3' }, { id: '2' }]);
        });

        it('Should support range filling', () => {
            expect(
                findNextPicked(
                    [{ id: '1' }, { id: '2' }, { id: '3' }, { id: '4' }],
                    [{ id: '4' }, { id: '1' }, { id: '2' }],
                    true,
                ),
            ).toEqual([{ id: '4' }, { id: '1' }, { id: '2' }, { id: '3' }]);
        });

        it('Should support range skip filling', () => {
            expect(
                findNextPicked(
                    [{ id: '1' }, { id: '2' }, { id: '3' }, { id: '4' }, { id: '5' }],
                    [{ id: '5' }, { id: '1' }, { id: '3' }, { id: '2' }],
                    true,
                ),
            ).toEqual([{ id: '5' }, { id: '1' }, { id: '3' }, { id: '2' }, { id: '4' }]);
        });
    });
});
