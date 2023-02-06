jest.dontMock('../../date/mask');

const mergeSegments = require.requireActual('../mergeSegments').default;
const {TRAIN_TYPE, BUS_TYPE} = require.requireActual('../../transportType');

const baseSegment = {
    title: 'A - B',
    number: '1',
    stops: '',
    departure: '2016-08-29T21:22:00+00:00',
    runDays: {
        2016: {
            9: Array.from({length: 31}, () => 1),
        },
    },
    thread: {
        uid: 'uid-1',
    },
    stationFrom: {
        platform: '1',
    },
    stationTo: {
        platform: '2',
    },
    transport: {
        code: TRAIN_TYPE,
    },
};

describe('mergeSegments', () => {
    it('should return segments array with same length: departure', () => {
        const segments = [
            baseSegment,
            {
                ...baseSegment,
                departure: '2014-07-29T21:21:00+00:00',
            },
        ];

        expect(mergeSegments(segments).length).toEqual(segments.length);
    });

    it('should return segments array with same length: stops', () => {
        const segments = [
            baseSegment,
            {
                ...baseSegment,
                stops: 'all',
            },
        ];

        expect(mergeSegments(segments).length).toEqual(segments.length);
    });

    it('should return segments array with same length: title', () => {
        const segments = [
            baseSegment,
            {
                ...baseSegment,
                title: 'A - C',
            },
        ];

        expect(mergeSegments(segments).length).toEqual(segments.length);
    });

    it('should return segments array with same length: number', () => {
        const segments = [
            baseSegment,
            {
                ...baseSegment,
                number: '13',
            },
        ];

        expect(mergeSegments(segments).length).toEqual(segments.length);
    });

    it('should return segments array with same length: transportTypes', () => {
        const segments = [
            baseSegment,
            {
                ...baseSegment,
                transport: {
                    code: BUS_TYPE,
                },
            },
        ];

        expect(mergeSegments(segments).length).toEqual(segments.length);
    });

    it('should not merge intervals segments', () => {
        const segment = {
            ...baseSegment,
            isInterval: true,
        };
        const segments = [segment, segment];

        expect(mergeSegments(segments).length).toEqual(segments.length);
    });

    it('should not merge transfers segments', () => {
        const segment = {
            ...baseSegment,
            isTransfer: true,
        };
        const segments = [segment, segment];

        expect(mergeSegments(segments).length).toEqual(segments.length);
    });

    it('should merge segments with different dates of departure but with same time', () => {
        const segments = [
            baseSegment,
            {
                ...baseSegment,
                departure: '2016-07-29T21:22:00+00:00',
            },
            {
                ...baseSegment,
                departure: '2016-08-29T21:22:00+00:00',
            },
        ];

        expect(mergeSegments(segments).length).toEqual(1);
    });

    it('should not modify platform if platforms are same in merged segments', () => {
        const segments = [baseSegment, baseSegment];
        const result = mergeSegments(segments)[0];

        expect(result.stationFrom.platform).toEqual(
            baseSegment.stationFrom.platform,
        );
        expect(result.stationTo.platform).toEqual(
            baseSegment.stationTo.platform,
        );
    });

    it('should set isMerged for merged segments', () => {
        const segments = [
            baseSegment,
            baseSegment,
            {
                ...baseSegment,
                title: 'D - E',
            },
        ];
        const result = mergeSegments(segments);

        expect(result.map(({isMerged}) => isMerged)).toEqual([true, false]);
    });

    it('should replace flag in "runDays" with thread uid', () => {
        const segments = [baseSegment];
        const result = mergeSegments(segments)[0];

        expect(result.runDays).toEqual({
            2016: {
                9: Array.from({length: 31}, () => baseSegment.thread.uid),
            },
        });
    });

    it('should return merged runDays', () => {
        const duplicate = {
            ...baseSegment,
            runDays: {
                2016: {
                    10: Array.from({length: 31}, () => 1),
                },
            },
            thread: {
                uid: 'uid-2',
            },
        };
        const segments = [baseSegment, duplicate];
        const result = mergeSegments(segments)[0];

        expect(result.runDays).toEqual({
            2016: {
                9: Array.from({length: 31}, () => baseSegment.thread.uid),
                10: Array.from({length: 31}, () => duplicate.thread.uid),
            },
        });
    });
});
