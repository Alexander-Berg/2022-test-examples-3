'use strict';

const manager = {
    ...require.requireActual('../../baseFilterManager').default,
    apply: jest.fn(),
    shouldApply: jest.fn(),
};

const segment1 = {
    title: 'segment1',
};

const segment2 = {
    title: 'segment2',
};

const filterValue = 'filter-value';

describe('baseFilterManager', () => {
    describe('getFilteredSegmentIndices', () => {
        it('default value', () => {
            manager.shouldApply.mockReturnValue(false);

            const indices = manager.getFilteredSegmentIndices(filterValue, [
                segment1,
                segment2,
            ]);

            expect(indices).toEqual([true, true]);
            expect(manager.shouldApply).toBeCalledWith(filterValue);
            expect(manager.apply).not.toBeCalled();
        });

        it('not default value, first segment - success, second segment - fail', () => {
            manager.shouldApply.mockReturnValue(true);
            manager.apply.mockImplementation(
                (val, segment) => segment === segment1,
            );

            const indices = manager.getFilteredSegmentIndices(filterValue, [
                segment1,
                segment2,
            ]);

            expect(indices).toEqual([true, false]);
            expect(manager.shouldApply).toBeCalledWith(filterValue);
            expect(manager.apply.mock.calls).toEqual([
                [filterValue, segment1],
                [filterValue, segment2],
            ]);
        });
    });
});
