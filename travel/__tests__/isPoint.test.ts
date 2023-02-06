import isPoint from '../isPoint';

describe('isPoint', () => {
    it('is Point', () => {
        expect(isPoint('s1930')).toBe(true);
        expect(isPoint('c1234')).toBe(true);
    });

    it('is not Point', () => {
        expect(isPoint('s1290 ')).toBe(false);
        expect(isPoint(' s1290 ')).toBe(false);
        expect(isPoint(' s1290')).toBe(false);
        expect(isPoint('c1290 ')).toBe(false);
        expect(isPoint(' c1290 ')).toBe(false);
        expect(isPoint(' c1290')).toBe(false);
        expect(isPoint('cc')).toBe(false);
        expect(isPoint('1290')).toBe(false);
    });
});
