'use strict';

jest.dontMock('../../baseFilterManager');
const carriers = require.requireActual('../../carriers').default;

describe('carriers', () => {
    describe('isAvailableForContext', () => {
        it('search by all transport types', () => {
            const context = {transportType: 'all'};

            const available = carriers.isAvailableForContext(context);

            expect(available).toBe(true);
        });

        it('search by plane only', () => {
            const context = {transportType: 'plane'};

            const available = carriers.isAvailableForContext(context);

            expect(available).toBe(true);
        });

        it('search by train only', () => {
            const context = {transportType: 'train'};

            const available = carriers.isAvailableForContext(context);

            expect(available).toBe(false);
        });
    });
});
