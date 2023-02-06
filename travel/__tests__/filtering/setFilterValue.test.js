import getFilterManagers from '../../filterManagerProvider';
import updateFiltering from '../../utils/updateFiltering';
import {setFilterValue} from '../../filtering';

jest.mock('../../filterManagerProvider');
jest.mock('../../utils/updateFiltering');

const manager1 = {type: 'filter1'};
const manager2 = {type: 'filter2'};
const manager3 = {type: 'filter3'};

const segments = ['segment1', 'segment2', 'segment3'];

describe('filtering', () => {
    it('setFilterValue', () => {
        getFilterManagers.mockReturnValue([manager1, manager2, manager3]);

        updateFiltering.mockReturnValue('new-filtering');

        const filtering = {
            filters: {
                filter1: 'fitler-data-1',
                filter2: 'fitler-data-2',
            },
        };

        const value = 'val';

        const newFiltering = setFilterValue({
            filtering,
            filterType: 'filter1',
            value,
            segments,
        });

        expect(newFiltering).toBe('new-filtering');

        expect(getFilterManagers).toBeCalledWith();
        expect(updateFiltering).toBeCalledWith({
            managers: [manager1, manager2],
            filtering,
            newValues: {filter1: value},
            segments,
        });
    });
});
