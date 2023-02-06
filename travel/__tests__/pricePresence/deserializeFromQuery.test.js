'use strict';

jest.dontMock('../../baseFilterManager');
const pricePresence = require.requireActual('../../pricePresence').default;

describe('pricePresence', () => {
    describe('deserializeFromQuery', () => {
        it('seats: y', () => {
            const value = pricePresence.deserializeFromQuery({seats: 'y'});

            expect(value).toBe(true);
        });

        it('seats: Y', () => {
            const value = pricePresence.deserializeFromQuery({seats: 'Y'});

            expect(value).toBe(true);
        });

        it('seats: unknown', () => {
            const value = pricePresence.deserializeFromQuery({
                seats: 'unknown',
            });

            expect(value).toBe(false);
        });

        it('no "seats" param', () => {
            const value = pricePresence.deserializeFromQuery({foo: 'bar'});

            expect(value).toBe(false);
        });
    });
});
