jest.disableAutomock();

import {TRAIN_TYPE, PLANE_TYPE} from '../../transportType';

import areAllSegmentsWithPrice from '../areAllSegmentsWithPrice';

const trainSegmentWithPrice = {
    transport: {
        code: TRAIN_TYPE,
    },
    tariffs: {
        classes: {
            compartment: {
                seats: 8,
                price: {
                    currency: 'RUB',
                    value: 3674,
                },
            },
        },
    },
};
const trainsSegmentWithoutPrice = {
    ...trainSegmentWithPrice,
    tariffs: undefined,
};
const planeSegmentWithPrice = {
    transport: {
        code: PLANE_TYPE,
    },
    tariffs: {
        classes: {
            economy: {
                seats: 2,
                price: {
                    currency: 'RUB',
                    value: 6666,
                },
            },
        },
    },
};
const planeSegmentWithoutPrice = {
    ...planeSegmentWithPrice,
    tariffs: undefined,
};
const trainSegments = [
    trainSegmentWithPrice,
    trainSegmentWithPrice,
    trainSegmentWithPrice,
];

const mixedSegmentsWithPrice = [
    trainSegmentWithPrice,
    planeSegmentWithPrice,
    trainSegmentWithPrice,
    planeSegmentWithPrice,
];

describe('areAllSegmentsWithPrice', () => {
    it('Смешанная выдача. Проверка по всем типам. У всех сегментов есть цена. Вернет true.', () => {
        expect(areAllSegmentsWithPrice(mixedSegmentsWithPrice)).toBe(true);
    });

    it('Смешанная выдача. Проверка по всем типам. Есть сегмент без цены. Вернет false.', () => {
        expect(
            areAllSegmentsWithPrice([
                ...mixedSegmentsWithPrice,
                planeSegmentWithoutPrice,
            ]),
        ).toBe(false);
    });

    it('Смешанная выдача. Проверка по конкретному типу. Все сегменты этого типа с ценой. Вернет true.', () => {
        expect(
            areAllSegmentsWithPrice(
                [...mixedSegmentsWithPrice, planeSegmentWithoutPrice],
                TRAIN_TYPE,
            ),
        ).toBe(true);
    });

    it('Смешанная выдача. Проверка по конкретному типу. Есть сегмент этого типа без цены. Вернет false.', () => {
        expect(
            areAllSegmentsWithPrice(
                [...mixedSegmentsWithPrice, trainsSegmentWithoutPrice],
                TRAIN_TYPE,
            ),
        ).toBe(false);
    });

    it('Выдача с одним типом транспорта. Все сегменты с ценой. Вернет true.', () => {
        expect(areAllSegmentsWithPrice(trainSegments)).toBe(true);
    });

    it('Выдача с одним типом транспорта. Все сегменты с ценой. Проверка по другому типу транспорта. Вернет false.', () => {
        expect(areAllSegmentsWithPrice(trainSegments, PLANE_TYPE)).toBe(false);
    });

    it('Пустая выдача. Вернет false.', () => {
        expect(areAllSegmentsWithPrice([])).toBe(false);
    });
});
