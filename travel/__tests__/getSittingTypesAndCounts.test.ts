import {ITrainsCoachPlace} from 'server/services/TrainsService/types/ITrainsDetailsInfoServiceResponse';

import getNearWindowAndPassagePlacesCounts from 'projects/trains/components/TrainsOrderPage/Requirements/helpers/getNearWindowAndPassagePlacesCounts';

const placeMap = {
    1: {} as ITrainsCoachPlace,
    2: {} as ITrainsCoachPlace,
    3: {} as ITrainsCoachPlace,
    4: {} as ITrainsCoachPlace,
    5: {} as ITrainsCoachPlace,
};

const coachNearWindowPlaces = [2, 4, 5];

describe('getNearWindowAndPassagePlacesCounts', () => {
    it('Есть места у окна и у прохода', () => {
        expect(
            getNearWindowAndPassagePlacesCounts(
                placeMap,
                [1, 2, 3, 4, 5],
                coachNearWindowPlaces,
            ),
        ).toEqual({
            nearWindow: 3,
            nearPassage: 2,
        });
    });

    it('Есть места у окна и у прохода, часть мест не свободны', () => {
        expect(
            getNearWindowAndPassagePlacesCounts(
                placeMap,
                [1, 2, 3, 4, 5, 7, 8],
                coachNearWindowPlaces,
            ),
        ).toEqual({
            nearWindow: 3,
            nearPassage: 2,
        });
    });
});
