import updateActiveOptions from '../../utils/updateActiveOptions';
import getFilteredSegmentIndices from '../../utils/getFilteredSegmentIndices';
import updateFiltering from '../../utils/updateFiltering';

jest.mock('../../filterManagerProvider');
jest.mock('../../utils/updateActiveOptions');
jest.mock('../../utils/getFilteredSegmentIndices');

const manager1 = {type: 'filter1', setFilterValue: jest.fn()};
const manager2 = {type: 'filter2', setFilterValue: jest.fn()};
const managers = [manager1, manager2];

const segments = ['segment1', 'segment2', 'segment3'];

describe('utils', () => {
    it('updateFiltering', () => {
        const updatedFiltersDataWithOptions =
            'filters-data-with-updated-active-options';

        updateActiveOptions.mockReturnValueOnce(updatedFiltersDataWithOptions);

        const updatedSegmentIndices = [false, true, false];

        getFilteredSegmentIndices.mockReturnValueOnce(updatedSegmentIndices);

        manager1.setFilterValue.mockReturnValue('new-filters-data-1');
        manager2.setFilterValue.mockReturnValue('new-filters-data-2');

        const filtering = {
            filters: {
                filter1: {value: 'value1'},
                filter2: {value: 'value2'},
            },
            filteredSegmentIndices: [true, true, true],
        };

        const newValues = {
            filter1: 'new-value1',
            filter2: 'new-value2',
        };

        const newFiltering = updateFiltering({
            managers,
            filtering,
            newValues,
            segments,
        });

        expect(newFiltering).toEqual({
            filters: updatedFiltersDataWithOptions,
            filteredSegmentIndices: updatedSegmentIndices,
        });

        expect(manager1.setFilterValue).toBeCalledWith({
            filtersData: filtering.filters,
            value: 'new-value1',
            segments,
        });
        expect(manager2.setFilterValue).toBeCalledWith({
            filtersData: 'new-filters-data-1',
            value: 'new-value2',
            segments,
        });

        expect(updateActiveOptions).toBeCalledWith({
            managers,
            segments,
            filtersData: 'new-filters-data-2',
        });
        expect(getFilteredSegmentIndices).toBeCalledWith({
            filtersData: updatedFiltersDataWithOptions,
            segments,
        });
    });
});
