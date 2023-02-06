jest.disableAutomock();

import findFastestSegmentValue from '../findFastestSegmentValue';

const fastestDuration = 1233;

const fastestSegment = {
    duration: fastestDuration,
};

const regularSegment = {
    duration: 55555,
};

const metasegmentWithFastestSegment = {
    duration: 5555,
    subSegments: [regularSegment, regularSegment, fastestSegment],
};

describe('findFastestSegmentValue', () => {
    it('Вернет время в пути самого быстрого сегмента', () => {
        expect(
            findFastestSegmentValue([
                fastestSegment,
                fastestSegment,
                regularSegment,
            ]),
        ).toBe(1233);
    });

    it('Для метасигмента вернет время в пути самого быстрого подсегмента', () => {
        expect(
            findFastestSegmentValue([
                regularSegment,
                metasegmentWithFastestSegment,
            ]),
        ).toBe(1233);
    });

    it('Вернет Infinity для пустого списка сегментов', () => {
        expect(findFastestSegmentValue([])).toBe(Infinity);
    });
});
