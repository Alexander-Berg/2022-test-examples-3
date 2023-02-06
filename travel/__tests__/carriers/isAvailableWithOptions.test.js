'use strict';

jest.dontMock('../../baseFilterManager');
const carriers = require.requireActual('../../carriers').default;

describe('carriers', () => {
    describe('isAvailableWithOptions', () => {
        it('no options', () => {
            const available = carriers.isAvailableWithOptions([]);

            expect(available).toBe(false);
        });

        it('single option', () => {
            const options = [{id: '123', title: 'Aeroflot'}];
            const available = carriers.isAvailableWithOptions(options);

            expect(available).toBe(false);
        });

        it('two options', () => {
            const options = [
                {id: '123', title: 'Aeroflot'},
                {id: '456', title: 'S7'},
            ];
            const available = carriers.isAvailableWithOptions(options);

            expect(available).toBe(true);
        });
    });
});
