import {ETimeOfDay} from 'utilities/dateUtils/types';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import departure from '../../departure';

const segment = {
    departure: '2016-08-06T00:00:00+00:00',
    stationFrom: {
        timezone: 'Asia/Yekaterinburg',
    },
} as ITrainsTariffApiSegment;

describe('departure.apply', () => {
    it('Вернёт true для дефолтного значения', () => {
        expect(departure.apply(departure.getDefaultValue(), segment)).toBe(
            true,
        );
    });

    it('Вернёт true если время прибытия соответствует заданному значению', () => {
        expect(
            departure.apply([ETimeOfDay.NIGHT, ETimeOfDay.DAY], segment),
        ).toBe(true);
    });

    it('Вернёт false если время прибытия не соответствует заданному значению', () => {
        expect(departure.apply([ETimeOfDay.DAY], segment)).toBe(false);
    });
});
