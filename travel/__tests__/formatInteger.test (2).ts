import {CHAR_SIX_PER_EM_SPACE} from 'constants/charCodes';

import formatInteger from '../formatInteger';

describe('formatInteger(number, thousandsDelimiter)', () => {
    test('Должен вернуть 5', () => {
        expect(formatInteger(5)).toBe('5');
    });

    test('Должен вернуть 55', () => {
        expect(formatInteger(55)).toBe('55');
    });

    test('Должен вернуть 555', () => {
        expect(formatInteger(555)).toBe('555');
    });

    test('Должен вернуть 5 555', () => {
        expect(formatInteger(5555)).toBe(`5${CHAR_SIX_PER_EM_SPACE}555`);
    });

    test('Должен вернуть 555 555', () => {
        expect(formatInteger(555555)).toBe(`555${CHAR_SIX_PER_EM_SPACE}555`);
    });

    test('Должен вернуть 5 555 555', () => {
        expect(formatInteger(5555555)).toBe(
            `5${CHAR_SIX_PER_EM_SPACE}555${CHAR_SIX_PER_EM_SPACE}555`,
        );
    });

    test('Должен вернуть 5,555', () => {
        expect(formatInteger(5555, ',')).toBe('5,555');
    });

    test('Должен вернуть 5 555, передали строку', () => {
        expect(formatInteger('5555')).toBe(`5${CHAR_SIX_PER_EM_SPACE}555`);
    });
});
