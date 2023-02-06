import {
    BUS_TYPE,
    PLANE_TYPE,
    TRAIN_TYPE,
    SUBURBAN_TYPE,
} from '../../../transportType';
import {TARIFF_CLASSES_BY_TYPE} from '../../tariffClasses';

import DateSpecialValue from '../../../../interfaces/date/DateSpecialValue';

import {
    getTariffsType,
    isSuburbanTariffs,
    shouldShowSuburbanTariffs,
} from '../tariffs';

const buildClasses = type => ({
    classes: TARIFF_CLASSES_BY_TYPE[type].reduce(
        (res, item) => ({
            ...res,
            [item]: {},
        }),
        {},
    ),
});

const TRANSPORT_TYPES = [BUS_TYPE, PLANE_TYPE, TRAIN_TYPE, SUBURBAN_TYPE];

const typeTariffs = {
    [BUS_TYPE]: buildClasses(BUS_TYPE),
    [PLANE_TYPE]: buildClasses(PLANE_TYPE),
    [TRAIN_TYPE]: buildClasses(TRAIN_TYPE),
    [SUBURBAN_TYPE]: buildClasses(SUBURBAN_TYPE),
};

describe('getTariffsType', () => {
    describe('вернёт null если ', () => {
        it('не определены тарифы', () => {
            expect(getTariffsType()).toBeNull();
        });

        it('не определены классы тарифов', () => {
            expect(getTariffsType({})).toBeNull();
        });

        it('классы тарифов пустые', () => {
            expect(
                getTariffsType({
                    classes: {},
                }),
            ).toBeNull();
        });

        it('классы не соответствуют ни одному из типов', () => {
            expect(
                getTariffsType({
                    classes: {
                        donkey: {},
                        elephant: {},
                    },
                }),
            ).toBeNull();
        });
    });

    TRANSPORT_TYPES.forEach(type => {
        it(`вернёт для ${type} тарифов соответствующий тип транспорта`, () => {
            expect(getTariffsType(typeTariffs[type])).toBe(type);
        });
    });
});

describe('isSuburbanTariffs', () => {
    it('Если тарифы не соответствуют электричечным - вернёт false', () => {
        expect(isSuburbanTariffs(typeTariffs[TRAIN_TYPE])).toBe(false);
    });

    it('Если тарифы соответствуют электричечным - вернёт true', () => {
        expect(isSuburbanTariffs(typeTariffs[SUBURBAN_TYPE])).toBe(true);
    });
});

describe('shouldShowSuburbanTariffs', () => {
    const allDaysContext = {
        when: {
            special: DateSpecialValue.allDays,
        },
    };
    const dateContext = {
        when: {
            text: 'завтра',
        },
    };

    const segmentWithSuburbanTariffs = {
        tariffs: typeTariffs[SUBURBAN_TYPE],
    };

    it('Если сегмент содержит не электричечные тарифы - вернёт false', () => {
        expect(
            shouldShowSuburbanTariffs(allDaysContext, {
                tariffs: typeTariffs[TRAIN_TYPE],
            }),
        ).toBe(false);
    });

    it('Для поисков на все дни - вернёт true', () => {
        expect(
            shouldShowSuburbanTariffs(
                allDaysContext,
                segmentWithSuburbanTariffs,
            ),
        ).toBe(true);
    });

    describe('Для поисков на дату: ', () => {
        it('если электричка без возможности продажи - вернёт true', () => {
            expect(
                shouldShowSuburbanTariffs(
                    dateContext,
                    segmentWithSuburbanTariffs,
                ),
            ).toBe(true);
        });

        it('если электричка с возможностью продажи и опрос цен ещё не окончился - вернёт false', () => {
            expect(
                shouldShowSuburbanTariffs(dateContext, {
                    ...segmentWithSuburbanTariffs,
                    hasTrainTariffs: true,
                    queryingPrices: true,
                }),
            ).toBe(false);
        });

        it('если электричка с возможностью продажи но не удалось получить жд тарифы - вернёт true', () => {
            expect(
                shouldShowSuburbanTariffs(dateContext, {
                    ...segmentWithSuburbanTariffs,
                    hasTrainTariffs: true,
                    queryingPrices: false,
                }),
            ).toBe(true);
        });
    });
});
