import {CHAR_THINSP} from '../../stringUtils';

import CurrencyCode from '../../../interfaces/CurrencyCode';

import formatCurrency from '../formatCurrency';

describe('formatCurrency', () => {
    it('неизвестная валюта', () => {
        expect(formatCurrency('FOO', '25', '35')).toBe(
            `25,35${CHAR_THINSP}FOO`,
        );
        expect(formatCurrency('FOO', '1 000', null)).toBe(
            `1 000${CHAR_THINSP}FOO`,
        );
    });

    it('рубли', () => {
        expect(formatCurrency(CurrencyCode.rub, '25', '35')).toBe(
            `25,35${CHAR_THINSP}₽`,
        );
        expect(formatCurrency(CurrencyCode.rub, '1 000', null)).toBe(
            `1 000${CHAR_THINSP}₽`,
        );
    });

    it('гривны', () => {
        expect(formatCurrency(CurrencyCode.uah, '25', '35')).toBe(
            `25,35${CHAR_THINSP}грн`,
        );
        expect(formatCurrency(CurrencyCode.uah, '1 000', null)).toBe(
            `1 000${CHAR_THINSP}грн`,
        );
    });

    it('доллары', () => {
        expect(formatCurrency(CurrencyCode.usd, '25', '35')).toBe('$25.35');
        expect(formatCurrency(CurrencyCode.usd, '1 000', null)).toBe('$1 000');
    });

    it('евро', () => {
        expect(formatCurrency(CurrencyCode.eur, '25', '35')).toBe('€25.35');
        expect(formatCurrency(CurrencyCode.eur, '1 000', null)).toBe('€1 000');
    });

    it('белорусские рубли', () => {
        expect(formatCurrency(CurrencyCode.byn, '25', '35')).toBe(
            `25,35${CHAR_THINSP}бел. р.`,
        );
    });
});
