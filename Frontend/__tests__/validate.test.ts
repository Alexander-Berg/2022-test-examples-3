import { isValidName } from '../validate';

describe('isValidName', () => {
    it('returns true on correct cyrillic name', () => {
        const name = 'Виктор-Ворони\'н';
        expect(isValidName(name)).toBe(true);
    });

    it('returns true on correct latin name', () => {
        const name = 'Vladimir-Kinp\'u';
        expect(isValidName(name)).toBe(true);
    });

    it('returns true on correct mixed name', () => {
        const name = 'Vladimir-Кинп\'у';
        expect(isValidName(name)).toBe(true);
    });

    it('returns false on unacceptable symbol', () => {
        const name1 = 'Vladimir-Кинп\'у@';
        const name2 = 'Vladimir-Кинп\'у1';
        const name3 = 'Vladimir-Кинп\'у ';
        expect(isValidName(name1)).toBe(false);
        expect(isValidName(name2)).toBe(false);
        expect(isValidName(name3)).toBe(false);
    });
});
