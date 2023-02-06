import {ETimeOfDay} from 'utilities/dateUtils/types';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import arrival from '../../arrival';

const segment = {
    arrival: '2016-08-06T00:00:00+00:00',
    stationTo: {
        timezone: 'Asia/Yekaterinburg',
    },
} as ITrainsTariffApiSegment;

describe('arrival.updateOptions', () => {
    it('Обновит опции фильтра с использованием обычного сегмента', () => {
        const options = arrival.updateOptions(
            arrival.getDefaultOptions(),
            segment,
        );

        expect(options.length).toBe(1);
        expect(options).toEqual(expect.arrayContaining([ETimeOfDay.NIGHT]));
    });
});
