import { formatNumber } from './numbers';

const MINUS = '−'; // это не дефис

describe('common/helpers/numbers', () => {
    let bemLangBackup: typeof process.env.BEM_LANG = 'en';

    beforeAll(() => {
        bemLangBackup = process.env.BEM_LANG;
        // форматируем всё в английской локали, чтобы тесты были стабильными в любом окружении
        process.env.BEM_LANG = 'en';
    });

    afterAll(() => {
        process.env.BEM_LANG = bemLangBackup;
    });

    describe('formatNumber', () => {
        it('Should format numbers', () => {
            expect(formatNumber('99999.99')).toBe('99,999.99');
            expect(formatNumber(99999.99)).toBe('99,999.99');
        });

        it('Should format NaN to a localized string', () => {
            // например, в русской локали тут будет строка "не число"
            expect(formatNumber('qwe')).toBe('NaN');
        });

        it('Should return empty string for empty value', () => {
            expect(formatNumber()).toBe('');
            expect(formatNumber(undefined)).toBe('');
            expect(formatNumber('')).toBe('');
        });

        it('Should format big numbers', () => {
            expect(formatNumber(`${Number.MAX_SAFE_INTEGER}0`)).toBe('90,071,992,547,409,910');
            expect(formatNumber(`${Number.MAX_SAFE_INTEGER}0.9999999999999999`)).toBe('90,071,992,547,409,910.9999999999999999');
        });

        it('Should use proper minus sign for negative numbers', () => {
            expect(formatNumber(-1)[0]).toBe(MINUS);
        });

        describe('options', () => {
            it('Should opt in to add the plus sign to positive numbers', () => {
                expect(formatNumber(1)[0]).not.toBe('+');
                expect(formatNumber(1, { asDiff: true })[0]).toBe('+');
            });

            it('Should accept a char to use as a decimal separator', () => {
                // формат по умолчанию - из текущей локали
                expect(formatNumber('1.2')).toBe('1.2');
                expect(formatNumber('1,2')).toBe('NaN');
                // заданный формат
                expect(formatNumber('-1,2', { decimal: ',' })).toBe(`${MINUS}1.2`);
                expect(formatNumber('-1.2', { decimal: ',' })).toBe('NaN');
            });

            it('Should opt in to preserve fractional part consisting of zeros', () => {
                expect(formatNumber('1.0000')).not.toBe('1.0000');
                expect(formatNumber('1.0000', { keepZeroFraction: true })).toBe('1.0000');
            });
        });

        it('Should remove unnecessary signs', () => {
            expect(formatNumber('-0')).toBe('0');
            expect(formatNumber('-0.000')).toBe('0');
            expect(formatNumber('+0')).toBe('0');
            expect(formatNumber('+0.000')).toBe('0');
            expect(formatNumber('+1000')).toBe('1,000');
        });

        it('Should not lose sign for negative numbers with a zero integer part', () => {
            expect(formatNumber(-0.1)).toBe(`${MINUS}0.1`);
            expect(formatNumber(-0.1, { asDiff: true })).toBe(`${MINUS}0.1`);
            expect(formatNumber(0.1, { asDiff: true })).toBe('+0.1');
        });
    });
});
