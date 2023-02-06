import applyFilters from '../../utils/applyFilters';
import baseFilterManager from '../../baseFilterManager';

jest.mock('../../utils/applyFilters');

const defaultOptions3 = 'default-options-3';
const manager3 = {
    ...baseFilterManager,
    type: 'filter3',
    getDefaultOptions: jest.fn(() => defaultOptions3),
    updateOptions: jest.fn(
        (options, segment) => `${options} + ${segment.title}`,
    ),
};

const filtersData = {
    filter1: {},
    filter2: {},
    filter3: {},
};

const segments = [{title: 'segment1'}, {title: 'segment2'}];

describe('baseFilterManager', () => {
    describe('getActiveOptions', () => {
        it('all segments - filter fail', () => {
            applyFilters.mockReturnValue(false);

            const activeOptions = manager3.getActiveOptions({
                filtersData,
                segments,
            });

            expect(activeOptions).toEqual(defaultOptions3);

            expect(applyFilters.mock.calls).toEqual([
                [{segmentIndex: 0, filtersData, excludeFilterType: 'filter3'}],
                [{segmentIndex: 1, filtersData, excludeFilterType: 'filter3'}],
            ]);
            expect(manager3.getDefaultOptions).toBeCalledWith();
            expect(manager3.updateOptions).not.toBeCalled();
        });

        it('first segment - filter success, second segment - filter fail', () => {
            applyFilters.mockImplementation(
                ({segmentIndex}) => segmentIndex === 0,
            );

            const activeOptions = manager3.getActiveOptions({
                filtersData,
                segments,
            });

            expect(activeOptions).toEqual('default-options-3 + segment1');

            expect(applyFilters.mock.calls).toEqual([
                [{segmentIndex: 0, filtersData, excludeFilterType: 'filter3'}],
                [{segmentIndex: 1, filtersData, excludeFilterType: 'filter3'}],
            ]);
            expect(manager3.getDefaultOptions).toBeCalledWith();
            expect(manager3.updateOptions).toBeCalledWith(
                defaultOptions3,
                segments[0],
            );
        });

        it('all segments - filter success', () => {
            applyFilters.mockReturnValue(true);

            const activeOptions = manager3.getActiveOptions({
                filtersData,
                segments,
            });

            expect(activeOptions).toEqual(
                'default-options-3 + segment1 + segment2',
            );

            expect(applyFilters.mock.calls).toEqual([
                [{segmentIndex: 0, filtersData, excludeFilterType: 'filter3'}],
                [{segmentIndex: 1, filtersData, excludeFilterType: 'filter3'}],
            ]);
            expect(manager3.getDefaultOptions).toBeCalledWith();
            expect(manager3.updateOptions.mock.calls).toEqual([
                [defaultOptions3, segments[0]],
                ['default-options-3 + segment1', segments[1]],
            ]);
        });
    });
});
