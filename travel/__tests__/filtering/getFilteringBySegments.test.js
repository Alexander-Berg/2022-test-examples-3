import getFilterManagers from '../../filterManagerProvider';
import {getFilteringBySegments} from '../../filtering';

jest.mock('../../filterManagerProvider');

const context = {from: 'from', to: 'to'};

const manager1 = {
    type: 'filter1',
    isAvailableForContext: jest.fn(() => true),
    initFilterData: jest.fn(() => 'filter-data-1'),
};
const manager2 = {type: 'filter2', isAvailableForContext: jest.fn(() => false)};

const segments = ['segment1', 'segment2', 'segment3'];

const isSuburbanSearchResult = false;

describe('filtering', () => {
    it('getFilteringBySegments', () => {
        getFilterManagers.mockReturnValueOnce([manager1, manager2]);

        const filtering = getFilteringBySegments({
            context,
            segments,
            isSuburbanSearchResult,
        });

        expect(filtering).toEqual({
            filters: {
                filter1: 'filter-data-1',
            },
            filteredSegmentIndices: [true, true, true],
        });

        expect(getFilterManagers).toBeCalledWith();
        expect(manager1.isAvailableForContext).toBeCalledWith(context, {
            isSuburbanSearchResult,
        });
        expect(manager1.initFilterData).toBeCalledWith(segments);
        expect(manager2.isAvailableForContext).toBeCalledWith(context, {
            isSuburbanSearchResult,
        });
    });
});
