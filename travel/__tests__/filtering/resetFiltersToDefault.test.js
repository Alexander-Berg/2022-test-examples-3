import {resetFiltersToDefault} from '../../filtering';
import getFilterManagers from '../../filterManagerProvider';
import updateFiltering from '../../utils/updateFiltering';

jest.mock('../../filterManagerProvider');
jest.mock('../../utils/updateFiltering');

const manager1 = {type: 'filter1', getDefaultValue: jest.fn()};
const manager2 = {type: 'filter2', getDefaultValue: jest.fn()};
const manager3 = {type: 'filter3'};

const segments = ['segment1', 'segment2', 'segment3'];

describe('filtering', () => {
    it('resetFiltersToDefault', () => {
        getFilterManagers.mockReturnValueOnce([manager1, manager2, manager3]);

        manager1.getDefaultValue.mockReturnValue('default-value-1');
        manager2.getDefaultValue.mockReturnValue('default-value-2');

        updateFiltering.mockReturnValue('new-filtering');

        const filtering = {
            filters: {
                filter1: 'filter-data-1',
                filter2: 'filter-data-2',
            },
        };

        const newFiltering = resetFiltersToDefault(filtering, segments);

        expect(newFiltering).toEqual('new-filtering');

        expect(getFilterManagers).toBeCalledWith();
        expect(manager1.getDefaultValue).toBeCalledWith();
        expect(manager2.getDefaultValue).toBeCalledWith();

        expect(updateFiltering).toBeCalledWith({
            managers: [manager1, manager2],
            filtering,
            newValues: {filter1: 'default-value-1', filter2: 'default-value-2'},
            segments,
        });
    });
});
