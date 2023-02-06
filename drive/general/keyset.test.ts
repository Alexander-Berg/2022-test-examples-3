import { keyset } from 'lib/keyset';

describe('keyset', function () {
    it('should return same value', function () {
        const translates = {};

        expect(keyset(translates)).toBe(translates);
    });
});
