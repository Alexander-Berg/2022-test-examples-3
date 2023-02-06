import { getAnswerPercentage } from '../polls';

describe('#getAnswerPercentage', () => {
    it('Should return correct values', () => {
        expect(getAnswerPercentage(10, 100)).toBe(10);
        expect(getAnswerPercentage(0, 100)).toBe(0);
        expect(getAnswerPercentage(20, 20)).toBe(100);
        expect(getAnswerPercentage(25, 20)).toBe(100);
        expect(getAnswerPercentage(10, 0)).toBe(0);
        expect(getAnswerPercentage(0, 20)).toBe(0);
        expect(getAnswerPercentage(0, 0)).toBe(0);
    });
});
