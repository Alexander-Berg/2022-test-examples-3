import { createFlags, createAttrs } from './TrendboxUtils';

describe('createArgs', () => {
    type TOptions = 'propA' | 'propB';

    it('build args string', () => {
        const expected = '--propA "valA" --propB "valB"';
        expect(createFlags<TOptions>({ propA: 'valA', propB: 'valB' })).toEqual(expected);
    });

    it('returns empty string on no props', () => {
        expect(createFlags<TOptions>()).toEqual('');
    });

    it('returns empty string on empty props', () => {
        expect(createFlags<TOptions>({})).toEqual('');
    });
});

describe('createAttrs', () => {
    it('build args string', () => {
        const expected = '--attr keyA="valA" --attr keyB="valB" --attr num="3" --attr success="true"';
        expect(createAttrs({ keyA: 'valA', keyB: 'valB', num: 3, success: true })).toEqual(expected);
    });

    it('returns empty string on no props', () => {
        expect(createAttrs()).toEqual('');
    });

    it('returns empty string on empty props', () => {
        expect(createAttrs({})).toEqual('');
    });
});
