'use strict';

const DEFAULT_VALUE = 'default-value';

const manager = {
    ...require.requireActual('../../baseFilterManager').default,
    type: 'test-filter',
    getDefaultValue: jest.fn(() => DEFAULT_VALUE),
    getOptions: jest.fn(() => 'initialized-options'),
    isAvailableWithOptions: jest.fn(() => 'available-with-options'),
};

const segments = [{title: 'segment 1'}, {title: 'segment 2'}];

describe('baseFilterManager', () => {
    it('initFilterData', () => {
        const filterData = manager.initFilterData(segments);

        expect(filterData).toEqual({
            value: DEFAULT_VALUE,
            options: 'initialized-options',
            activeOptions: 'initialized-options',
            availableWithOptions: 'available-with-options',
            availableWithActiveOptions: 'available-with-options',
            type: 'test-filter',
            filteredSegmentIndices: [true, true],
        });

        expect(manager.getDefaultValue).toBeCalledWith();
        expect(manager.getOptions).toBeCalledWith(segments);
        expect(manager.isAvailableWithOptions).toBeCalledWith(
            'initialized-options',
        );
    });
});
