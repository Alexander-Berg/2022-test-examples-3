'use strict';

jest.dontMock('../../baseFilterManager');
const pricePresence = require.requireActual('../../pricePresence').default;

describe('pricePresence', () => {
    describe('isDefaultValue', () => {
        it('show segments with price only', () => {
            const result = pricePresence.isDefaultValue(true);

            expect(result).toBe(false);
        });

        it('show all segments', () => {
            const result = pricePresence.isDefaultValue(false);

            expect(result).toBe(true);
        });
    });
});
