const gone = require.requireActual('../../gone').default;

const goneSegment = {isGone: true};
const upcomingSegment = {isGone: false};

describe('testing gone filter', () => {
    describe('updateOptions', () => {
        const initialOptionState = {
            isGone: 0,
            isNotGoneYet: 0,
        };

        it('update options by gone segment', () => {
            const result = gone.updateOptions(initialOptionState, goneSegment);

            expect(result).toEqual({
                isGone: 1,
                isNotGoneYet: 0,
            });
        });

        it('update options by upcoming segment', () => {
            const result = gone.updateOptions(
                initialOptionState,
                upcomingSegment,
            );

            expect(result).toEqual({
                isGone: 0,
                isNotGoneYet: 1,
            });
        });
    });
});
