const {mix} = require.requireActual('../../stringUtils');

describe('mix', () => {
    it('При вызове без аргументов - вернёт пустую строку', () =>
        expect(mix()).toBe(''));

    it('При вызове без одного из параметров - вернёт непустой аргумент', () => {
        expect(mix(undefined, 'Kitty')).toBe('Kitty');
        expect(mix('hello')).toBe('hello');
    });

    it('Оба параметра не пустые - вернёт объединенную строку', () =>
        expect(mix('hello,', 'Kitty')).toBe('hello, Kitty'));

    it('У результирующей строки убираются пробелы в начале и конце строки', () => {
        expect(mix('  hello,', 'Kitty  ')).toBe('hello, Kitty');
        expect(mix(' hello, ', ' Kitty ')).toBe('hello,   Kitty');
    });
});
