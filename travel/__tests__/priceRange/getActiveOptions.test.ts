import {ITrainsFilters} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import {CURRENCY_RUB} from 'utilities/currency/codes';

import priceRange from '../../priceRange';

const currency = CURRENCY_RUB;

describe('priceRange', () => {
    describe('getActiveOptions', () => {
        it('Получение списка доступных опций', () => {
            const segments = [
                {
                    tariffs: {
                        classes: {
                            compartment: {
                                price: {
                                    currency,
                                    value: 150,
                                },
                            },
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            compartment: {
                                price: {
                                    currency,
                                    value: 250,
                                },
                            },
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            compartment: {
                                price: {
                                    currency,
                                    value: 350,
                                },
                            },
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            compartment: {
                                price: {
                                    currency,
                                    value: 450,
                                },
                            },
                        },
                    },
                },
            ] as ITrainsTariffApiSegment[];
            // @ts-ignore
            const filtersData = {
                someOtherFilter: {
                    filteredSegmentIndices: [
                        true,
                        false, // второй сегмент как бы отфильтрован
                        true,
                        true,
                    ],
                },
            } as ITrainsFilters;
            const newOptionsForSegments = [
                {
                    count: 1,
                    min: 100,
                    max: 200,
                    currency,
                    value: '100-200',
                },
                {
                    count: 1,
                    min: 300,
                    max: 400,
                    currency,
                    value: '300-400',
                },
                {
                    count: 1,
                    min: 400,
                    max: 500,
                    currency,
                    value: '400-500',
                },
            ];
            const result = priceRange.getActiveOptions({filtersData, segments});

            expect(result).toEqual(newOptionsForSegments);
        });

        it('Получение списка доступных опций. Случай, когда выбран тип вагона.', () => {
            /*
            Например в сегменте с поездом есть три типа тарифа: плацкарт, купе и СВ с ценами,
            соответственно 2000, 5000 и 15000 рублей. Если выбрать тип вагона "СВ", то
            сам сниппет с поездом остается, но из него исчезнут неподходящие тарифы.
            Вместе с этим нам нужно сделать неактивными неподходящие под оставшиеся цены диапазоны
             */
            const segments = [
                {
                    tariffs: {
                        classes: {
                            platzkarte: {
                                price: {
                                    currency,
                                    value: 150,
                                },
                            },
                            compartment: {
                                price: {
                                    currency,
                                    value: 250,
                                },
                            },
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            compartment: {
                                price: {
                                    currency,
                                    value: 250,
                                },
                            },
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            compartment: {
                                price: {
                                    currency,
                                    value: 350,
                                },
                            },
                        },
                    },
                },
                {
                    tariffs: {
                        classes: {
                            compartment: {
                                price: {
                                    currency,
                                    value: 450,
                                },
                            },
                        },
                    },
                },
            ] as ITrainsTariffApiSegment[];
            const filtersData = {
                trainTariffClass: {
                    value: ['compartment'],
                    filteredSegmentIndices: [true, true, true, true, true],
                },
            } as ITrainsFilters;
            const newOptionsForSegments = [
                {
                    count: 2,
                    min: 200,
                    max: 300,
                    currency,
                    value: '200-300',
                },
                {
                    count: 1,
                    min: 300,
                    max: 400,
                    currency,
                    value: '300-400',
                },
                {
                    count: 1,
                    min: 400,
                    max: 500,
                    currency,
                    value: '400-500',
                },
            ];
            const result = priceRange.getActiveOptions({
                filtersData,
                segments,
            });

            expect(result).toEqual(newOptionsForSegments);
        });
    });
});
