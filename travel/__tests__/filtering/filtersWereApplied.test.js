import getFilterManagers from '../../filterManagerProvider';
import {filtersWereApplied} from '../../filtering';

const transportManager = {
    type: 'transport',
    isDefaultValue: value => value.length === 0,
};

jest.mock('../../filterManagerProvider');

describe('filtering', () => {
    describe('filtersWereApplied', () => {
        it('should return false', () => {
            getFilterManagers.mockReturnValue([transportManager]);
            const filtering = {
                filters: {
                    transport: {
                        value: ['plane', 'train'],
                    },
                },
            };
            const result = filtersWereApplied(filtering);

            expect(result).toBe(true);
            expect(getFilterManagers).toBeCalledWith();
        });

        it('should return true', () => {
            getFilterManagers.mockReturnValue([transportManager]);
            const filtering = {
                filters: {
                    transport: {
                        value: [],
                    },
                },
            };
            const result = filtersWereApplied(filtering);

            expect(result).toBe(false);
            expect(getFilterManagers).toBeCalledWith();
        });
    });
});
