const gone = require.requireActual('../../gone').default;

describe('testing gone filter', () => {
    describe('isAvailableForContext', () => {
        const time = {
            // 2016-04-19T12:00:00+05:00
            now: 1461049200000,
            timezone: 'Asia/Yekaterinburg',
        };
        const presentContext = {
            when: {date: '2016-04-19'},
            time,
        };
        const futureContext = {
            when: {date: '2016-04-20'},
            time,
        };
        const allDaysContext = {
            when: {special: 'all-days'},
            time,
        };

        it('searching in all days', () => {
            const result = gone.isAvailableForContext(allDaysContext);

            expect(result).toBe(false);
        });

        it('searching in the future', () => {
            const result = gone.isAvailableForContext(futureContext);

            expect(result).toBe(false);
        });

        it('searching in the present', () => {
            const result = gone.isAvailableForContext(presentContext);

            expect(result).toBe(true);
        });
    });
});
