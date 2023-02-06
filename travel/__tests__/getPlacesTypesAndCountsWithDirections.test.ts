import {REQUIREMENTS_IRRELEVANT_OPTION} from 'projects/trains/constants/requirements';

import {
    ITrainsSchema,
    TrainPlaceType,
} from 'server/api/TrainsApi/types/ITrainsDetailsApiResponse';
import {ITrainsCoachPlace} from 'server/services/TrainsService/types/ITrainsDetailsInfoServiceResponse';

import getPlacesTypesAndCountsWithDirections from 'projects/trains/components/TrainsOrderPage/Requirements/SapsanRequirements/helpers/getPlacesTypesAndCountsWithDirections';
import {
    SAPSAN_NEAR_TABLE,
    SAPSAN_NEAR_WINDOW,
    SAPSAN_NEAR_WINDOW_IN_ROW,
    SAPSAN_NO_TABLE,
    SAPSAN_SINGLE,
} from 'projects/trains/lib/order/placesTypes';

const schema = {
    placeFlags: {
        groupsInRows: [
            [1, 2],
            [3, 4],
            [5, 6],
        ],
    },
} as ITrainsSchema;

describe('getPlacesTypesAndCountsWithDirections', () => {
    describe('1 место', () => {
        it('Без стола по направлению', () => {
            expect(
                getPlacesTypesAndCountsWithDirections(
                    [
                        {
                            number: 1,
                            type: TrainPlaceType.NO_WINDOW_FORWARD,
                        },
                    ] as ITrainsCoachPlace[],
                    schema,
                ),
            ).toEqual({
                [REQUIREMENTS_IRRELEVANT_OPTION]: {
                    forward: 1,
                    backward: 0,
                },
                [SAPSAN_NEAR_WINDOW]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_NEAR_WINDOW_IN_ROW]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_NEAR_TABLE]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_NO_TABLE]: {
                    forward: 1,
                    backward: 0,
                },
                [SAPSAN_SINGLE]: {
                    forward: 0,
                    backward: 0,
                },
            });
        });

        it('У окна без стола по направлению', () => {
            expect(
                getPlacesTypesAndCountsWithDirections(
                    [
                        {
                            number: 1,
                            type: TrainPlaceType.NO_TABLE_FORWARD,
                        },
                    ] as ITrainsCoachPlace[],
                    schema,
                ),
            ).toEqual({
                [REQUIREMENTS_IRRELEVANT_OPTION]: {
                    forward: 1,
                    backward: 0,
                },
                [SAPSAN_NEAR_WINDOW]: {
                    forward: 1,
                    backward: 0,
                },
                [SAPSAN_NEAR_WINDOW_IN_ROW]: {
                    forward: 1,
                    backward: 0,
                },
                [SAPSAN_NEAR_TABLE]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_NO_TABLE]: {
                    forward: 1,
                    backward: 0,
                },
                [SAPSAN_SINGLE]: {
                    forward: 0,
                    backward: 0,
                },
            });
        });

        it('У окна у стола против направления', () => {
            expect(
                getPlacesTypesAndCountsWithDirections(
                    [
                        {
                            number: 1,
                            type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                        },
                    ] as ITrainsCoachPlace[],
                    schema,
                ),
            ).toEqual({
                [REQUIREMENTS_IRRELEVANT_OPTION]: {
                    forward: 0,
                    backward: 1,
                },
                [SAPSAN_NEAR_WINDOW]: {
                    forward: 0,
                    backward: 1,
                },
                [SAPSAN_NEAR_WINDOW_IN_ROW]: {
                    forward: 0,
                    backward: 1,
                },
                [SAPSAN_NEAR_TABLE]: {
                    forward: 0,
                    backward: 1,
                },
                [SAPSAN_NO_TABLE]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_SINGLE]: {
                    forward: 0,
                    backward: 0,
                },
            });
        });

        it('Одиночное место', () => {
            expect(
                getPlacesTypesAndCountsWithDirections(
                    [
                        {
                            number: 13,
                            type: TrainPlaceType.SINGLE_FORWARD,
                        },
                    ] as ITrainsCoachPlace[],
                    schema,
                ),
            ).toEqual({
                [REQUIREMENTS_IRRELEVANT_OPTION]: {
                    forward: 1,
                    backward: 0,
                },
                [SAPSAN_NEAR_WINDOW]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_NEAR_WINDOW_IN_ROW]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_NEAR_TABLE]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_NO_TABLE]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_SINGLE]: {
                    forward: 1,
                    backward: 0,
                },
            });
        });

        it('Место с животными', () => {
            expect(
                getPlacesTypesAndCountsWithDirections(
                    [
                        {
                            number: 13,
                            type: TrainPlaceType.WITH_PETS,
                        },
                    ] as ITrainsCoachPlace[],
                    schema,
                ),
            ).toEqual({
                [REQUIREMENTS_IRRELEVANT_OPTION]: {
                    forward: 0,
                    backward: 1,
                },
                [SAPSAN_NEAR_WINDOW]: {
                    forward: 0,
                    backward: 1,
                },
                [SAPSAN_NEAR_WINDOW_IN_ROW]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_NEAR_TABLE]: {
                    forward: 0,
                    backward: 0,
                },
                [SAPSAN_NO_TABLE]: {
                    forward: 0,
                    backward: 1,
                },
                [SAPSAN_SINGLE]: {
                    forward: 0,
                    backward: 0,
                },
            });
        });
    });

    describe('Больше одного места', () => {
        it('Разные типы мест', () => {
            expect(
                getPlacesTypesAndCountsWithDirections(
                    [
                        {
                            number: 1,
                            type: TrainPlaceType.NO_TABLE_FORWARD,
                        },
                        {
                            number: 2,
                            type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                        },
                        {
                            number: 3,
                            type: TrainPlaceType.SINGLE_FORWARD,
                        },
                    ] as ITrainsCoachPlace[],
                    schema,
                ),
            ).toEqual({
                [REQUIREMENTS_IRRELEVANT_OPTION]: {
                    forward: 2,
                    backward: 1,
                },
                [SAPSAN_NEAR_WINDOW]: {
                    forward: 1,
                    backward: 0,
                },
                [SAPSAN_NEAR_WINDOW_IN_ROW]: {
                    forward: 2,
                    backward: 1,
                },
                [SAPSAN_NEAR_TABLE]: {
                    forward: 0,
                    backward: 1,
                },
                [SAPSAN_NO_TABLE]: {
                    forward: 1,
                    backward: 0,
                },
                [SAPSAN_SINGLE]: {
                    forward: 1,
                    backward: 0,
                },
            });
        });
    });
});
