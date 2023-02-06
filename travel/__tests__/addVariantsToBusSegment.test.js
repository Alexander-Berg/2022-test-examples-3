import {YBUS} from '../tariffSources';

jest.disableAutomock();

import {BUS_TYPE} from '../../transportType';

import {
    addVariantsToBusSegment,
    buildVariant,
    getNewVariantsObject,
    getVariantKey,
} from '../addVariantsToBusSegment';

const busSegment = {
    transport: {
        code: BUS_TYPE,
    },
};

const yaSegment = {
    ...busSegment,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 1000,
                    currency: 'RUR',
                },
                orderUrl: 'http://ya.ru',
            },
        },
    },
};
const eSegment = {
    ...busSegment,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 1100,
                    currency: 'RUR',
                },
                orderUrl: 'http://etraffic.ru',
            },
        },

        supplier: {
            code: 'etraffic',
            url: '/data/e.png',
        },
    },
};
const newSupplierSegment = {
    ...busSegment,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 100,
                    currency: 'RUR',
                },
                orderUrl: 'http://newsupplier.ru',
            },
        },

        supplier: {
            code: 'newsupplier',
            url: '/data/new.png',
        },
    },
};

const segmentWithVariants = {
    ...busSegment,
    hasVariants: true,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 1000,
                    currency: 'RUR',
                },

                variants: {
                    ybus: {
                        price: {
                            value: 1000,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://ya.ru',
                    },

                    etraffic: {
                        price: {
                            value: 1100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://etraffic.ru',

                        supplier: {
                            code: 'etraffic',
                            url: '/data/e.png',
                        },
                    },
                },
                orderUrl: 'http://ya.ru',
            },
        },
    },
};
const segmentWithExpensiveVariants = {
    ...busSegment,
    hasVariants: true,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 9000,
                    currency: 'RUR',
                },

                variants: {
                    ybus: {
                        price: {
                            value: 9000,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://ya.ru',
                    },

                    etraffic: {
                        price: {
                            value: 9100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://etraffic.ru',

                        supplier: {
                            code: 'etraffic',
                            url: '/data/e.png',
                        },
                    },
                },
                orderUrl: 'http://ya.ru',
            },
        },
    },
};
const segmentWithMixedVariants = {
    ...busSegment,
    hasVariants: true,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 9000,
                    currency: 'RUR',
                },

                variants: {
                    ybus: {
                        price: {
                            value: 9000,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://ya.ru',
                    },

                    etraffic: {
                        price: {
                            value: 1100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://etraffic.ru',

                        supplier: {
                            code: 'etraffic',
                            url: '/data/e.png',
                        },
                    },
                },
                orderUrl: 'http://ya.ru',
            },
        },
    },
};
const segmentWithMixedVariantsEAsMain = {
    ...busSegment,
    hasVariants: true,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 1100,
                    currency: 'RUR',
                },

                variants: {
                    ybus: {
                        price: {
                            value: 9000,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://ya.ru',
                    },

                    etraffic: {
                        price: {
                            value: 1100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://etraffic.ru',

                        supplier: {
                            code: 'etraffic',
                            url: '/data/e.png',
                        },
                    },
                },
                orderUrl: 'http://etraffic.ru',
            },
        },

        supplier: {
            code: 'etraffic',
            url: '/data/e.png',
        },
    },
};

const segmentWithAllVariants = {
    ...busSegment,
    hasVariants: true,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 1000,
                    currency: 'RUR',
                },

                variants: {
                    ybus: {
                        price: {
                            value: 1000,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://ya.ru',
                    },

                    etraffic: {
                        price: {
                            value: 1100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://etraffic.ru',

                        supplier: {
                            code: 'etraffic',
                            url: '/data/e.png',
                        },
                    },

                    newsupplier: {
                        price: {
                            value: 100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://newsupplier.ru',

                        supplier: {
                            code: 'newsupplier',
                            url: '/data/new.png',
                        },
                    },
                },
                orderUrl: 'http://ya.ru',
            },
        },
    },
};
const segmentWithAllExpensiveVariants = {
    ...busSegment,
    hasVariants: true,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 8000,
                    currency: 'RUR',
                },

                variants: {
                    ybus: {
                        price: {
                            value: 8000,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://ya.ru',
                    },

                    etraffic: {
                        price: {
                            value: 8100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://etraffic.ru',

                        supplier: {
                            code: 'etraffic',
                            url: '/data/e.png',
                        },
                    },

                    newsupplier: {
                        price: {
                            value: 7100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://newsupplier.ru',

                        supplier: {
                            code: 'newsupplier',
                            url: '/data/new.png',
                        },
                    },
                },
                orderUrl: 'http://ya.ru',
            },
        },
    },
};
const segmentWithAllMixedVariants = {
    ...busSegment,
    hasVariants: true,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 1000,
                    currency: 'RUR',
                },

                variants: {
                    ybus: {
                        price: {
                            value: 1000,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://ya.ru',
                    },

                    etraffic: {
                        price: {
                            value: 1100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://etraffic.ru',

                        supplier: {
                            code: 'etraffic',
                            url: '/data/e.png',
                        },
                    },

                    newsupplier: {
                        price: {
                            value: 7100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://newsupplier.ru',

                        supplier: {
                            code: 'newsupplier',
                            url: '/data/new.png',
                        },
                    },
                },
                orderUrl: 'http://ya.ru',
            },
        },
    },
};
const segmentWithAllVariantsNewAsMain = {
    ...busSegment,
    hasVariants: true,
    tariffs: {
        classes: {
            bus: {
                price: {
                    value: 100,
                    currency: 'RUR',
                },

                variants: {
                    ybus: {
                        price: {
                            value: 1000,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://ya.ru',
                    },

                    etraffic: {
                        price: {
                            value: 1100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://etraffic.ru',

                        supplier: {
                            code: 'etraffic',
                            url: '/data/e.png',
                        },
                    },

                    newsupplier: {
                        price: {
                            value: 100,
                            currency: 'RUR',
                        },
                        orderUrl: 'http://newsupplier.ru',

                        supplier: {
                            code: 'newsupplier',
                            url: '/data/new.png',
                        },
                    },
                },
                orderUrl: 'http://newsupplier.ru',
            },
        },

        supplier: {
            code: 'newsupplier',
            url: '/data/new.png',
        },
    },
};

const yaVariant = {
    price: {
        value: 1000,
        currency: 'RUR',
    },
    orderUrl: 'http://ya.ru',
};
const eVariant = {
    price: {
        value: 1100,
        currency: 'RUR',
    },
    orderUrl: 'http://etraffic.ru',

    supplier: {
        code: 'etraffic',
        url: '/data/e.png',
    },
};

const yaBasedVariants = {
    ybus: {
        price: {
            value: 1000,
            currency: 'RUR',
        },
        orderUrl: 'http://ya.ru',
    },
};
const eBasedVariants = {
    etraffic: {
        price: {
            value: 1100,
            currency: 'RUR',
        },
        orderUrl: 'http://etraffic.ru',

        supplier: {
            code: 'etraffic',
            url: '/data/e.png',
        },
    },
};

describe('addVariantsToBusSegment', () => {
    it('Вернёт оригинальный сегмент: в случае одинаковых сегментов', () => {
        const result = addVariantsToBusSegment(yaSegment, yaSegment);

        expect(result).toBe(yaSegment);
    });

    it('Вернет оригинальный сегмент: в случае отсутствия тарифов у одного из сегментов', () => {
        const result = addVariantsToBusSegment(yaSegment, busSegment);

        expect(result).toBe(yaSegment);
    });

    it('Вернет сегмент с вариантами: случай, когда вариантов нет ни у одного из сегментов', () => {
        const result = addVariantsToBusSegment(yaSegment, eSegment);

        expect(result).toEqual(segmentWithVariants);
    });

    it('Вернет сегмент с вариантами: случай, когда варианты есть у базового сегмента, нет пересечений по ключам', () => {
        const result = addVariantsToBusSegment(
            segmentWithVariants,
            newSupplierSegment,
        );

        expect(result).toEqual(segmentWithAllVariants);
    });

    it('Вернет сегмент с вариантами: случай, когда варианты есть у базового сегмента, есть пересечения по ключам, перезапись запрещена', () => {
        const result = addVariantsToBusSegment(
            segmentWithExpensiveVariants,
            eSegment,
            false,
        );

        expect(result).toEqual(segmentWithExpensiveVariants);
    });

    it('Вернет сегмент с вариантами: случай, когда варианты есть у базового сегмента, есть пересечения по ключам, перезапись разрешена', () => {
        const result = addVariantsToBusSegment(
            segmentWithExpensiveVariants,
            eSegment,
        );

        expect(result).toEqual(segmentWithMixedVariants);
    });

    it('Вернет сегмент с вариантами: случай, когда варианты есть у добавляемого сегмента, нет пересечений по ключам', () => {
        const result = addVariantsToBusSegment(
            newSupplierSegment,
            segmentWithVariants,
        );

        expect(result).toEqual(segmentWithAllVariantsNewAsMain);
    });

    it('Вернет сегмент с вариантами: случай, когда варианты есть у добавляемого сегмента, есть пересечения по ключам', () => {
        const result = addVariantsToBusSegment(
            eSegment,
            segmentWithExpensiveVariants,
        );

        expect(result).toEqual(segmentWithMixedVariantsEAsMain);
    });

    it('Вернет сегмент с вариантами: случай, когда варианты есть у обеих сегментов с пересечением ключей', () => {
        const result = addVariantsToBusSegment(
            segmentWithVariants,
            segmentWithAllExpensiveVariants,
        );

        expect(result).toEqual(segmentWithAllMixedVariants);
    });
});

describe('buildVariant', () => {
    it('Вернет null, если аргумет входной тариф приводится к false', () => {
        const result = buildVariant(false);

        expect(result).toBeNull();
    });

    it('Вернет вариант для Я.Автобусов, если на вход приходит тариф без supplier', () => {
        const result = buildVariant(yaSegment.tariffs);

        expect(result).toEqual(yaVariant);
    });

    it('Вернет вариант с информацией о партнере, если на вход приходит тариф с supplier', () => {
        const result = buildVariant(eSegment.tariffs);

        expect(result).toEqual(eVariant);
    });
});

describe('getVariantKey', () => {
    it('Вернет null, если входной тариф приводится к false', () => {
        const result = getVariantKey(false);

        expect(result).toBeNull();
    });

    it('Вернет ключ Я.Автобусов, если в тарифе нет кода поставщика', () => {
        const result = getVariantKey(yaSegment.tariffs);

        expect(result).toBe(YBUS);
    });

    it('Вернет ключ поставщика, если он есть в тарифе', () => {
        const result = getVariantKey(eSegment.tariffs);

        expect(result).toBe(eSegment.tariffs.supplier.code);
    });
});

describe('getNewVariantsObject', () => {
    it('Вернет пустой объект, если входной тариф приводится к false', () => {
        const result = getNewVariantsObject(false);

        expect(result).toEqual({});
    });

    it('Вернет объект с ключом ybusесли на вход приходит тариф без supplier', () => {
        const result = getNewVariantsObject(yaSegment.tariffs);

        expect(result).toEqual(yaBasedVariants);
    });

    it('Вернет объект с ключом поставщика, если он есть в тарифе', () => {
        const result = getNewVariantsObject(eSegment.tariffs);

        expect(result).toEqual(eBasedVariants);
    });
});
