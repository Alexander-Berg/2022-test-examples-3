jest.disableAutomock();

import isFastestSegment from '../isFastestSegment';

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

describe('isFastestSegment', () => {
    it('Вернет true, если это "самый быстрый" сегмент', () => {
        expect(isFastestSegment(fastestSegment, fastestDuration)).toBe(true);
    });

    it('Вернет false, если это не "самый быстрый" сегмент', () => {
        expect(isFastestSegment(regularSegment, fastestDuration)).toBe(false);
    });

    it('Подсегменты игнорируются при выборе', () => {
        expect(
            isFastestSegment(metasegmentWithFastestSegment, fastestDuration),
        ).toBe(false);
    });
});
