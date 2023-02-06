import {IAviaPriceIndexPrice} from 'server/api/AviaPriceIndexApi/types/IAviaPriceIndexPrice';

import {
    getPriceHintType,
    EPriceHintType,
    usd,
} from 'projects/avia/components/Dynamics/utilities/getPriceHintType';
import IPrice from 'utilities/currency/PriceInterface';
import {CurrencyType} from 'utilities/currency/CurrencyType';

function getWeekPrice(value: number): IAviaPriceIndexPrice {
    return {
        value,
        currency: CurrencyType.RUB,
        roughly: false,
    };
}

const USD_LIMIT = 30;

describe('Определение типа цены getPriceHintType', () => {
    describe('В PI нет данных на 7 дней', () => {
        const allWithPrices = false;

        describe(`10% от цены на выдаче больше ${USD_LIMIT} usd`, () => {
            const currentPrice: IPrice = {
                value: USD_LIMIT * usd * 10 + 1,
                currency: CurrencyType.RUB,
            };

            it('Минимальная цена за 7 дней совпадает с текущей', () => {
                const weekMinPrice = getWeekPrice(currentPrice.value);

                expect(
                    getPriceHintType({
                        currentPrice,
                        weekMinPrice,
                        allWithPrices,
                        limit: USD_LIMIT,
                    }),
                ).toEqual(EPriceHintType.EMPTY);
            });

            describe('Минимальная цена за 7 дней больше текущей', () => {
                it('Минимальная цена за 7 дней больше текущей на 1', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value + 1);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 1%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.01,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 10%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.1,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 11%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.11,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 20%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.2,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 1$', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value + usd);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${
                    USD_LIMIT - 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT - 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${USD_LIMIT}$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + USD_LIMIT * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${
                    USD_LIMIT + 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT + 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });
            });

            describe('Минимальная цена за 7 дней меньше текущей', () => {
                it('Минимальная цена за 7 дней меньше текущей на 1', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value - 1);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней меньше текущей на 1%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.01,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней меньше текущей на 10%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.1,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней меньше текущей на 11%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.11,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 20%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.2,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 1$', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value - usd);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${
                    USD_LIMIT - 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT - 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${USD_LIMIT}$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - USD_LIMIT * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${
                    USD_LIMIT + 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - (USD_LIMIT + 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });
            });
        });

        describe(`10% цены на выдаче меньше ${USD_LIMIT} usd`, () => {
            const currentPrice: IPrice = {
                value: USD_LIMIT * usd * 10 - 1,
                currency: CurrencyType.RUB,
            };

            it('Минимальная цена за 7 дней совпадает с текущей', () => {
                const weekMinPrice = getWeekPrice(currentPrice.value);

                expect(
                    getPriceHintType({
                        currentPrice,
                        weekMinPrice,
                        allWithPrices,
                        limit: USD_LIMIT,
                    }),
                ).toEqual(EPriceHintType.EMPTY);
            });

            describe('Минимальная цена за 7 дней больше текущей', () => {
                it('Минимальная цена за 7 дней больше текущей на 1', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value + 1);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 1%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.01,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 10%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.1,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 11%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.11,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 20%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.2,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней больше текущей на 1$', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value + usd);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${
                    USD_LIMIT - 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT - 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${USD_LIMIT}$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + USD_LIMIT * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${
                    USD_LIMIT + 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT + 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });
            });

            describe('Минимальная цена за 7 дней меньше текущей', () => {
                it('Минимальная цена за 7 дней меньше текущей на 1', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value - 1);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней меньше текущей на 1%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.01,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней меньше текущей на 10%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.1,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it('Минимальная цена за 7 дней меньше текущей на 11%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.11,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 20%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.2,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 1$', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value - usd);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${
                    USD_LIMIT - 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT - 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${USD_LIMIT}$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - USD_LIMIT * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EMPTY);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${
                    USD_LIMIT + 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - (USD_LIMIT + 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });
            });
        });
    });

    describe('В PI есть данные на 7 дней', () => {
        const allWithPrices = true;

        describe(`10% от цены на выдаче больше ${USD_LIMIT} usd`, () => {
            const currentPrice: IPrice = {
                value: USD_LIMIT * usd * 10 + 1,
                currency: CurrencyType.RUB,
            };

            it('Минимальная цена за 7 дней совпадает с текущей', () => {
                const weekMinPrice = getWeekPrice(currentPrice.value);

                expect(
                    getPriceHintType({
                        currentPrice,
                        weekMinPrice,
                        allWithPrices,
                        limit: USD_LIMIT,
                    }),
                ).toEqual(EPriceHintType.ACCEPTABLE);
            });

            describe('Минимальная цена за 7 дней больше текущей', () => {
                it('Минимальная цена за 7 дней больше текущей на 1', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value + 1);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней больше текущей на 1%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.01,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней больше текущей на 10%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.1,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней больше текущей на 11%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.11,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.MINIMAL);
                });

                it('Минимальная цена за 7 дней больше текущей на 20%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.2,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.MINIMAL);
                });

                it('Минимальная цена за 7 дней больше текущей на 1$', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value + usd);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${
                    USD_LIMIT - 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT - 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${USD_LIMIT}$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + USD_LIMIT * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${
                    USD_LIMIT + 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT + 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.MINIMAL);
                });
            });

            describe('Минимальная цена за 7 дней меньше текущей', () => {
                it('Минимальная цена за 7 дней меньше текущей на 1', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value - 1);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 1%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.01,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 10%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.1,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 11%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.11,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 20%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.2,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 1$', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value - usd);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${
                    USD_LIMIT - 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT - 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${USD_LIMIT}$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - USD_LIMIT * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${
                    USD_LIMIT + 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - (USD_LIMIT + 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });
            });
        });

        describe(`10% цены на выдаче меньше ${USD_LIMIT} usd`, () => {
            const currentPrice: IPrice = {
                value: USD_LIMIT * usd * 10 - 1,
                currency: CurrencyType.RUB,
            };

            it('Минимальная цена за 7 дней совпадает с текущей', () => {
                const weekMinPrice = getWeekPrice(currentPrice.value);

                expect(
                    getPriceHintType({
                        currentPrice,
                        weekMinPrice,
                        allWithPrices,
                        limit: USD_LIMIT,
                    }),
                ).toEqual(EPriceHintType.ACCEPTABLE);
            });

            describe('Минимальная цена за 7 дней больше текущей', () => {
                it('Минимальная цена за 7 дней больше текущей на 1', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value + 1);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней больше текущей на 1%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.01,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней больше текущей на 10%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.1,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней больше текущей на 11%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.11,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.MINIMAL);
                });

                it('Минимальная цена за 7 дней больше текущей на 20%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + currentPrice.value * 0.2,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.MINIMAL);
                });

                it('Минимальная цена за 7 дней больше текущей на 1$', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value + usd);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${
                    USD_LIMIT - 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT - 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${USD_LIMIT}$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + USD_LIMIT * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней больше текущей на ${
                    USD_LIMIT + 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT + 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.MINIMAL);
                });
            });

            describe('Минимальная цена за 7 дней меньше текущей', () => {
                it('Минимальная цена за 7 дней меньше текущей на 1', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value - 1);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 1%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.01,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 10%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.1,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 11%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.11,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 20%', () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - currentPrice.value * 0.2,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });

                it('Минимальная цена за 7 дней меньше текущей на 1$', () => {
                    const weekMinPrice = getWeekPrice(currentPrice.value - usd);

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${
                    USD_LIMIT - 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value + (USD_LIMIT - 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${USD_LIMIT}$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - USD_LIMIT * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.ACCEPTABLE);
                });

                it(`Минимальная цена за 7 дней меньше текущей на ${
                    USD_LIMIT + 1
                }$`, () => {
                    const weekMinPrice = getWeekPrice(
                        currentPrice.value - (USD_LIMIT + 1) * usd,
                    );

                    expect(
                        getPriceHintType({
                            currentPrice,
                            weekMinPrice,
                            allWithPrices,
                            limit: USD_LIMIT,
                        }),
                    ).toEqual(EPriceHintType.EXPENSIVE);
                });
            });
        });
    });
});
