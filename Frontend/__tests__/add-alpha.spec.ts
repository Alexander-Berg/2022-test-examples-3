import addAlpha from '../add-alpha';

describe('addAlpha', () => {
    it('hexToRgba', () => {
        expect(addAlpha('#ffffff', 0.3)).toBe('rgba(255, 255, 255, 0.3)');
        expect(addAlpha('#000000', 0.3)).toBe('rgba(0, 0, 0, 0.3)');
    });

    it('rgba', () => {
        expect(addAlpha('rgba(255, 255, 255, 0.9)', 0.5)).toBe('rgba(255, 255, 255, 0.45)');
        expect(addAlpha('rgba(0, 0, 0, .6)', 0.2)).toBe('rgba(0, 0, 0, 0.12)');

        expect(addAlpha('rgba(255, 255, 255)', 0.5)).toBe('rgba(255, 255, 255, 0.5)');
        expect(addAlpha('rgba(0, 0, 0)', 0.5)).toBe('rgba(0, 0, 0, 0.5)');
    });

    it('wrong', () => {
        expect(addAlpha('ffffff', 0.9)).toBe('ffffff');
        expect(addAlpha('rgba(0, 0.3)', 0.3)).toBe('rgba(0, 0.3)');
    });
});
