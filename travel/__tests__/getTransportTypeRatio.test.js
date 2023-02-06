jest.disableAutomock();

import {
    BUS_TYPE,
    PLANE_TYPE,
    TRAIN_TYPE,
    SUBURBAN_TYPE,
} from '../../transportType';
import {
    getTrainSegmentsRatio,
    getTransportTypeRatio,
} from '../getTransportTypeRatio';

const busSegment = {transport: {code: BUS_TYPE}};
const planeSegment = {transport: {code: PLANE_TYPE}};
const trainSegment = {transport: {code: TRAIN_TYPE}};
const suburbanSegment = {transport: {code: SUBURBAN_TYPE}};

const segments = [
    busSegment,
    planeSegment,
    trainSegment,
    trainSegment,
    planeSegment,
    planeSegment,
    planeSegment,
    suburbanSegment,
];

describe('getTransportTypeRatio', () => {
    it('Для пустого списка сегментов - вернет NaN', () => {
        expect(isNaN(getTransportTypeRatio([], BUS_TYPE))).toBe(true);
    });

    it('Вернёт соотношение сегментов 0', () => {
        expect(getTransportTypeRatio([busSegment], TRAIN_TYPE)).toBe(0);
    });

    it('Вернёт соотношение сегментов 50', () => {
        expect(getTransportTypeRatio(segments, PLANE_TYPE)).toBe(50);
    });

    it('Вернёт соотношение сегментов 100', () => {
        expect(getTransportTypeRatio([busSegment, busSegment], BUS_TYPE)).toBe(
            100,
        );
    });
});

describe('getTrainSegmentsRatio', () => {
    it('Вернет соотношение поездов ко всем сегментам - 25', () => {
        expect(getTrainSegmentsRatio(segments)).toBe(25);
    });
});
