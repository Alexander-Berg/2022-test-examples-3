import resolveRgba from '../resolveRgba';

describe('resolveRgba', () => {
    it('HEX в rgba заменяется на rgb', () => {
        expect(resolveRgba('rgba(#04b, 0.6), rgba(#04f, 0.6)')).toBe(
            'rgba(0, 68, 187, 0.6), rgba(0, 68, 255, 0.6)',
        );
    });
});
