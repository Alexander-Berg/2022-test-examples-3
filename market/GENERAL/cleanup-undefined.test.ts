import {cleanupUndefined} from './cleanup-undefined';

test('cleanup', function () {
    const cleaned = cleanupUndefined({
        a: null,
        b: undefined,
        c: false,
        d: 0,
        e: '',
        f: [],
        g: {},
    });

    expect(cleaned).toEqual({
        c: false,
        d: 0,
        e: '',
        f: [],
        g: {},
    });
});
