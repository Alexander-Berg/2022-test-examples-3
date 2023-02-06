import {getFilteringQuery} from '../../filtering';
import getFilterManagers from '../../filterManagerProvider';

jest.mock('../../filterManagerProvider');

const manager1 = {
    type: 'filter1',
    serializeToQuery: jest.fn(() => ({f1: 'val1'})),
};
const manager2 = {
    type: 'filter2',
    serializeToQuery: jest.fn(() => ({f2: 'val2'})),
};
const manager3 = {
    type: 'filter3',
    serializeToQuery: jest.fn(() => ({f3: 'val3'})),
};

describe('filtering', () => {
    it('getFilteringQuery', () => {
        getFilterManagers.mockReturnValue([manager1, manager2, manager3]);

        const filtering = {
            filters: {
                filter1: {value: 'value1'},
                filter2: {value: 'value2'},
            },
        };

        const query = getFilteringQuery(filtering);

        expect(query).toEqual({
            f1: 'val1',
            f2: 'val2',
        });

        expect(getFilterManagers).toBeCalledWith();
        expect(manager1.serializeToQuery).toBeCalledWith('value1');
        expect(manager2.serializeToQuery).toBeCalledWith('value2');
    });
});
