'use strict';

jest.dontMock('../../baseFilterManager');
const pricePresence = require.requireActual('../../pricePresence').default;

const segmentWithPrice = {
    title: 'Moscow - Omsk',
    tariffs: {
        classes: {
            business: '1000 roubles',
        },
    },
};

const segmentWithoutPrice = {
    title: 'Moscow - Omsk',
};

describe('pricePresence', () => {
    describe('apply', () => {
        it('show segments with price only, segment with price', () => {
            const result = pricePresence.apply(true, segmentWithPrice);

            expect(result).toBe(true);
        });

        it('show segments with price only, segment without price', () => {
            const result = pricePresence.apply(true, segmentWithoutPrice);

            expect(result).toBe(false);
        });

        it('show all segments, segment with price', () => {
            const result = pricePresence.apply(false, segmentWithPrice);

            expect(result).toBe(true);
        });

        it('show all segments, segment without price', () => {
            const result = pricePresence.apply(false, segmentWithoutPrice);

            expect(result).toBe(true);
        });
    });
});
