import AbcExpFlags from 'b:abc-exp-flags';

describe('AbcExpFlags', () => {
    it('Should return false on key', () => {
        expect(AbcExpFlags.hasKey('foobarbaz')).toBe(false);
    });

    it('Should return false on key/val', () => {
        expect(AbcExpFlags.hasVal('foobarbaz', 'asdasd')).toBe(false);
    });
});
