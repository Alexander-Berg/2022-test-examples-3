import CurrencyCode from '../../../interfaces/CurrencyCode';

import {getMinPriceFromTariffs} from '../getMinPriceFromTariffs';

describe('getMinPriceFromTariffs', () => {
    it('Если данных нет - вернёт null', () => {
        expect(
            getMinPriceFromTariffs([{classes: {}}, {classes: {}}]),
        ).toBeNull();
    });

    it('Если данные есть - вернёт минимальную цену', () => {
        expect(
            getMinPriceFromTariffs([
                {
                    classes: {
                        platzkarte: {
                            price: {
                                value: 1200,
                                currency: CurrencyCode.rub,
                            },
                        },
                        suite: {
                            price: {
                                value: 2400,
                                currency: CurrencyCode.rub,
                            },
                        },
                    },
                },
                {
                    classes: {
                        sitting: {
                            price: {
                                value: 600,
                                currency: CurrencyCode.rub,
                            },
                        },
                    },
                },
            ]),
        ).toEqual({
            value: 600,
            currency: CurrencyCode.rub,
        });
    });
});
