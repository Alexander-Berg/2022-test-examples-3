import {REQUIREMENTS_IRRELEVANT_OPTION} from 'projects/trains/constants/requirements';

import {
    ITrainsSchema,
    TrainPlaceType,
} from 'server/api/TrainsApi/types/ITrainsDetailsApiResponse';
import {ITrainsCoach} from 'reducers/trains/order/types';

import getAvailableOptionTypes from 'projects/trains/components/TrainsOrderPage/Requirements/SapsanRequirements/helpers/getAvailableOptionTypes';
import {
    SAPSAN_NEAR_TABLE,
    SAPSAN_NEAR_WINDOW,
    SAPSAN_NEAR_WINDOW_IN_ROW,
    SAPSAN_NO_TABLE,
} from 'projects/trains/lib/order/placesTypes';

const schemas = {
    1: {
        placeFlags: {
            groupsInRows: [
                [1, 2],
                [3, 4],
                [5, 6],
            ],
        },
    } as ITrainsSchema,
};

describe('getAvailableOptionTypes', () => {
    describe('1 пассажир, есть доступные варианты', () => {
        it('Одно место у окна без стола - вернет вариант "у окна" и "не у стола"', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NO_TABLE_FORWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 1,
                    isForward: false,
                }),
            ).toEqual([
                REQUIREMENTS_IRRELEVANT_OPTION,
                SAPSAN_NEAR_WINDOW,
                SAPSAN_NO_TABLE,
            ]);
        });

        it('Два места у окна без стола - вернет вариант "у окна" и "не у стола"', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NO_TABLE_FORWARD,
                                },
                                {
                                    number: 2,
                                    type: TrainPlaceType.NO_TABLE_FORWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 1,
                    isForward: false,
                }),
            ).toEqual([
                REQUIREMENTS_IRRELEVANT_OPTION,
                SAPSAN_NEAR_WINDOW,
                SAPSAN_NO_TABLE,
            ]);
        });
    });

    describe('1 пассажир, нет доступных вариантов', () => {
        it('Одно места у окна без стола против хода, нужно место по ходу - вернет пустой список вариантов', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 1,
                    isForward: true,
                }),
            ).toEqual([]);
        });
    });

    describe('2 пассажира, есть доступные варианты', () => {
        it('Два места у окна без стола против хода - вернет вариант "в ряду у окна" и "не у стола"', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                                {
                                    number: 2,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 2,
                    isForward: false,
                }),
            ).toEqual([
                REQUIREMENTS_IRRELEVANT_OPTION,
                SAPSAN_NEAR_WINDOW_IN_ROW,
                SAPSAN_NO_TABLE,
            ]);
        });

        it('Два разыных места - вернет вариант "в ряду у окна"', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                                {
                                    number: 2,
                                    type: TrainPlaceType.NEAR_TABLE_FORWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 2,
                    isForward: false,
                }),
            ).toEqual([
                REQUIREMENTS_IRRELEVANT_OPTION,
                SAPSAN_NEAR_WINDOW_IN_ROW,
            ]);
        });

        it('Два места у стола не подряд - вернет вариант "у стола" и "в ряду у окна"', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NEAR_TABLE_FORWARD,
                                },
                                {
                                    number: 2,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                                {
                                    number: 3,
                                    type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 2,
                    isForward: false,
                }),
            ).toEqual([
                REQUIREMENTS_IRRELEVANT_OPTION,
                SAPSAN_NEAR_WINDOW_IN_ROW,
                SAPSAN_NEAR_TABLE,
            ]);
        });

        it('Два места у стола не подряд по ходу - вернет вариант "у стола" и "в ряду у окна"', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NEAR_TABLE_FORWARD,
                                },
                                {
                                    number: 2,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                                {
                                    number: 3,
                                    type: TrainPlaceType.NEAR_TABLE_FORWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 2,
                    isForward: true,
                }),
            ).toEqual([
                REQUIREMENTS_IRRELEVANT_OPTION,
                SAPSAN_NEAR_WINDOW_IN_ROW,
                SAPSAN_NEAR_TABLE,
            ]);
        });
    });

    describe('2 пассажира, нет доступных вариантов', () => {
        it('Одно место - вернет пустой список вариантов', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 2,
                    isForward: false,
                }),
            ).toEqual([]);
        });

        it('Нужны места по направлению, но достаточного количества нет - вернет пустой список вариантов', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                                {
                                    number: 3,
                                    type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 2,
                    isForward: true,
                }),
            ).toEqual([]);
        });
    });

    describe('4 пассажира', () => {
        it('Eсть вариант "в ряду у окна"', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                                {
                                    number: 2,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                                {
                                    number: 3,
                                    type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                                },
                                {
                                    number: 5,
                                    type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                                },
                                {
                                    number: 6,
                                    type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 4,
                    isForward: false,
                }),
            ).toEqual([
                REQUIREMENTS_IRRELEVANT_OPTION,
                SAPSAN_NEAR_WINDOW_IN_ROW,
            ]);
        });

        it('Нет варианта "в ряду у окна"', () => {
            expect(
                getAvailableOptionTypes({
                    coaches: [
                        {
                            schemaId: 1,
                            places: [
                                {
                                    number: 1,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                                {
                                    number: 2,
                                    type: TrainPlaceType.NO_TABLE_BACKWARD,
                                },
                                {
                                    number: 3,
                                    type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                                },
                                {
                                    number: 5,
                                    type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                                },
                                {
                                    number: 7,
                                    type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                                },
                            ],
                        } as ITrainsCoach,
                    ],
                    schemas,
                    passengersCount: 4,
                    isForward: false,
                }),
            ).toEqual([REQUIREMENTS_IRRELEVANT_OPTION]);
        });
    });
});
