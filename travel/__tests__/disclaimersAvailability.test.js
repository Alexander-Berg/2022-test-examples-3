jest.dontMock('../../transportType');
const {
    trainIncompleteScheduleDisclaimerIsAvailable,
    belarusSearchDisclaimerIsAvailable,
} = require.requireActual('../disclaimersAvailability');

const {TRAIN_TYPE, BUS_TYPE} = require.requireActual('../../transportType');

describe('disclaimersAvailability', () => {
    describe('trainIncompleteScheduleDisclaimerIsAvailable', () => {
        it('should return false for thread in our countries', () => {
            const context = {
                from: {
                    country: {
                        code: 'RU',
                    },
                },
                to: {
                    country: {
                        code: 'BY',
                    },
                },
            };
            const segments = [
                {
                    transport: {
                        code: TRAIN_TYPE,
                    },
                },
            ];

            const result = trainIncompleteScheduleDisclaimerIsAvailable(
                context,
                segments,
            );

            expect(result).toBe(false);
        });

        it('should return true for thread in other countries', () => {
            const context = {
                from: {
                    country: {
                        code: 'FR',
                    },
                },
                to: {
                    country: {
                        code: 'GB',
                    },
                },
            };
            const segments = [
                {
                    transport: {
                        code: TRAIN_TYPE,
                    },
                },
            ];

            const result = trainIncompleteScheduleDisclaimerIsAvailable(
                context,
                segments,
            );

            expect(result).toBe(true);
        });

        it('should return false for segments without train', () => {
            const context = {
                from: {
                    country: {
                        code: 'FR',
                    },
                },
                to: {
                    country: {
                        code: 'GB',
                    },
                },
            };
            const segments = [
                {
                    transport: {
                        code: BUS_TYPE,
                    },
                },
            ];

            const result = trainIncompleteScheduleDisclaimerIsAvailable(
                context,
                segments,
            );

            expect(result).toBe(false);
        });

        it('should return false for empty segments', () => {
            const context = {
                from: {
                    country: {
                        code: 'FR',
                    },
                },
                to: {
                    country: {
                        code: 'GB',
                    },
                },
            };

            const result = trainIncompleteScheduleDisclaimerIsAvailable(
                context,
                [],
            );

            expect(result).toBe(false);
        });
    });

    describe('belarusSearchDisclaimerIsAvailable', () => {
        it('should return false for all national versions except "by"', () => {
            const context = {
                from: {
                    country: {
                        code: 'BY',
                    },
                },
                to: {
                    country: {
                        code: 'BY',
                    },
                },
            };
            const tld = 'ru';

            const result = belarusSearchDisclaimerIsAvailable(context, tld);

            expect(result).toBe(false);
        });

        it('should return true for "by" national version if thread points are in the country', () => {
            const context = {
                from: {
                    country: {
                        code: 'BY',
                    },
                },
                to: {
                    country: {
                        code: 'BY',
                    },
                },
            };
            const tld = 'by';

            const result = belarusSearchDisclaimerIsAvailable(context, tld);

            expect(result).toBe(true);
        });

        it('should return false for "by" national version if thread points are not in the country', () => {
            const context = {
                from: {
                    country: {
                        code: 'RU',
                    },
                },
                to: {
                    country: {
                        code: 'BY',
                    },
                },
            };
            const tld = 'by';

            const result = belarusSearchDisclaimerIsAvailable(context, tld);

            expect(result).toBe(false);
        });
    });
});
