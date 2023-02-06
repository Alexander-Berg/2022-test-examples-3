import CurrencyCode from '../../../interfaces/CurrencyCode';

import formatPrice from '../formatPrice';
import formatInteger from '../formatInteger';
import formatCurrency from '../formatCurrency';

import priceKeyset from '../../../i18n/price';

jest.mock('../../../i18n/price', () => jest.fn(() => 'от 120 ₴'));
jest.mock('../formatInteger');
jest.mock('../formatCurrency');

describe('formatPrice', () => {
    it('should not round prices less than 100', () => {
        const price = {
            value: 99.99,
            currency: CurrencyCode.usd,
        };

        (formatInteger as jest.Mock).mockReturnValue('99');
        (formatCurrency as jest.Mock).mockReturnValue('$99.99');

        expect(formatPrice(price, {})).toBe('$99.99');

        expect(formatInteger).toBeCalledWith('99');
        expect(formatCurrency).toBeCalledWith(CurrencyCode.usd, '99', '99');
    });

    it('should round prices equal or greater than 100', () => {
        const price = {
            value: 100.01,
            currency: 'RUR',
        };

        (formatInteger as jest.Mock).mockReturnValue('100');
        (formatCurrency as jest.Mock).mockReturnValue('100 Р');

        expect(formatPrice(price, {})).toBe('100 Р');

        expect(formatInteger).toBeCalledWith('100');
        expect(formatCurrency).toBeCalledWith('RUR', '100', undefined);
    });

    it('should not show zero cents for whole prices', () => {
        const price = {
            value: 90,
            currency: CurrencyCode.eur,
        };

        (formatInteger as jest.Mock).mockReturnValue('90');
        (formatCurrency as jest.Mock).mockReturnValue('€90');

        expect(formatPrice(price, {})).toBe('€90');

        expect(formatInteger).toBeCalledWith('90');
        expect(formatCurrency).toBeCalledWith(CurrencyCode.eur, '90', '');
    });

    it('should round price if `round` parameter is `true`', () => {
        const price = {
            value: 199.99,
            currency: CurrencyCode.usd,
        };

        const round = true;

        (formatInteger as jest.Mock).mockReturnValue('200');
        (formatCurrency as jest.Mock).mockReturnValue('$200');

        expect(formatPrice(price, {round})).toBe('$200');

        expect(formatInteger).toBeCalledWith('200');
        expect(formatCurrency).toBeCalledWith(
            CurrencyCode.usd,
            '200',
            undefined,
        );
    });

    it('should not round price if `round` parameter is `false`', () => {
        const price = {
            value: 300.01,
            currency: 'RUR',
        };

        const round = false;

        (formatInteger as jest.Mock).mockReturnValue('300');
        (formatCurrency as jest.Mock).mockReturnValue('300.01 Р');

        expect(formatPrice(price, {round})).toBe('300.01 Р');

        expect(formatInteger).toBeCalledWith('300');
        expect(formatCurrency).toBeCalledWith('RUR', '300', '01');
    });

    it('should show localized from string if `from` parameter is specified', () => {
        const price = {
            value: 120,
            currency: CurrencyCode.uah,
        };

        (formatInteger as jest.Mock).mockReturnValue('120');
        (formatCurrency as jest.Mock).mockReturnValue('120 ₴');

        expect(formatPrice(price, {from: true})).toBe('от 120 ₴');
        expect(priceKeyset).toBeCalledWith('from', {price: '120 ₴'});
    });
});
