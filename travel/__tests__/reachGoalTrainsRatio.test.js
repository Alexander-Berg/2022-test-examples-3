jest.dontMock('../../segments/getTransportTypeRatio');

const reachGoalOnce = jest.fn();

jest.setMock('../../yaMetrika', {reachGoalOnce});
const reachGoalTrainsRatio = require.requireActual(
    '../reachGoalTrainsRatio',
).default;
const {TRAIN_TYPE, BUS_TYPE} = require.requireActual('../../transportType');

const ratio80 = 'train-segments-ratio-80';
const ratio50 = 'train-segments-ratio-50';

const getSegments = (code, length) =>
    Array.from({length}, () => ({transport: {code}}));

describe('reachGoalSegmentsRatio', () => {
    it('should not call reach goal', () => {
        reachGoalTrainsRatio([]);
        reachGoalTrainsRatio(getSegments(BUS_TYPE, 10));
        expect(reachGoalOnce).not.toBeCalledWith(ratio50);
        expect(reachGoalOnce).not.toBeCalledWith(ratio80);
    });

    it('should call reach goal "train-segments-ratio-50"', () => {
        reachGoalTrainsRatio([
            ...getSegments(BUS_TYPE, 5),
            ...getSegments(TRAIN_TYPE, 5),
        ]);
        expect(reachGoalOnce).toBeCalledWith(ratio50);
        expect(reachGoalOnce).not.toBeCalledWith(ratio80);
    });

    it('should call reach goal "train-segments-ratio-50" and "train-segments-ratio-80"', () => {
        reachGoalTrainsRatio([
            ...getSegments(BUS_TYPE, 2),
            ...getSegments(TRAIN_TYPE, 8),
        ]);
        expect(reachGoalOnce).toBeCalledWith(ratio50);
        expect(reachGoalOnce).toBeCalledWith(ratio80);
    });
});
