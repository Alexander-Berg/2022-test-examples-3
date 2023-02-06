const { isValidHost } = require('../../core/data-from-apphost-context');

describe('data-from-apphost-context', () => {
    test('isValidHost отсекает одноуровневые домены', () => {
        expect(isValidHost('about')).toBe(false);
    });

    test('isValidHost отсекает домены с wildcard', () => {
        expect(isValidHost('*.google.com')).toBe(false);
        expect(isValidHost('top.*.com')).toBe(false);
        expect(isValidHost('top.second.*')).toBe(false);
    });

    test('isValidHost работает если не передано значение', () => {
        expect(isValidHost()).toBe(false);
    });

    test('isValidHost возвращает true для корректных хостов', () => {
        expect(isValidHost('lenta.ru')).toBe(true);
        expect(isValidHost('m.lenta.ru')).toBe(true);
        expect(isValidHost('gazeta2.com')).toBe(true);
    });
});
