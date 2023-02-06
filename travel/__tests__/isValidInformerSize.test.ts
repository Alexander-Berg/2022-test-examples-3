import isValidInformerSize from '../isValidInformerSize';

describe('isInformerSize', () => {
    it('is IInformerSize', () => {
        expect(isValidInformerSize(5)).toBe(true);
        expect(isValidInformerSize(15)).toBe(true);
        expect(isValidInformerSize(25)).toBe(true);
    });

    it('is not IInformerSize', () => {
        expect(isValidInformerSize(3)).toBe(false);
    });
});
