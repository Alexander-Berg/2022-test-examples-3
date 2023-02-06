const gone = require.requireActual('../../gone').default;

const goneSegment = {isGone: true};
const upcomingSegment = {isGone: false};

describe('testing gone filter', () => {
    it('show gone segments, gone segment in parameters', () => {
        const result = gone.apply(true, goneSegment);

        expect(result).toBe(true);
    });

    it('show gone segments, upcoming segment in parameters', () => {
        const result = gone.apply(true, upcomingSegment);

        expect(result).toBe(true);
    });

    it('hide gone segments, upcoming segments in parameters', () => {
        const result = gone.apply(false, upcomingSegment);

        expect(result).toBe(true);
    });

    it('hide gone segments, gone segment in parameters', () => {
        const result = gone.apply(false, goneSegment);

        expect(result).toBe(false);
    });
});
