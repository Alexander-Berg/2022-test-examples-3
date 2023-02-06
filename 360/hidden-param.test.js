'use strict';

const HiddenParam = require('./hidden-param');

test('simple tests', () => {
    const h = new HiddenParam(42, 'answer');
    expect(Number(h)).toEqual(42);
    expect(String(h)).toEqual('42');
    expect(JSON.stringify({ h })).toEqual('{"h":"answer"}');
});

test('evaluates #valueOf to original value', () => {
    const value = { answer: 42 };
    const h = new HiddenParam(value, '***');
    expect(h.valueOf()).toBe(value);
});
