const {PLATZKARTE, TRAIN_TARIFF_CLASSES} =
    require.requireActual('../tariffClasses');

const sortTariffClassKeys = jest.fn(() => TRAIN_TARIFF_CLASSES);

jest.setMock('../getBaseTariffClassKeys', {
    sortTariffClassKeys,
});

const getTariffKeys = require.requireActual('../getTariffKeys').default;

describe('getTariffKeys', () => {
    it('Если у сегмента нет тарифов - вернёт пустую структуру', () => {
        const expectedData = {
            tariffKeys: [],
            mainTariffKey: null,
        };

        expect(getTariffKeys({})).toEqual(expectedData);
        expect(
            getTariffKeys({
                tariffs: {},
            }),
        ).toEqual(expectedData);
    });

    it('Если у тарифов нет ссылок на покупку - вернём структуру без главного ключа', () => {
        expect(
            getTariffKeys({
                tariffs: {
                    classes: TRAIN_TARIFF_CLASSES.reduce(
                        (tariffs, key) => ({
                            ...tariffs,
                            [key]: {seats: 50},
                        }),
                        {},
                    ),
                },
            }),
        ).toEqual({
            tariffKeys: TRAIN_TARIFF_CLASSES,
            mainTariffKey: null,
        });
    });

    it('Если у тарифов есть ссылки на покупку - вернём структуру с главным ключом', () => {
        expect(
            getTariffKeys({
                tariffs: {
                    classes: TRAIN_TARIFF_CLASSES.reduce(
                        (tariffs, key) => ({
                            ...tariffs,
                            [key]: {
                                seats: 50,
                                orderUrl: `https://rasp.yandex.ru/order/${key}`,
                            },
                        }),
                        {},
                    ),
                },
            }),
        ).toEqual({
            tariffKeys: TRAIN_TARIFF_CLASSES,
            mainTariffKey: {
                seats: 50,
                orderUrl: `https://rasp.yandex.ru/order/${PLATZKARTE}`,
            },
        });
    });
});
