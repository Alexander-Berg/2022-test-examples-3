import highspeed from '../../highSpeedTrain';

describe('highspeed filter test', () => {
    describe('apply method', () => {
        const highSpeedSegment = {
            transport: {
                code: 'train',
            },
            thread: {
                deluxeTrain: {
                    id: '1',
                    title: 'Flash',
                    isHighSpeed: true,
                },
            },
        };
        const slowpokeSegment = {
            transport: {
                code: 'train',
            },
            thread: {
                deluxeTrain: {
                    id: '2',
                    title: 'Slowpoke',
                    isHighSpeed: false,
                },
            },
        };
        const bus = {
            transport: {
                code: 'bus',
            },
            thread: {},
        };
        const segmentWithNullThread = {
            transport: {
                code: 'train',
            },
            thread: null,
        };

        it('is highspeed segment on empty array', () => {
            const value = [];
            const result = highspeed.apply(value, highSpeedSegment);

            expect(result).toBe(false);
        });

        it('is slowpoke segment on empty array', () => {
            const value = [];
            const result = highspeed.apply(value, slowpokeSegment);

            expect(result).toBe(false);
        });

        it('is highspeed segment', () => {
            const value = ['1', '2'];
            const result = highspeed.apply(value, highSpeedSegment);

            expect(result).toBe(true);
        });

        it('is slowpoke segment', () => {
            const value = ['1', '2'];
            const result = highspeed.apply(value, slowpokeSegment);

            expect(result).toBe(false);
        });

        it('is bus segment', () => {
            const value = ['1', '2'];
            const result = highspeed.apply(value, bus);

            expect(result).toBe(false);
        });

        it('should return false on segment with null thread field', () => {
            const value = ['1', '2'];
            const result = highspeed.apply(value, segmentWithNullThread);

            expect(result).toBe(false);
        });
    });
});
