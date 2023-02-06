jest.disableAutomock();

import {ECONOMY, TARIFF_CLASSES_BY_TYPE} from '../../tariffClasses';
import {
    BUS_TYPE,
    TRAIN_TYPE,
    PLANE_TYPE,
    WATER_TYPE,
    SUBURBAN_TYPE,
} from '../../../transportType';
import {getBaseTariffClassKeys} from '../../getBaseTariffClassKeys';

const getTariffs = (tariffs, seats) => ({
    classes: tariffs.reduce(
        (dict, item) => ({
            ...dict,
            [item]: {seats},
        }),
        {},
    ),
});

const busSegment = {transport: {code: BUS_TYPE}};
const planeSegment = {transport: {code: PLANE_TYPE}};
const waterSegment = {transport: {code: WATER_TYPE}};
const trainsSegment = {transport: {code: TRAIN_TYPE}};
const suburbanSegment = {transport: {code: SUBURBAN_TYPE}};

describe('getBaseTariffClassKeys', () => {
    it('вернёт пустой список ключей если у сегмента нет цен', () => {
        expect(
            getBaseTariffClassKeys({
                ...trainsSegment,
                tariffs: null,
            }),
        ).toEqual([]);
    });

    it('вернёт автобусные тарифы', () =>
        expect(
            getBaseTariffClassKeys({
                ...busSegment,
                tariffs: getTariffs(TARIFF_CLASSES_BY_TYPE[BUS_TYPE]),
            }),
        ).toEqual(TARIFF_CLASSES_BY_TYPE[BUS_TYPE]));

    it('вернёт железнодорожные тарифы', () =>
        expect(
            getBaseTariffClassKeys({
                ...trainsSegment,
                tariffs: getTariffs(TARIFF_CLASSES_BY_TYPE[TRAIN_TYPE], 10),
            }),
        ).toEqual(TARIFF_CLASSES_BY_TYPE[TRAIN_TYPE]));

    it('если не осталось мест ни в одном железнодорожном тарифе - вернёт пустой список', () =>
        expect(
            getBaseTariffClassKeys({
                ...trainsSegment,
                tariffs: getTariffs(TARIFF_CLASSES_BY_TYPE[TRAIN_TYPE], 0),
            }),
        ).toEqual([]));

    it('если в сегменте присутствуют неизвестные тарифы - они будут проигнорированы', () => {
        const tariffs = getTariffs(
            [...TARIFF_CLASSES_BY_TYPE[TRAIN_TYPE], 'cab'],
            10,
        );

        expect(
            getBaseTariffClassKeys({
                ...trainsSegment,
                tariffs,
            }),
        ).toEqual(TARIFF_CLASSES_BY_TYPE[TRAIN_TYPE]);
    });

    it('вернёт самолётный тариф в порядке приоритета', () => {
        expect(
            getBaseTariffClassKeys({
                ...planeSegment,
                tariffs: getTariffs(TARIFF_CLASSES_BY_TYPE[PLANE_TYPE]),
            }),
        ).toEqual([ECONOMY]);
    });

    it('вернёт электричечные тарифы', () =>
        expect(
            getBaseTariffClassKeys({
                ...suburbanSegment,
                tariffs: getTariffs(TARIFF_CLASSES_BY_TYPE[SUBURBAN_TYPE]),
            }),
        ).toEqual(TARIFF_CLASSES_BY_TYPE[SUBURBAN_TYPE]));

    it('Если для электрички доступны жд тарифы - вернёт список имеющихся жд тарифов', () => {
        const tariffs = getTariffs(TARIFF_CLASSES_BY_TYPE[TRAIN_TYPE], 10);

        expect(
            getBaseTariffClassKeys({
                ...suburbanSegment,
                hasTrainTariffs: true,
                tariffs,
            }),
        ).toEqual(TARIFF_CLASSES_BY_TYPE[TRAIN_TYPE]);
    });

    it('для других типов транспорта вернёт нефиксированный список тарифов', () => {
        const tariffs = getTariffs(['submarine', 'galera']);

        expect(
            getBaseTariffClassKeys({
                ...waterSegment,
                tariffs,
            }),
        ).toEqual(['submarine', 'galera']);
    });
});
