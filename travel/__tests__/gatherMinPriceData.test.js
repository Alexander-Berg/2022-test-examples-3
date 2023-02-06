import {
    BUS_TYPE,
    TRAIN_TYPE,
    PLANE_TYPE,
    SUBURBAN_TYPE,
} from '../../transportType';
import {BUS, SUBURBAN} from '../tariffClasses';

import CurrencyCode from '../../../interfaces/CurrencyCode';

import gatherMinPriceData from '../gatherMinPriceData';

const getSegments = isGone => [
    {
        tariffs: {
            classes: {
                bus: {
                    price: {
                        currency: CurrencyCode.rub,
                        value: 4990,
                    },
                },
            },
        },
        isGone: Boolean(isGone),
        transport: {
            code: BUS_TYPE,
        },
    },
    {
        tariffs: {
            classes: {
                suite: {
                    price: {
                        currency: CurrencyCode.rub,
                        value: 8800,
                    },
                },
            },
        },
        isGone: Boolean(isGone),
        transport: {
            code: TRAIN_TYPE,
        },
    },
    {
        tariffs: {
            classes: {
                economy: {
                    price: {
                        currency: CurrencyCode.rub,
                        value: 12450,
                    },
                },
            },
        },
        isGone: Boolean(isGone),
        transport: {
            code: PLANE_TYPE,
        },
    },
    {
        tariffs: {
            classes: {
                suburban: {
                    price: {
                        currency: CurrencyCode.rub,
                        value: 124,
                    },
                },
            },
        },
        isGone: Boolean(isGone),
        transport: {
            code: SUBURBAN_TYPE,
        },
    },
];

describe('gatherMinPriceData', () => {
    it('вернёт null для пустого списка сегментов', () => {
        expect(gatherMinPriceData([])).toEqual(null);
    });

    it('вернёт null для ушедших сегментов', () => {
        expect(gatherMinPriceData(getSegments(true))).toEqual(null);
    });

    it('вернёт null для сегментов без цен', () => {
        expect(
            gatherMinPriceData(
                getSegments().map(segment => ({
                    ...segment,
                    tariffs: null,
                })),
            ),
        ).toEqual(null);
    });

    it('вернёт минимальную цену среди всех сегментов', () => {
        expect(gatherMinPriceData(getSegments())).toEqual({
            transportType: SUBURBAN_TYPE,
            price: {
                class: SUBURBAN,
                currency: CurrencyCode.rub,
                value: 124,
            },
        });
    });

    it('вернёт минимальную цену среди неушедших сегментов', () => {
        expect(
            gatherMinPriceData(
                getSegments().map(segment => ({
                    ...segment,
                    isGone: segment.transport.code === SUBURBAN_TYPE,
                })),
            ),
        ).toEqual({
            transportType: BUS_TYPE,
            price: {
                class: BUS,
                currency: CurrencyCode.rub,
                value: 4990,
            },
        });
    });
});
