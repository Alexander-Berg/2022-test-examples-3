import CurrencyCode from '../../../interfaces/CurrencyCode';

import filterSegmentsWithIncorrectPrice from '../filterSegmentsWithIncorrectPrice';

const currencies = {
    currencyRates: {
        RUB: {
            EUR: 67,
        },
        EUR: {
            RUB: 0.014,
        },
    },
};

const segmentBuilder = price => ({
    tariffs: {
        classes: {
            bus: {
                price,
            },
        },
    },
});

const correctRubPrice = {
    value: 100,
    currency: CurrencyCode.rub,
};
const incorrectRubPrice = {
    value: 9,
    currency: CurrencyCode.rub,
};

const segmentWithCorrectRubPrice = segmentBuilder(correctRubPrice);
const segmentWithIncorrectRubPrice = segmentBuilder(incorrectRubPrice);

const correctEurPrice = {
    value: 1,
    currency: CurrencyCode.eur,
};
const incorrectEurPrice = {
    value: 0.1,
    currency: CurrencyCode.eur,
};

const segmentWithCorrectEurPrice = segmentBuilder(correctEurPrice);
const segmentWithIncorrectEurPrice = segmentBuilder(incorrectEurPrice);

describe('filterSegmentsWithIncorrectPrice', () => {
    it('Пустой набор данных - вернём пустой список сегментов', () => {
        expect(filterSegmentsWithIncorrectPrice([], currencies)).toEqual([]);
    });

    it('Все цены выше минимального значения - вернём весь список сегментов', () => {
        const rubSegments = [
            segmentWithCorrectRubPrice,
            segmentWithCorrectRubPrice,
        ];

        expect(
            filterSegmentsWithIncorrectPrice(rubSegments, currencies),
        ).toEqual(rubSegments);

        const eurSegments = [
            segmentWithCorrectEurPrice,
            segmentWithCorrectEurPrice,
        ];

        expect(
            filterSegmentsWithIncorrectPrice(eurSegments, currencies),
        ).toEqual(eurSegments);
    });

    it('Есть сегменты с ценой ниже минимального значения - вернём отфильтрованный список сегментов', () => {
        expect(
            filterSegmentsWithIncorrectPrice(
                [segmentWithCorrectRubPrice, segmentWithIncorrectRubPrice],
                currencies,
            ),
        ).toEqual([segmentWithCorrectRubPrice]);

        expect(
            filterSegmentsWithIncorrectPrice(
                [segmentWithCorrectEurPrice, segmentWithIncorrectEurPrice],
                currencies,
            ),
        ).toEqual([segmentWithCorrectEurPrice]);
    });

    it('Все цены ниже минимального значения - вернём пустой список сегментов', () => {
        expect(
            filterSegmentsWithIncorrectPrice(
                [segmentWithIncorrectRubPrice, segmentWithIncorrectRubPrice],
                currencies,
            ),
        ).toEqual([]);

        expect(
            filterSegmentsWithIncorrectPrice(
                [segmentWithIncorrectEurPrice, segmentWithIncorrectEurPrice],
                currencies,
            ),
        ).toEqual([]);
    });

    it('В сегментах есть цены > и < минимального значения - вернём список измененных сегментов', () => {
        expect(
            filterSegmentsWithIncorrectPrice(
                [
                    {
                        tariffs: {
                            classes: {
                                economy: {price: correctRubPrice},
                                business: {price: incorrectRubPrice},
                            },
                        },
                    },
                ],
                currencies,
            ),
        ).toEqual([
            {
                tariffs: {
                    classes: {
                        economy: {price: correctRubPrice},
                    },
                },
            },
        ]);

        expect(
            filterSegmentsWithIncorrectPrice(
                [
                    {
                        tariffs: {
                            classes: {
                                economy: {price: correctEurPrice},
                                business: {price: incorrectEurPrice},
                            },
                        },
                    },
                ],
                currencies,
            ),
        ).toEqual([
            {
                tariffs: {
                    classes: {
                        economy: {price: correctEurPrice},
                    },
                },
            },
        ]);
    });
});
