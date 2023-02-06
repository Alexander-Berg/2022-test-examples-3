import applyFilters from '../../utils/applyFilters';
import getFilteredSegmentIndices from '../../utils/getFilteredSegmentIndices';

jest.mock('../../utils/applyFilters');

const filtersData = {
    filter1: {},
    filter2: {},
};

const segments = [
    {title: 'segment1'},
    {title: 'segment2'},
    {title: 'segment3'},
];

describe('utils', () => {
    describe('getFilteredSegmentIndices', () => {
        it('first segment - success, second - fail, third - success', () => {
            applyFilters.mockImplementation(
                ({segmentIndex}) => segmentIndex !== 1,
            );

            const indices = getFilteredSegmentIndices({filtersData, segments});

            expect(indices).toEqual([true, false, true]);

            expect(applyFilters.mock.calls).toEqual([
                [{segmentIndex: 0, filtersData}],
                [{segmentIndex: 1, filtersData}],
                [{segmentIndex: 2, filtersData}],
            ]);
        });
    });
});
