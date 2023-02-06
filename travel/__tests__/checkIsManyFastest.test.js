jest.disableAutomock();

import checkIsManyFastest from '../checkIsManyFastest';

const fastestDuration = 1233;
const percentToOffBadges = 0.5;

const fastestSegment = {
    duration: fastestDuration,
};

const regularSegment = {
    duration: 55555,
};

describe('checkIsManyFastest', () => {
    it('Доля самых быстрых сегментов не превышает границу для отключения бейджиков этого типа', () => {
        expect(
            checkIsManyFastest(
                [
                    fastestSegment,
                    fastestSegment,
                    regularSegment,
                    regularSegment,
                    regularSegment,
                ],
                fastestDuration,
                percentToOffBadges,
            ),
        ).toBe(false);
    });

    it('Доля самых быстрых сегментов превышает границу для отключения бейджиков этого типа', () => {
        expect(
            checkIsManyFastest(
                [
                    fastestSegment,
                    fastestSegment,
                    fastestSegment,
                    regularSegment,
                    regularSegment,
                ],
                fastestDuration,
                percentToOffBadges,
            ),
        ).toBe(true);
    });
});
