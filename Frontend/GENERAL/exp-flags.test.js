/* global it, expect */

const ExpFlags = require('./exp-flags');

describe('exp-flags', () => {
    it('Should check exp flags', () => {
        const expFlags = ExpFlags.fromUrl('https://abc.yandex-team.ru/?foo=1&exp_flags=exp1,exp2=1,exp2=2,exp3=5');

        expect(expFlags.hasKey('exp1')).toBe(true);
        expect(expFlags.hasKey('exp2')).toBe(true);
        expect(expFlags.hasKey('exp3')).toBe(true);
        expect(expFlags.hasKey('exp4')).toBe(false);

        expect(expFlags.hasVal('exp1', '')).toBe(true);
        expect(expFlags.hasVal('exp2', '1')).toBe(true);
        expect(expFlags.hasVal('exp2', '2')).toBe(true);
        expect(expFlags.hasVal('exp2', '3')).toBe(false);
        expect(expFlags.hasVal('exp3', '5')).toBe(true);
        expect(expFlags.hasVal('exp4', '?')).toBe(false);
    });
});
