import { EMPTY_DATA } from 'constants/constants';

import { CarStatusType } from 'entities/Car/consts/CarStatusType';
import { getCarSpeed } from 'entities/Car/helpers/getCarSpeed/getCarSpeed';
import { CarCommonSpeedTraitSchema } from 'entities/Car/types/CarCommonSpeedTraitSchema';
import { CarSchema } from 'entities/Car/types/CarSchema';

const CAR_INFO = {
    speed: 25,
    status: CarStatusType.RIDING,
} as Pick<CarSchema, 'status'> & CarCommonSpeedTraitSchema;

describe('getCarSpeed', function () {
    it('works with car info', function () {
        expect(getCarSpeed(CAR_INFO)).toEqual('25 km/h');
    });

    it('works with engine_off', function () {
        const car = { ...CAR_INFO, status: CarStatusType.ENGINE_OFF };
        expect(getCarSpeed(car)).toEqual(EMPTY_DATA);
    });

    it('works with 0 speed', function () {
        const car = { ...CAR_INFO, speed: 0 };

        expect(getCarSpeed(car)).toEqual(EMPTY_DATA);
    });
});
