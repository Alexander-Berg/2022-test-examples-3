import {ETimeOfDay} from 'utilities/dateUtils/types';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import arrival from '../../arrival';

const segment = {
    arrival: '2016-08-06T00:00:00+00:00',
    stationTo: {
        timezone: 'Asia/Yekaterinburg',
    },
} as ITrainsTariffApiSegment;

describe('arrival.apply', () => {
    it('Вернёт true для дефолтного значения', () => {
        expect(arrival.apply(arrival.getDefaultValue(), segment)).toBe(true);
    });

    it('Вернёт true если время прибытия соответствует заданному значению', () => {
        expect(arrival.apply([ETimeOfDay.NIGHT, ETimeOfDay.DAY], segment)).toBe(
            true,
        );
    });

    it('Вернёт false если время прибытия не соответствует заданному значению', () => {
        expect(arrival.apply([ETimeOfDay.DAY], segment)).toBe(false);
    });
});
