import { numericRegExp } from 'ui-kit/text-input/numeric';

describe('text-input/numeric', () => {
    it('Должны быть числовые значения', () => {
        const numbers = [
            '0', '1',
            '0.1', '0,1',
            '.1', ',1',
            '-1',
            '-0.1', '-0,1',
            '-.1', '-,1',
        ];
        numbers.forEach((number) => {
            expect(number).toMatch(numericRegExp);
        });
    });
    it('Должны быть не числовые значения', () => {
        const numbers = [
            '0xf', '1b',
            '0-1', '+23',
            'один', 'минус5',
            '1-',
        ];
        numbers.forEach((number) => {
            expect(number).not.toMatch(numericRegExp);
        });
    });
});
