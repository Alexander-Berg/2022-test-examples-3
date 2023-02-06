import {
    ITrainsSchema,
    TrainPlaceType,
} from 'server/api/TrainsApi/types/ITrainsDetailsApiResponse';
import {ITrainsCoachPlace} from 'server/services/TrainsService/types/ITrainsDetailsInfoServiceResponse';

import getNearWindowInRowPlacesWithDirections from 'projects/trains/components/TrainsOrderPage/Requirements/SapsanRequirements/helpers/getNearWindowInRowPlacesWithDirections';

const schema = {
    placeFlags: {
        groupsInRows: [
            [1, 2],
            [3, 4],
            [5, 6],
        ],
    },
} as ITrainsSchema;

describe('getNearWindowInRowPlacesWithDirections', () => {
    it('Две полные группы с местами у окон - вернет 4 места: 2 по ходу и 2 против', () => {
        expect(
            getNearWindowInRowPlacesWithDirections(
                [
                    {
                        number: 1,
                        type: TrainPlaceType.NEAR_TABLE_FORWARD,
                    },
                    {
                        number: 2,
                        type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                    },
                    {
                        number: 3,
                        type: TrainPlaceType.NEAR_TABLE_FORWARD,
                    },
                    {
                        number: 4,
                        type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                    },
                ] as ITrainsCoachPlace[],
                schema,
            ),
        ).toEqual({
            forward: 2,
            backward: 2,
        });
    });

    it('Две полные группы не подряд с местами у окон - вернет 4 места: 2 по ходу и 2 против', () => {
        expect(
            getNearWindowInRowPlacesWithDirections(
                [
                    {
                        number: 1,
                        type: TrainPlaceType.NEAR_TABLE_FORWARD,
                    },
                    {
                        number: 2,
                        type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                    },
                    {
                        number: 5,
                        type: TrainPlaceType.NEAR_TABLE_FORWARD,
                    },
                    {
                        number: 6,
                        type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                    },
                ] as ITrainsCoachPlace[],
                schema,
            ),
        ).toEqual({
            forward: 2,
            backward: 2,
        });
    });

    it('Одна полная группа и одно место у окна - вернет 3 места: 2 по ходу и 1 против', () => {
        expect(
            getNearWindowInRowPlacesWithDirections(
                [
                    {
                        number: 1,
                        type: TrainPlaceType.NEAR_TABLE_FORWARD,
                    },
                    {
                        number: 2,
                        type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                    },
                    {
                        number: 5,
                        type: TrainPlaceType.NEAR_TABLE_FORWARD,
                    },
                ] as ITrainsCoachPlace[],
                schema,
            ),
        ).toEqual({
            forward: 2,
            backward: 1,
        });
    });

    it('Одна полная группа и одно место не у окна - вернет 2 места: 1 по ходу и 1 против', () => {
        expect(
            getNearWindowInRowPlacesWithDirections(
                [
                    {
                        number: 1,
                        type: TrainPlaceType.NEAR_TABLE_FORWARD,
                    },
                    {
                        number: 2,
                        type: TrainPlaceType.NEAR_TABLE_FORWARD,
                    },
                    {
                        number: 6,
                        type: TrainPlaceType.NEAR_TABLE_BACKWARD,
                    },
                ] as ITrainsCoachPlace[],
                schema,
            ),
        ).toEqual({
            forward: 2,
            backward: 0,
        });
    });

    it('Одно место не у окна - вернет 0 мест', () => {
        expect(
            getNearWindowInRowPlacesWithDirections(
                [
                    {
                        number: 2,
                        type: TrainPlaceType.NEAR_TABLE_FORWARD,
                    },
                ] as ITrainsCoachPlace[],
                schema,
            ),
        ).toEqual({
            forward: 0,
            backward: 0,
        });
    });
});
