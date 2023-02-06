const filterTariffs = jest.fn(segments => segments);

jest.setMock('../filterTariffs', filterTariffs);

const findCheapestValue = require.requireActual('../findCheapestValue').default;

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

describe('findCheapestValue', () => {
    describe('Позитивные', () => {
        it('Вернет минимальное из всех значений', () => {
            const segments = [
                {
                    tariffs: {
                        classes: {
                            compartment: CHEAP_1050,
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            suite: CHEAPEST_1000,
                            platzkarte: EXPENSIVE_2000,
                        },
                    },
                },
            ];

            expect(findCheapestValue(segments)).toEqual(1000);
        });

        it('Вернет обрезанное (до целого) значение, если оно было дробным', () => {
            const segments = [
                {
                    tariffs: {
                        classes: {
                            compartment: {
                                nationalPrice: {
                                    value: 1002,
                                },
                            },
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            suite: {
                                nationalPrice: {
                                    value: 1000.8,
                                },
                            },
                            platzkarte: {
                                nationalPrice: {
                                    value: 1000.9,
                                },
                            },
                        },
                    },
                },
            ];

            expect(findCheapestValue(segments)).toEqual(1000);
        });
    });

    /* Отсутствие каких-либо полей не тестируем, это фильтруется в filterTariffs */
});
