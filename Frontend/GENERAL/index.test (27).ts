import { parseMeta } from '.';

describe('parseMeta', () => {
    const g = global as typeof global & { Ya: Record<string, string | Function> };

    beforeEach(() => {
        // В jsdom в new Function вместо global - window, почему-то.
        global.global = g;
    });

    it('parse valid JSONP', () => {
        const res = parseMeta('Ya[123](\'{\\\'a\\\': 1}\')');
        expect(res).toEqual({ a: 1 });
    });

    it('parse valid JSON', () => {
        const res = parseMeta('{"a": 1}');
        expect(res).toEqual({ a: 1 });
    });

    it('parse valid JSONP inside JSON', () => {
        const res = parseMeta('{\'a\': 1}');
        expect(res).toEqual({ a: 1 });
    });

    it('throws on invalid JSONP', () => {
        expect(() => parseMeta('Ya[123](\'{\\\'a\\\': \\\'1}\')')).toThrow('Malformed JSONP');
    });

    it('throws on invalid JSON', () => {
        expect(() => parseMeta('{\'a\': \'1}')).toThrow('Malformed JSON');
    });
});
