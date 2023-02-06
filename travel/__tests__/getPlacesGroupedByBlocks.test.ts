import {TRAIN_COACH_TYPE} from 'projects/trains/constants/coachType';
import {ARRANGEMENT_REQUIREMENTS} from 'projects/trains/constants/requirements';

import {ISchemaPlaceFlags} from 'server/api/TrainsApi/types/ITrainsDetailsApiResponse';
import {ITrainsCoach} from 'reducers/trains/order/types';

import getPlacesGroupedByBlocks from 'projects/trains/components/TrainsOrderPage/Requirements/helpers/getPlacesGroupedByBlocks';

const placeFlags = {
    compartments: [
        [1, 2, 3, 4],
        [5, 6, 7, 8],
    ],
    sections: [
        [1, 2, 3, 4, 55, 56],
        [5, 6, 7, 8, 53, 54],
    ],
    side: [2, 4],
    groupsInRows: [
        [1, 2],
        [3, 4],
        [5, 6],
    ],
} as ISchemaPlaceFlags;

const coach = {
    places: [
        {
            number: 1,
        },
        {
            number: 2,
        },
        {
            number: 3,
        },
        {
            number: 4,
        },
    ],
} as ITrainsCoach;

describe('getPlacesGroupedByBlocks', () => {
    it('Вернет купейные группы', () => {
        expect(
            getPlacesGroupedByBlocks({
                placesType: ARRANGEMENT_REQUIREMENTS.COMPARTMENT,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                placeFlags,
                coach,
            }),
        ).toEqual(placeFlags.compartments);
    });

    it('Вернет группы секций', () => {
        expect(
            getPlacesGroupedByBlocks({
                placesType: ARRANGEMENT_REQUIREMENTS.SECTION,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                placeFlags,
                coach,
            }),
        ).toEqual(placeFlags.sections);
    });

    it('Вернет группу не боковых мест', () => {
        expect(
            getPlacesGroupedByBlocks({
                placesType: ARRANGEMENT_REQUIREMENTS.NOT_SIDE,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                placeFlags,
                coach,
            }),
        ).toEqual([[1, 3]]);
    });

    it('Вернет группы в ряду для сидячих', () => {
        expect(
            getPlacesGroupedByBlocks({
                placesType: ARRANGEMENT_REQUIREMENTS.NEAREST,
                coachType: TRAIN_COACH_TYPE.SITTING,
                placeFlags,
                coach,
            }),
        ).toEqual(placeFlags.groupsInRows);
    });

    it('Если тип мест не задан - вернет весь диапазон мест вагона', () => {
        expect(
            getPlacesGroupedByBlocks({
                placesType: undefined,
                coachType: TRAIN_COACH_TYPE.SITTING,
                placeFlags,
                coach,
            }),
        ).toEqual([[1, 2, 3, 4]]);
    });
});
