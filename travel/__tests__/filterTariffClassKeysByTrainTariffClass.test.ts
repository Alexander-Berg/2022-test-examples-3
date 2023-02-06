import {TRAIN_COACH_TYPE} from 'projects/trains/constants/coachType';

import {ITrainsFilters} from 'types/trains/search/filters/ITrainsFilters';

import filterTariffClassKeysByTrainTariffClass from '../filterTariffClassKeysByTrainTariffClass';

describe('filterTariffClassKeysByTrainTariffClass', () => {
    it('Показываем только compartment', () => {
        const filtersData = {
            trainTariffClass: {
                value: [TRAIN_COACH_TYPE.COMPARTMENT],
            },
        } as ITrainsFilters;
        const result = filterTariffClassKeysByTrainTariffClass(
            [TRAIN_COACH_TYPE.COMPARTMENT],
            filtersData,
        );

        expect(result).toEqual([TRAIN_COACH_TYPE.COMPARTMENT]);
    });

    it('Тариф СВ будет прокинут дальше, т.к. только он подходит под фильтр', () => {
        const filtersData = {
            trainTariffClass: {
                value: [TRAIN_COACH_TYPE.SUITE],
            },
        } as ITrainsFilters;
        const result = filterTariffClassKeysByTrainTariffClass(
            [TRAIN_COACH_TYPE.COMPARTMENT, TRAIN_COACH_TYPE.SUITE],
            filtersData,
        );

        expect(result).toEqual([TRAIN_COACH_TYPE.SUITE]);
    });
});
