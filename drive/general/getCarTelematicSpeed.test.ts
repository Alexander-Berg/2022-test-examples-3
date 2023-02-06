import { EMPTY_DATA } from 'constants/constants';

import { CarTelematicsRes } from 'entities/Car/api/useCarTelematics/useCarTelematics';
import { getCarTelematicSpeed } from 'entities/Car/helpers/getCarTelematicSpeed/getCarTelematicSpeed';

const TELEMATICS_INFO = {
    speed: {
        id: 103,
        value: 10,
        name: 'speed',
        since: 0,
        updated: 0,
    },
};

describe('getCarTelematicSpeed', function () {
    it('works with telematics info', function () {
        expect(getCarTelematicSpeed(TELEMATICS_INFO)).toEqual('10 km/h');
    });

    it('works without telematics infj', function () {
        expect(getCarTelematicSpeed({})).toEqual(EMPTY_DATA);
    });

    it('works with 0 speed', function () {
        const telematics = { ...TELEMATICS_INFO, speed: { speed: 0 } } as unknown as CarTelematicsRes;

        expect(getCarTelematicSpeed(telematics)).toEqual(EMPTY_DATA);
    });
});
