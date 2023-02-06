'use strict';

const DEFAULT_OPTIONS = 'default-options';

const manager = {
    ...require.requireActual('../../baseFilterManager').default,
    getDefaultOptions: jest.fn(() => DEFAULT_OPTIONS),
    updateOptions: jest.fn(
        (options, segment) => `${options} + ${segment.title}`,
    ),
};

describe('baseFilterManager', () => {
    describe('getOptions', () => {
        it('options by empty list of segments', () => {
            const options = manager.getOptions([]);

            expect(options).toBe(DEFAULT_OPTIONS);

            expect(manager.getDefaultOptions).toBeCalledWith();
            expect(manager.updateOptions).not.toBeCalled();
        });

        it('options by pair of segments', () => {
            const segments = [{title: 's1'}, {title: 's2'}];

            const options = manager.getOptions(segments);

            expect(options).toBe('default-options + s1 + s2');

            expect(manager.getDefaultOptions).toBeCalledWith();
            expect(manager.updateOptions.mock.calls).toEqual([
                [DEFAULT_OPTIONS, segments[0]],
                ['default-options + s1', segments[1]],
            ]);
        });
    });
});
