import CurrencyCode from '../../../interfaces/CurrencyCode';

import getNationalCurrency from '../getNationalCurrency';

describe('getNationalCurrency', () => {
    it('Get for ru', () => {
        expect(getNationalCurrency('ru')).toBe(CurrencyCode.rub);
    });
});
