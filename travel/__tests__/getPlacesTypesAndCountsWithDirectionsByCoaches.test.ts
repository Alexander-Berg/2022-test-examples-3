import {REQUIREMENTS_IRRELEVANT_OPTION} from 'projects/trains/constants/requirements';

import {ITrainsCoach} from 'reducers/trains/order/types';
import {
    ITrainsSchema,
    TrainPlaceType,
} from 'server/api/TrainsApi/types/ITrainsDetailsApiResponse';

import getPlacesTypesAndCountsWithDirectionsByCoaches from 'projects/trains/components/TrainsOrderPage/Requirements/SapsanRequirements/helpers/getPlacesTypesAndCountsWithDirectionsByCoaches';
import {
    SAPSAN_NEAR_TABLE,
    SAPSAN_NEAR_WINDOW,
    SAPSAN_NEAR_WINDOW_IN_ROW,
    SAPSAN_NO_TABLE,
    SAPSAN_SINGLE,
} from 'projects/trains/lib/order/placesTypes';

const coaches = [
    {
        schemaId: 1,
        places: [
            {
                number: 1,
                type: TrainPlaceType.NO_TABLE_FORWARD,
            },
        ],
    },
    {
        schemaId: 1,
        places: [
            {
                number: 3,
                type: TrainPlaceType.NO_TABLE_FORWARD,
            },
        ],
    },
    {
        schemaId: 1,
        places: [
            {
                number: 1,
                type: TrainPlaceType.NO_TABLE_FORWARD,
            },
            {
                number: 3,
                type: TrainPlaceType.NO_TABLE_FORWARD,
            },
        ],
    },
    {
        schemaId: 1,
        places: [
            {
                number: 1,
                type: TrainPlaceType.NO_TABLE_FORWARD,
            },
            {
                number: 3,
                type: TrainPlaceType.NO_TABLE_FORWARD,
            },
            {
                number: 5,
                type: TrainPlaceType.NO_TABLE_FORWARD,
            },
            {
                number: 7,
                type: TrainPlaceType.NO_TABLE_FORWARD,
            },
        ],
    },
    {
        schemaId: 1,
        places: [
            {
                number: 1,
                type: TrainPlaceType.NO_TABLE_FORWARD,
            },
            {
                number: 3,
                type: TrainPlaceType.NO_TABLE_BACKWARD,
            },
            {
                number: 5,
                type: TrainPlaceType.NO_TABLE_BACKWARD,
            },
            {
                number: 7,
                type: TrainPlaceType.NO_TABLE_BACKWARD,
            },
            {
                number: 9,
                type: TrainPlaceType.NO_TABLE_BACKWARD,
            },
        ],
    },
] as ITrainsCoach[];

const schemas = {
    1: {
        placeFlags: {
            groupsInRows: [
                [1, 2],
                [3, 4],
                [5, 6],
                [7, 8],
                [9, 10],
            ],
        },
    } as ITrainsSchema,
};

describe('getPlacesTypesAndCountsWithDirectionsByCoaches', () => {
    it('По одному месту у окна не у стола в двух вагонах', () => {
        expect(
            getPlacesTypesAndCountsWithDirectionsByCoaches(
                [coaches[0], coaches[1]],
                schemas,
            ),
        ).toEqual({
            [REQUIREMENTS_IRRELEVANT_OPTION]: {
                forward: 1,
                backward: 0,
                maxForwardPlaces: 1,
            },
            [SAPSAN_NEAR_WINDOW]: {
                forward: 1,
                backward: 0,
                maxForwardPlaces: 1,
            },
            [SAPSAN_NEAR_WINDOW_IN_ROW]: {
                forward: 1,
                backward: 0,
                maxForwardPlaces: 1,
            },
            [SAPSAN_NEAR_TABLE]: {
                forward: 0,
                backward: 0,
                maxForwardPlaces: 0,
            },
            [SAPSAN_NO_TABLE]: {
                forward: 1,
                backward: 0,
                maxForwardPlaces: 1,
            },
            [SAPSAN_SINGLE]: {
                forward: 0,
                backward: 0,
                maxForwardPlaces: 0,
            },
        });
    });

    it('В одном вагоне одно место у окна не у стола, во втором два у окан не у стола', () => {
        expect(
            getPlacesTypesAndCountsWithDirectionsByCoaches(
                [coaches[0], coaches[2]],
                schemas,
            ),
        ).toEqual({
            [REQUIREMENTS_IRRELEVANT_OPTION]: {
                forward: 2,
                backward: 0,
                maxForwardPlaces: 2,
            },
            [SAPSAN_NEAR_WINDOW]: {
                forward: 2,
                backward: 0,
                maxForwardPlaces: 2,
            },
            [SAPSAN_NEAR_WINDOW_IN_ROW]: {
                forward: 1,
                backward: 0,
                maxForwardPlaces: 1,
            },
            [SAPSAN_NEAR_TABLE]: {
                forward: 0,
                backward: 0,
                maxForwardPlaces: 0,
            },
            [SAPSAN_NO_TABLE]: {
                forward: 2,
                backward: 0,
                maxForwardPlaces: 2,
            },
            [SAPSAN_SINGLE]: {
                forward: 0,
                backward: 0,
                maxForwardPlaces: 0,
            },
        });
    });

    it(`В одном вагоне несколько мест по направлению у окна не у стола,
        во втором одно по направлению у окна не у стола и несколько против направления`, () => {
        expect(
            getPlacesTypesAndCountsWithDirectionsByCoaches(
                [coaches[3], coaches[4]],
                schemas,
            ),
        ).toEqual({
            [REQUIREMENTS_IRRELEVANT_OPTION]: {
                forward: 1,
                backward: 4,
                maxForwardPlaces: 4,
            },
            [SAPSAN_NEAR_WINDOW]: {
                forward: 1,
                backward: 4,
                maxForwardPlaces: 4,
            },
            [SAPSAN_NEAR_WINDOW_IN_ROW]: {
                forward: 1,
                backward: 1,
                maxForwardPlaces: 1,
            },
            [SAPSAN_NEAR_TABLE]: {
                forward: 0,
                backward: 0,
                maxForwardPlaces: 0,
            },
            [SAPSAN_NO_TABLE]: {
                forward: 1,
                backward: 4,
                maxForwardPlaces: 4,
            },
            [SAPSAN_SINGLE]: {
                forward: 0,
                backward: 0,
                maxForwardPlaces: 0,
            },
        });
    });
});
