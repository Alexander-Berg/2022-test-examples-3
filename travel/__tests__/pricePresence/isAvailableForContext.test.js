'use strict';

jest.dontMock('../../baseFilterManager');
const pricePresence = require.requireActual('../../pricePresence').default;

describe('pricePresence', () => {
    describe('isAvailableForContext', () => {
        it('search for all days', () => {
            const searchContext = {
                when: {
                    special: 'all-days',
                },
                searchForPastDate: false,
            };
            const available =
                pricePresence.isAvailableForContext(searchContext);

            expect(available).toBe(true);
        });

        it('search for future date', () => {
            const searchContext = {
                when: {
                    date: '2016-04-16',
                },
                searchForPastDate: false,
            };
            const available =
                pricePresence.isAvailableForContext(searchContext);

            expect(available).toBe(true);
        });

        it('search for past date', () => {
            const searchContext = {
                when: {
                    date: '2016-04-16',
                },
                searchForPastDate: true,
            };
            const available =
                pricePresence.isAvailableForContext(searchContext);

            expect(available).toBe(false);
        });
    });
});
