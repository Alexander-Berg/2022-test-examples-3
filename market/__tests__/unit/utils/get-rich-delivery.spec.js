const { getRichDelivery } = require('../../../middleware/product-offers-search/format/format-offers');

describe('Rich delivery', () => {
    const testTable = [
        {
            name: 'free delivery, 10-15 days',
            input: {
                free: true,
                options: [
                    {
                        conditions: {
                            daysFrom: 10,
                            daysTo: 15,
                        },
                    },
                ],
            },
            expected: expect.objectContaining({
                free: true,
                duration: '10-15 дней',
            }),
        },
        {
            name: 'free delivery, 2 days',
            input: {
                free: true,
                options: [
                    {
                        conditions: {
                            daysFrom: 2,
                            daysTo: 2,
                        },
                    },
                ],
            },
            expected: expect.objectContaining({
                free: true,
                duration: '2 дня',
            }),
        },
        {
            name: 'free delivery, 2 days',
            input: {
                free: true,
                options: [
                    {
                        conditions: {
                            daysFrom: 0,
                            daysTo: 2,
                        },
                    },
                ],
            },
            expected: expect.objectContaining({
                free: true,
                duration: '2 дня',
            }),
        },
        {
            name: '300 ₽, 2-3 days',
            input: {
                free: false,
                price: {
                    value: '300',
                },
                options: [
                    {
                        conditions: {
                            daysFrom: 2,
                            daysTo: 3,
                        },
                    },
                ],
            },
            expected: expect.objectContaining({
                price: '300',
                duration: '2-3 дня',
            }),
        },
        {
            name: '300 ₽, 6 days',
            input: {
                free: false,
                price: {
                    value: '300',
                },
                options: [
                    {
                        conditions: {
                            daysFrom: 0,
                            daysTo: 6,
                        },
                    },
                ],
            },
            expected: expect.objectContaining({
                price: '300',
                duration: '6 дней',
            }),
        },
        {
            name: 'from city',
            input: {
                free: false,
                carried: true,
                options: [
                    {
                        brief: 'в Калининград из Петропаловск-Камчатский',
                    },
                ],
            },
            expected: expect.objectContaining({
                text: 'из Петропаловск-Камчатский',
            }),
        },
        {
            name: 'without delivery',
            input: {
                free: false,
                carried: false,
            },
            expected: expect.objectContaining({
                withoutDelivery: true,
            }),
        },
        {
            name: 'free delivery, tomorrow',
            input: {
                free: true,
                options: [
                    {
                        conditions: {
                            daysTo: 1,
                        },
                    },
                ],
            },
            expected: expect.objectContaining({
                free: true,
                duration: 'завтра',
            }),
        },
        {
            name: '300 ₽, tomorrow',
            input: {
                free: false,
                price: {
                    value: '300',
                },
                options: [
                    {
                        conditions: {
                            daysTo: 1,
                        },
                    },
                ],
            },
            expected: expect.objectContaining({
                price: '300',
                duration: 'завтра',
            }),
        },
        {
            name: '300 ₽',
            input: {
                free: false,
                carried: true,
                price: {
                    value: '300',
                },
                options: [
                    {
                        brief: 'на заказ',
                    },
                ],
            },
            expected: expect.objectContaining({
                price: '300',
            }),
        },
        {
            name: '300 ₽ (ignore &nbsp;)',
            input: {
                free: false,
                carried: true,
                price: {
                    value: '300',
                },
                options: [
                    {
                        brief: 'на&nbspзаказ',
                    },
                ],
            },
            expected: expect.objectContaining({
                price: '300',
                duration: undefined,
            }),
        },
        {
            name: 'free delivery, today',
            input: {
                free: true,
                carried: true,
                options: [
                    {
                        brief: 'на заказ',
                        conditions: {
                            daysTo: 0,
                            daysFrom: 0,
                        },
                    },
                ],
            },
            expected: expect.objectContaining({
                free: true,
                duration: 'сегодня',
            }),
        },
        {
            name: '300 ₽, today',
            input: {
                free: false,
                carried: true,
                price: {
                    value: '300',
                },
                options: [
                    {
                        conditions: {
                            daysTo: 0,
                            daysFrom: 0,
                        },
                    },
                ],
            },
            expected: expect.objectContaining({
                duration: 'сегодня',
                price: '300',
            }),
        },
    ];

    testTable.forEach((testCase) => {
        const { input, expected, name } = testCase;

        test(`${name}`, () => {
            expect(getRichDelivery(input)).toEqual(expected);
        });
    });
});
