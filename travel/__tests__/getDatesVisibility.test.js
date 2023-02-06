jest.dontMock('../cachedSegmentTime');
const {SORTING_TYPES} = require.requireActual('../../sort/utils');
const getDatesVisibility = require.requireActual(
    '../getDatesVisibility',
).default;

const vector = [0, 1];
const context = {
    from: {
        timezone: 'Europe/Moscow',
    },
    to: {
        timezone: 'Europe/Moscow',
    },
};

describe('test getDatesVisibility function', () => {
    it('should return dates visibility for each segments', () => {
        const result = getDatesVisibility({
            vector,
            segments: [
                {
                    departure: '2016-04-18T12:59:00+00:00',
                    arrival: '2016-04-18T21:59:00+00:00',
                },
                {
                    departure: '2016-04-19T12:59:00+00:00',
                    arrival: '2016-04-19T21:59:00+00:00',
                },
            ],
            filteredSegmentIndices: [true, true],
            context,
            sort: {
                by: SORTING_TYPES.PRICE,
                reverse: false,
            },
        });

        expect(result).toEqual({
            0: {
                showDatesSeparator: false,
                showDepartureDate: true,
                showArrivalDate: true,
            },
            1: {
                showDatesSeparator: false,
                showDepartureDate: true,
                showArrivalDate: true,
            },
        });
    });

    it('should return dates visibility for first segments, and separator visibility for second segment (departure date is changed)', () => {
        const result = getDatesVisibility({
            vector,
            segments: [
                {
                    departure: '2016-04-18T12:59:00+00:00',
                    arrival: '2016-04-18T21:59:00+00:00',
                },
                {
                    departure: '2016-04-19T12:59:00+00:00',
                    arrival: '2016-04-19T21:59:00+00:00',
                },
            ],
            filteredSegmentIndices: [true, true],
            context,
            sort: {
                by: SORTING_TYPES.DEPARTURE,
                reverse: false,
            },
        });

        expect(result).toEqual({
            0: {
                showDatesSeparator: false,
                showDepartureDate: true,
                showArrivalDate: true,
            },
            1: {
                showDatesSeparator: true,
                showDepartureDate: false,
                showArrivalDate: true,
            },
        });
    });

    it('should return visibility for first segment', () => {
        const result = getDatesVisibility({
            vector,
            segments: [
                {
                    departure: '2016-04-18T12:59:00+00:00',
                    arrival: '2016-04-18T21:59:00+00:00',
                },
                {
                    departure: '2016-04-18T13:59:00+00:00',
                    arrival: '2016-04-18T22:59:00+00:00',
                },
            ],
            filteredSegmentIndices: [true, true],
            context,
            segment: {
                by: SORTING_TYPES.DEPARTURE,
                reverse: true,
            },
        });

        expect(result).toEqual({
            0: {
                showDatesSeparator: false,
                showDepartureDate: true,
                showArrivalDate: true,
            },
            1: {
                showDatesSeparator: false,
                showDepartureDate: false,
                showArrivalDate: false,
            },
        });
    });
});
