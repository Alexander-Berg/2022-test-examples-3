import {ETimeOfDay} from 'utilities/dateUtils/types';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import departure from '../../departure';

const segment = {
    departure: '2016-08-06T00:00:00+00:00',
    stationFrom: {
        timezone: 'Asia/Yekaterinburg',
    },
} as ITrainsTariffApiSegment;

describe('departure.updateOptions', () => {
    it('Обновит опции фильтра с использованием обычного сегмента', () => {
        const options = departure.updateOptions(
            departure.getDefaultOptions(),
            segment,
        );

        expect(options.length).toBe(1);
        expect(options).toEqual(expect.arrayContaining([ETimeOfDay.NIGHT]));
    });
});
