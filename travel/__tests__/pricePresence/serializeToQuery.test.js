'use strict';

jest.dontMock('../../baseFilterManager');
const pricePresence = require.requireActual('../../pricePresence').default;

describe('pricePresence', () => {
    describe('serializeToQuery', () => {
        it('show segments with price only', () => {
            const query = pricePresence.serializeToQuery(true);

            expect(query).toEqual({seats: 'y'});
        });

        it('show all segments', () => {
            const query = pricePresence.serializeToQuery(false);

            expect(query).toEqual({seats: ''});
        });
    });
});
