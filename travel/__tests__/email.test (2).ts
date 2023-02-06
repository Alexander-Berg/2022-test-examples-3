import {EMAIL_MASK} from '../common';

describe('Валидация email в бронировании поездов', () => {
    const emailRegex = new RegExp(EMAIL_MASK);

    test('Базовая структура email', () => {
        // Valid case
        expect(emailRegex.test('t@test.te')).toBe(true);
        expect(emailRegex.test('test@test.test')).toBe(true);
        expect(emailRegex.test('  test@test.test  ')).toBe(true);

        // Invalid cases
        expect(emailRegex.test('testtesttest')).toBe(false);
        expect(emailRegex.test('testtest.test')).toBe(false);
        expect(emailRegex.test('test@testtest')).toBe(false);
    });

    test('Проверка на соответствие симоволов: /a-zA-Z_.+\\-@0-9/', () => {
        // Valid case
        expect(emailRegex.test('ANZanz_.+-059@ANZanz_+-059.ANZanz')).toBe(true);

        // Invalid cases
        expect(emailRegex.test('tesЯt@test.test')).toBe(false);
        expect(emailRegex.test('test@teЯst.test')).toBe(false);

        expect(emailRegex.test('tes*t@test.test')).toBe(false);
        expect(emailRegex.test('test@te*st.test')).toBe(false);

        // Invalid TLD
        expect(emailRegex.test('test@test.teЯst')).toBe(false);
        expect(emailRegex.test('test@test.te*st')).toBe(false);
        expect(emailRegex.test('test@test.te0st')).toBe(false);
    });

    test('Не пропускает лишние точки (точка в начале, перед и после @, после TLD, с лишними точками перед TLD, вместо TLD)', () => {
        // Valid cases
        expect(emailRegex.test('test@test.test')).toBe(true);
        expect(emailRegex.test('as.test@test.test')).toBe(true);
        expect(emailRegex.test('test.test.test@test.test')).toBe(true);

        // Invalid cases
        expect(emailRegex.test('.test@test.test')).toBe(false);
        expect(emailRegex.test('test.@test.test')).toBe(false);
        expect(emailRegex.test('test@.test.test')).toBe(false);
        expect(emailRegex.test('test@te.st.test')).toBe(false);
        expect(emailRegex.test('test@test..test')).toBe(false);
        expect(emailRegex.test('test@test.test.')).toBe(false);
    });
});
