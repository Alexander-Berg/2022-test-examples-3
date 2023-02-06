export const OFFERS_VERDICTS_MOCK = {
    entries: [
        {
            verdicts: {
                messages: [
                    {
                        identifiers: [
                            {
                                shopId: 11138645,
                                warehouseId: 48585,
                                offerId: '123',
                                businessId: 10964073,
                            },
                        ],
                        explanation: {
                            namespace: 'mboc.ci.error',
                            code: 'mboc.error.excel-value-is-required',
                            params: [{name: 'header', value: 'Вес в упаковке в килограммах'}],
                            text: "Отсутствует значение для колонки 'Вес в упаковке в килограммах'",
                            level: 3,
                            details: '{"header":"Вес в упаковке в килограммах"}',
                        },
                        isRelevant: true,
                    },
                    {
                        identifiers: [
                            {
                                shopId: 11138645,
                                warehouseId: 48585,
                                offerId: '123',
                                businessId: 10964073,
                            },
                        ],
                        explanation: {
                            namespace: 'mboc.ci.error',
                            code: 'mboc.error.excel-value-is-required',
                            params: [{name: 'header', value: 'Размер'}],
                            text: "Отсутствует значение для колонки 'Размер'",
                            level: 2,
                            details: '{"header":"Размер"}',
                        },
                        isRelevant: true,
                    },
                    {
                        identifiers: [
                            {
                                shopId: 11138645,
                                warehouseId: 48585,
                                offerId: '123',
                                businessId: 10964073,
                            },
                            {
                                shopId: 11138646,
                                warehouseId: 48585,
                                offerId: '123',
                                businessId: 10964073,
                            },
                        ],
                        explanation: {
                            namespace: 'mboc.ci.error',
                            code: 'mboc.error.excel-value-is-required',
                            params: [{name: 'header', value: 'Диаметр'}],
                            text: "Отсутствует значение для колонки 'Диаметр'",
                            level: 2,
                            details: '{"header":"Диаметр"}',
                        },
                        isRelevant: true,
                    },
                ],
            },
        },
    ],
};
