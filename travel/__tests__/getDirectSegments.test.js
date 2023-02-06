jest.disableAutomock();

import {TRAIN_TYPE, PLANE_TYPE} from '../../transportType';

import getDirectSegments from '../getDirectSegments';

const trainSegment = {
    transport: {
        code: TRAIN_TYPE,
    },
};
const trainsTransferSegment = {
    ...trainSegment,
    segments: [trainSegment, trainSegment],
};
const planeSegment = {
    transport: {
        code: PLANE_TYPE,
    },
};
const planeTransferSegment = {
    ...planeSegment,
    segments: [planeSegment, planeSegment],
};

describe('getDirectSegments', () => {
    it('Вернет все прямые сегменты', () => {
        expect(
            getDirectSegments([
                trainsTransferSegment,
                trainSegment,
                planeTransferSegment,
                planeSegment,
            ]),
        ).toEqual([trainSegment, planeSegment]);
    });

    it('Вернет прямые сегенты нужного типа.', () => {
        expect(
            getDirectSegments(
                [trainSegment, planeSegment, trainsTransferSegment],
                TRAIN_TYPE,
            ),
        ).toEqual([trainSegment]);
    });

    it('Есть прямые сегменты, но нет нужного типа. Вернет [].', () => {
        expect(
            getDirectSegments(
                [planeSegment, trainsTransferSegment],
                TRAIN_TYPE,
            ),
        ).toEqual([]);
    });

    it('Только трансферные сегменты. Вернет [].', () => {
        expect(
            getDirectSegments([planeTransferSegment, trainsTransferSegment]),
        ).toEqual([]);
        expect(
            getDirectSegments(
                [planeTransferSegment, trainsTransferSegment],
                TRAIN_TYPE,
            ),
        ).toEqual([]);
        expect(
            getDirectSegments(
                [planeTransferSegment, trainsTransferSegment],
                PLANE_TYPE,
            ),
        ).toEqual([]);
    });

    it('Пустая выдача. Вернет [].', () => {
        expect(getDirectSegments([])).toEqual([]);
    });
});
