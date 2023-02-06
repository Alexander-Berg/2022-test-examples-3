import {TrainPlaceType} from 'server/api/TrainsApi/types/ITrainsDetailsApiResponse';

import isNearWindowPlace from 'projects/trains/components/TrainsOrderPage/Requirements/SapsanRequirements/helpers/isNearWindowPlace';

describe('isNearWindowPlace', () => {
    it('Нечетное место - вернет true', () => {
        expect(isNearWindowPlace(3, TrainPlaceType.NO_TABLE_FORWARD)).toBe(
            true,
        );
    });

    it('Нечетное место по направлению, но задан тип, что место не у окна - вернет false', () => {
        expect(isNearWindowPlace(3, TrainPlaceType.NO_WINDOW_FORWARD)).toBe(
            false,
        );
    });

    it('Нечетное место против направлению, но задан тип, что место не у окна - вернет false', () => {
        expect(isNearWindowPlace(3, TrainPlaceType.NO_WINDOW_BACKWARD)).toBe(
            false,
        );
    });

    it('Четное место - вернет false', () => {
        expect(isNearWindowPlace(2, TrainPlaceType.NO_TABLE_FORWARD)).toBe(
            false,
        );
    });
});
