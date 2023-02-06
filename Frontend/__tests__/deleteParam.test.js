const { deleteParamPolyfill } = require('../deleteParam');

describe('deleteCGIParams', () => {
    it('deleting get parameters in the middle', () => {
        expect(deleteParamPolyfill('?flags=qwer=1&text=etrgerrgrgr&zxcv=1', 'text'))
            .toBe('?flags=qwer%3D1&zxcv=1');
    });

    it('deleting get parameters in the begin', () => {
        expect(deleteParamPolyfill('?flags=qwer=1&text=etrgerrgrgr&zxcv=1', 'flags'))
            .toBe('?text=etrgerrgrgr&zxcv=1');
    });

    it('deleting get parameters in the end', () => {
        expect(deleteParamPolyfill('?flags=qwer=1&text=etrgerrgrgr&zxcv=1', 'zxcv'))
            .toBe('?flags=qwer%3D1&text=etrgerrgrgr');
    });

    it('not delete', () => {
        expect(deleteParamPolyfill('?flags=qwer=1&text=etrgerrgrgr&zxcv=1', 'qwer'))
            .toBe('?flags=qwer%3D1&text=etrgerrgrgr&zxcv=1');
    });

    it('not delete with an empty param value', () => {
        expect(deleteParamPolyfill('?flags=qwer=1&text&zxcv=1', 'qwer'))
            .toBe('?flags=qwer%3D1&text&zxcv=1');
    });

    it('delete with an empty param value', () => {
        expect(deleteParamPolyfill('?flags=qwer=1&text&zxcv=1', 'text'))
            .toBe('?flags=qwer%3D1&zxcv=1');
    });

    it('empty search', () => {
        expect(deleteParamPolyfill('', 'text'))
            .toBe('');
    });

    it('deleting parameter with encode param', () => {
        expect(deleteParamPolyfill('?flags=qwer%3D1&text=qwer=1&zxcv=1', 'text'))
            .toBe('?flags=qwer%3D1&zxcv=1');
    });

    it('deleting parameter with not encode param', () => {
        expect(deleteParamPolyfill('?flags=qwer%3D1&text=qwer=1&zxcv=1', 'flags'))
            .toBe('?text=qwer%3D1&zxcv=1');
    });

    it('deleting parameter in incorrect url', () => {
        expect(deleteParamPolyfill('?flags=qweqwe%sfdfr%3D1&text=qwer=1&zxcv=1', 'text'))
            .toBe('?flags=qweqwe%sfdfr%3D1&zxcv=1');
    });
});
