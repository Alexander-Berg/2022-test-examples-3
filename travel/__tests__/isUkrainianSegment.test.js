const isUkrainianSegment = require.requireActual(
    '../isUkrainianSegment',
).default;

jest.unmock('../../countries');

const segment = {
    stationFrom: {
        country: null,
    },
    stationTo: {
        country: null,
    },
};

describe('isUkrainianSegment', () => {
    it('should return false without countries info in stationFrom and stationTo objects', () => {
        expect(isUkrainianSegment(segment)).toBe(false);
    });

    it('should return true to ukrainian segment', () => {
        segment.stationFrom.country = {code: 'RU'};
        segment.stationTo.country = {code: 'UA'};

        expect(isUkrainianSegment(segment)).toBe(true);
    });

    it('should return false to non ukrainian segment', () => {
        segment.stationFrom.country = {code: 'RU'};
        segment.stationTo.country = {code: 'RU'};

        expect(isUkrainianSegment(segment)).toBe(false);
    });
});
