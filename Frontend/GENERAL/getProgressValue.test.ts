import { levelScale } from '../InterviewAchievementProgress.constants';

import { getProgressValue } from './getProgressValue';

describe('InterviewAchievementProgress: getProgressValue', () => {
    for (let level = 0; level < levelScale.length - 1; level++) {
        const rightLevelBoundary = levelScale[level + 1];
        const count = rightLevelBoundary * 0.2;

        it(`should return progress value equal to 20 when count equal ${count}`, () => {
            expect(getProgressValue(count, levelScale[level + 1])).toEqual(20);
        });
    }
});
