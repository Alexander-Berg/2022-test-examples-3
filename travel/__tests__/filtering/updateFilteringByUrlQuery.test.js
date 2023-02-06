import getFilterManagers from '../../filterManagerProvider';
import updateFiltering from '../../utils/updateFiltering';
import {updateFilteringByUrlQuery} from '../../filtering';

jest.mock('../../filterManagerProvider');
jest.mock('../../utils/updateFiltering');

const manager1 = {type: 'filter1', deserializeFromQuery: jest.fn()};
const manager2 = {type: 'filter2', deserializeFromQuery: jest.fn()};
const manager3 = {type: 'filter3'};

const segments = ['segment1', 'segment2', 'segment3'];

describe('filtering', () => {
    it('updateFilteringByUrlQuery', () => {
        getFilterManagers.mockReturnValueOnce([manager1, manager2, manager3]);

        manager1.deserializeFromQuery.mockReturnValue('query-value-1');
        manager2.deserializeFromQuery.mockReturnValue('query-value-2');

        updateFiltering.mockReturnValue('new-filtering');

        const filtering = {
            filters: {
                filter1: 'filter-data-1',
                filter2: 'filter-data-2',
            },
        };
        const query = 'query';

        const newFiltering = updateFilteringByUrlQuery({
            filtering,
            query,
            segments,
        });

        expect(newFiltering).toEqual('new-filtering');

        expect(getFilterManagers).toBeCalledWith();
        expect(manager1.deserializeFromQuery).toBeCalledWith(query);
        expect(manager2.deserializeFromQuery).toBeCalledWith(query);

        expect(updateFiltering).toBeCalledWith({
            managers: [manager1, manager2],
            filtering,
            newValues: {filter1: 'query-value-1', filter2: 'query-value-2'},
            segments,
        });
    });
});
