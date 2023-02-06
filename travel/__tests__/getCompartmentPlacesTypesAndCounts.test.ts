import {ITrainsCoachPlace} from 'server/services/TrainsService/types/ITrainsDetailsInfoServiceResponse';

import getUpperAndBottomPlacesCounts from 'projects/trains/components/TrainsOrderPage/Requirements/helpers/getUpperAndBottomPlacesCounts';

const placeMap = {
    1: {} as ITrainsCoachPlace,
    2: {} as ITrainsCoachPlace,
    3: {} as ITrainsCoachPlace,
    4: {} as ITrainsCoachPlace,
    5: {} as ITrainsCoachPlace,
};

const coachUpperPlaces = [2, 4, 5];

describe('getUpperAndBottomPlacesCounts', () => {
    it('Есть верхние и нижние места', () => {
        expect(
            getUpperAndBottomPlacesCounts(
                placeMap,
                [1, 2, 3, 4, 5],
                coachUpperPlaces,
            ),
        ).toEqual({
            upper: 3,
            bottom: 2,
        });
    });

    it('Есть верхние и нижние места, часть мест не свободны', () => {
        expect(
            getUpperAndBottomPlacesCounts(
                placeMap,
                [1, 2, 3, 4, 5, 7, 8],
                coachUpperPlaces,
            ),
        ).toEqual({
            upper: 3,
            bottom: 2,
        });
    });
});
