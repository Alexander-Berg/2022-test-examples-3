import {TRAIN_TYPE} from '../../transportType';
import {PLATZKARTE, COMPARTMENT, SUITE} from '../tariffClasses';

import CurrencyCode from '../../../interfaces/CurrencyCode';

import {
    filterTariffClassKeysByTrainTariffClass,
    filterTariffClassKeys,
} from '../getBaseTariffClassKeys';

const transport = {code: TRAIN_TYPE};
const currency = CurrencyCode.rub;
const compartment = {
    [COMPARTMENT]: {
        nationalPrice: {
            currency,
            value: 200,
        },
    },
};
const platzkarte = {
    [PLATZKARTE]: {
        nationalPrice: {
            currency,
            value: 100,
        },
    },
};
const suite = {
    [SUITE]: {
        nationalPrice: {
            currency,
            value: 10000,
        },
    },
};

describe('getBaseTariffClassKeys', () => {
    describe('filterTariffClassKeysByTrainTariffClass', () => {
        it('Показываем только compartment', () => {
            const segment = {
                title: 'Moscow - Omsk',
                transport,
                tariffs: {
                    classes: {
                        ...compartment,
                        ...platzkarte,
                    },
                },
            };
            const filtersData = {
                trainTariffClass: {
                    value: [COMPARTMENT],
                },
            };
            const result = filterTariffClassKeysByTrainTariffClass(
                [COMPARTMENT],
                segment,
                filtersData,
            );

            expect(result).toEqual([COMPARTMENT]);
        });

        it('Тариф СВ будет прокинут дальше, т.к. только он подходит под фильтр', () => {
            const segment = {
                title: 'Moscow - Omsk',
                transport,
                tariffs: {
                    classes: {
                        ...compartment,
                        ...platzkarte,
                    },
                },
            };
            const filtersData = {
                trainTariffClass: {
                    value: [SUITE],
                },
            };
            const result = filterTariffClassKeysByTrainTariffClass(
                [COMPARTMENT, SUITE],
                segment,
                filtersData,
            );

            expect(result).toEqual([SUITE]);
        });
    });

    describe('filterTariffClassKeys', () => {
        it('Фильтры не определены', () => {
            const segment = {
                title: 'Moscow - Omsk',
                transport,
                tariffs: {
                    classes: {
                        ...compartment,
                        ...platzkarte,
                        ...suite,
                    },
                },
            };
            const tariffClassKeys = [COMPARTMENT, SUITE, PLATZKARTE];
            const result = filterTariffClassKeys({
                tariffClassKeys,
                segment,
            });

            expect(result).toEqual(tariffClassKeys);
        });

        it('Возврат списка тарифов для показа в сниппете', () => {
            const segment = {
                title: 'Moscow - Omsk',
                transport,
                tariffs: {
                    classes: {
                        ...compartment,
                        ...platzkarte,
                        ...suite,
                    },
                },
            };
            const filtering = {
                filters: {
                    priceRange: {
                        value: [
                            {
                                min: 300,
                                max: 12000,
                            },
                        ],
                    },
                    trainTariffClass: {
                        value: [SUITE],
                    },
                },
            };
            const result = filterTariffClassKeys({
                tariffClassKeys: [COMPARTMENT, SUITE, PLATZKARTE],
                segment,
                filtering,
            });

            expect(result).toEqual([SUITE]);
        });
    });
});
