'use strict';

const toMid = require('./to-mid.js');

describe('helper toMid', () => {
    it('должен вернуть mid, если передали tid', () => {
        expect(toMid('t42')).toBe('42');
    });

    it('должен вернуть mid, если передали mid', () => {
        expect(toMid('42')).toBe('42');
    });

    it('должен вернуть пустую строку, если вызвали без аргументов', () => {
        expect(toMid()).toBe('');
    });
});
