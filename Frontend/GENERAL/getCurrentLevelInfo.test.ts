import { levelScale } from '../InterviewAchievementProgress.constants';

import { getCurrentLevelInfo } from './getCurrentLevelInfo';

describe('InterviewAchievementProgress: getCurrentLevelInfo', () => {
    it('should return zero level info when count is zero', () => {
        const count = 0;
        const level = 0;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return zero level info when count in zero level range', () => {
        const count = 5;
        const level = 0;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return first level info when count equal to first level left boundary', () => {
        const count = 10;
        const level = 1;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return first level info when count in first level range', () => {
        const count = 15;
        const level = 1;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return second level info when count in second level range', () => {
        const count = 26;
        const level = 2;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return third level info when count in third level range', () => {
        const count = 60;
        const level = 3;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return fourth level info when count in fourth level range', () => {
        const count = 150;
        const level = 4;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return fifth level info when count in fifth level range', () => {
        const count = 499;
        const level = 5;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return sixth level info when count in sixth level range', () => {
        const count = 571;
        const level = 6;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return seventh level info when count in seventh level range', () => {
        const count = 1945;
        const level = 7;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return eight level info when count equal then eight level left boundary', () => {
        const count = 2000;
        const level = 7;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });

    it('should return eight level info when count more then eight level left boundary', () => {
        const count = 10000;
        const level = 7;
        expect(getCurrentLevelInfo(count)).toEqual([level, levelScale[level + 1]]);
    });
});
