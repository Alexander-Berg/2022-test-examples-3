const havePrices = jest.fn();

jest.setMock('../../../havePrices', havePrices);

const isCheapest = jest.requireActual('../isCheapest').default;

const CHEAPEST_1000 = {
    nationalPrice: {
        value: 1000,
    },
};
const CHEAP_1050 = {
    nationalPrice: {
        value: 1050,
    },
};
const EXPENSIVE_2000 = {
    nationalPrice: {
        value: 2000,
    },
};

describe('isCheapest', () => {
    describe('Позитивные', () => {
        it('Вернет `true`, если хотя бы одна цена тарифа совпадает с `cheapestValue`', () => {
            havePrices.mockReturnValue(true);

            const cheapestValue = 1000;
            const isManyCheapest = false;
            const segment = {
                tariffs: {
                    classes: {
                        suite: CHEAP_1050,
                        platzkarte: CHEAPEST_1000,
                    },
                },
            };

            expect(
                isCheapest(segment, {cheapestValue, isManyCheapest}),
            ).toEqual(true);
        });

        it(
            'Вернет `true`, если хотя бы одна цена ' +
                'совпадает с `cheapestValue` с точностью до целого',
            () => {
                havePrices.mockReturnValue(true);

                const cheapestValue = 1000;
                const isManyCheapest = false;
                const segment = {
                    tariffs: {
                        classes: {
                            suite: CHEAP_1050,
                            platzkarte: {
                                nationalPrice: {
                                    value: 1000.8,
                                },
                            },
                        },
                    },
                };

                expect(
                    isCheapest(segment, {cheapestValue, isManyCheapest}),
                ).toEqual(true);
            },
        );
    });

    describe('Негативные', () => {
        it('Вернет `false`, если значение `cheapestValue` не было установлено', () => {
            havePrices.mockReturnValue(true);

            const cheapestValue = 0;
            const isManyCheapest = false;
            const segment = {
                tariffs: {
                    classes: {
                        suite: {
                            nationalPrice: {
                                value: 0,
                            },
                        },
                        platzkarte: CHEAPEST_1000,
                    },
                },
            };

            expect(
                isCheapest(segment, {cheapestValue, isManyCheapest}),
            ).toEqual(false);
        });

        it('Вернет `false`, если в сегменте отсутствуют цены', () => {
            havePrices.mockReturnValue(false);

            const cheapestValue = 1000;
            const isManyCheapest = false;
            const segment = {};

            expect(
                isCheapest(segment, {cheapestValue, isManyCheapest}),
            ).toEqual(false);
        });

        it('Вернет `false`, если никакая цена тарифа не совпадает с `cheapestValue`', () => {
            havePrices.mockReturnValue(true);

            const cheapestValue = 1000;
            const isManyCheapest = false;
            const segment = {
                tariffs: {
                    classes: {
                        suite: CHEAP_1050,
                        platzkarte: EXPENSIVE_2000,
                    },
                },
            };

            expect(
                isCheapest(segment, {cheapestValue, isManyCheapest}),
            ).toEqual(false);
        });

        it('Вернет `false`, Если >50% тарифов являются самыми дешевыми', () => {
            havePrices.mockReturnValue(true);

            const cheapestValue = 1000;
            const isManyCheapest = true;
            const segment = {
                tariffs: {
                    classes: {
                        compartment: CHEAP_1050,
                        platzkarte: CHEAPEST_1000,
                    },
                },
            };

            expect(
                isCheapest(segment, {cheapestValue, isManyCheapest}),
            ).toEqual(false);
        });
    });
});
