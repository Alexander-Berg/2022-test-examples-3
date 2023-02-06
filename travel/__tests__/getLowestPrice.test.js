const sortTariffClassKeys = jest.fn();
const getBusTariffClassKeys = jest.fn();
const getBaseTariffClassKeys = jest.fn();

jest.setMock('../getBaseTariffClassKeys', {
    sortTariffClassKeys,
    getBusTariffClassKeys,
    getBaseTariffClassKeys,
});

const {BUS, SUITE, COMPARTMENT, SITTING} =
    require.requireActual('../tariffClasses');
const {BUS_TYPE, TRAIN_TYPE} = require.requireActual('../../transportType');

const getLowestPrice = require.requireActual('../getLowestPrice').default;

const trainSegment = {
    tariffs: {
        classes: {
            [SUITE]: {
                price: {
                    value: 5400,
                },
            },
            [SITTING]: {
                price: {
                    value: 1200,
                },
            },
        },
    },
    transport: {
        code: TRAIN_TYPE,
    },
};

const busSegment = {
    tariffs: {
        classes: {
            [BUS]: {
                price: {
                    value: 780,
                },
            },
        },
    },
    transport: {
        code: BUS_TYPE,
    },
};

describe('getLowestPrice', () => {
    it('вернёт null если у сегмента нет тарифов', () => {
        getBaseTariffClassKeys.mockReturnValueOnce([]);

        expect(
            getLowestPrice({
                ...trainSegment,
                tariffs: null,
            }),
        ).toEqual(null);
    });

    it('вернёт объект с минимальной ценой для поезда', () => {
        getBaseTariffClassKeys.mockReturnValueOnce([SUITE, SITTING]);
        sortTariffClassKeys.mockReturnValueOnce([SITTING, SUITE]);

        expect(getLowestPrice(trainSegment)).toEqual({
            value: 1200,
            class: SITTING,
        });
    });

    it('вернёт объект с минимальной ценой для автобуса', () => {
        getBusTariffClassKeys.mockReturnValueOnce([BUS]);
        sortTariffClassKeys.mockReturnValueOnce([BUS]);

        expect(getLowestPrice(busSegment)).toEqual({
            value: 780,
            class: BUS,
        });
    });

    it('sortTariffClassKeys вызовется с заданными тарифами', () => {
        getBaseTariffClassKeys.mockReturnValueOnce([SUITE, SITTING]);
        sortTariffClassKeys.mockReturnValueOnce([SITTING, SUITE]);

        getLowestPrice(trainSegment, [SITTING, SUITE]);

        expect(sortTariffClassKeys).toHaveBeenLastCalledWith({
            segment: trainSegment,
            sort: {by: 'price'},
            tariffClassKeys: [SITTING, SUITE],
        });
    });

    it('Вернет null, если заданных тарифов нету у сегмента', () => {
        getBaseTariffClassKeys.mockReturnValueOnce([SUITE, SITTING]);

        expect(getLowestPrice(trainSegment, [COMPARTMENT])).toBe(null);
    });
});
