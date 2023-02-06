import { formatPrice } from './formatPrice';

const MINUS_SIGN = '\u2212';

test('zero', () => {
    expect(formatPrice(0)).toBe('0');
});

test('basic', () => {
    expect(formatPrice(42)).toBe('42');
});

test('big positive', () => {
    expect(formatPrice(1000042)).toBe('1\xa0000\xa0042');
});

test('big negative', () => {
    expect(formatPrice(-1000042)).toBe(`${MINUS_SIGN}1\xa0000\xa0042`);
});

test('big positive with decimal part', () => {
    expect(formatPrice(1000042.005)).toBe('1\xa0000\xa0042,01');
});

test('big negative with decimal part', () => {
    expect(formatPrice(-1000042.123)).toBe(`${MINUS_SIGN}1\xa0000\xa0042,12`);
});

test('round', () => {
    expect(formatPrice(1.51)).toBe('1,51');
});

test('1-digit decimal part', () => {
    expect(formatPrice(0.2)).toBe('0,20');
});

test('with config', () => {
    const config = {
        separator: '.',
        currency: '₽',
        hideDecimalPart: false
    };
    expect(formatPrice(27880.00, config)).toBe('27\xa0880.00\xa0₽');
});
