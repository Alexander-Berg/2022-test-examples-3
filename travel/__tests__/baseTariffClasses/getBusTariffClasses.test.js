jest.dontMock('../../tariffClasses');
jest.dontMock('../../tariffSources');
jest.dontMock('../../../transportType');

const {YBUS} = require.requireActual('../../tariffSources');
const {BUS_TYPE} = require.requireActual('../../../transportType');
const {TARIFF_CLASSES_BY_TYPE} = require.requireActual('../../tariffClasses');
const {getBusTariffClassKeys} = require.requireActual(
    '../../getBaseTariffClassKeys',
);

const getTariffs = (tariffs, seats) => ({
    classes: tariffs.reduce(
        (dict, item) => ({
            ...dict,
            [item]: {seats},
        }),
        {},
    ),
});

describe('getBusTariffClassKeys', () => {
    it('вернёт пустой список ключей если у сегмента нет цен', () =>
        expect(
            getBusTariffClassKeys({
                tariffs: null,
                transport: {
                    code: BUS_TYPE,
                },
            }),
        ).toEqual([]));

    it('если поставщик данных не "Яндекс.Автобусы", то вернётся список тарифов без учёта мест', () =>
        expect(
            getBusTariffClassKeys({
                tariffs: getTariffs(TARIFF_CLASSES_BY_TYPE[BUS_TYPE]),
                transport: {
                    code: BUS_TYPE,
                },
            }),
        ).toEqual(TARIFF_CLASSES_BY_TYPE[BUS_TYPE]));

    it('если поставщик данных "Яндекс.Автобусы", то вернётся список тарифов с местами', () => {
        expect(
            getBusTariffClassKeys({
                tariffs: getTariffs(TARIFF_CLASSES_BY_TYPE[BUS_TYPE], 23),
                source: YBUS,
                transport: {
                    code: BUS_TYPE,
                },
            }),
        ).toEqual(TARIFF_CLASSES_BY_TYPE[BUS_TYPE]);
    });

    it('если поставщик данных "Яндекс.Автобусы" и нет тарифов с местами - вернётся пустой список', () => {
        expect(
            getBusTariffClassKeys({
                tariffs: getTariffs(TARIFF_CLASSES_BY_TYPE[BUS_TYPE], 0),
                source: YBUS,
                transport: {
                    code: BUS_TYPE,
                },
            }),
        ).toEqual([]);
    });
});
