jest.dontMock('../../transportType');

const {BUS_TYPE, SUBURBAN_TYPE} = require.requireActual('../../transportType');
const {excludeSuburbanSegmentsOfWrongPlan} =
    require.requireActual('../suburbanPlans');

const busSegment = {
    transport: {code: BUS_TYPE},
};

const suburbanSegmentOfCurrentPlan = {
    transport: {code: SUBURBAN_TYPE},
    thread: {schedulePlanCode: 'g16'},
};

const suburbanSegmentOfNextPlan = {
    transport: {code: SUBURBAN_TYPE},
    thread: {schedulePlanCode: 'g17'},
};

const suburbanSegmentWithoutPlan = {
    transport: {code: SUBURBAN_TYPE},
    thread: {schedulePlanCode: null},
};

const segments = [
    busSegment,
    suburbanSegmentOfCurrentPlan,
    suburbanSegmentOfNextPlan,
    suburbanSegmentWithoutPlan,
];

const plans = {
    current: {
        code: 'g16',
    },
    next: {
        code: 'g17',
    },
};

describe('excludeSuburbanSegmentsOfWrongPlan', () => {
    it('no current plan', () => {
        const filteredSegments = excludeSuburbanSegmentsOfWrongPlan(
            {segments, plans: {}},
            null,
        );

        expect(filteredSegments).toEqual(segments);
    });

    it('no plan in context', () => {
        const filteredSegments = excludeSuburbanSegmentsOfWrongPlan(
            {segments, plans},
            null,
        );

        expect(filteredSegments).toEqual([
            busSegment,
            suburbanSegmentOfCurrentPlan,
            suburbanSegmentWithoutPlan,
        ]);
    });

    it('current plan in context', () => {
        const filteredSegments = excludeSuburbanSegmentsOfWrongPlan(
            {segments, plans},
            'g16',
        );

        expect(filteredSegments).toEqual([
            busSegment,
            suburbanSegmentOfCurrentPlan,
            suburbanSegmentWithoutPlan,
        ]);
    });

    it('next plan in context', () => {
        const filteredSegments = excludeSuburbanSegmentsOfWrongPlan(
            {segments, plans},
            'g17',
        );

        expect(filteredSegments).toEqual([
            busSegment,
            suburbanSegmentOfNextPlan,
            suburbanSegmentWithoutPlan,
        ]);
    });
});
