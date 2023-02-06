import AllThreadType from '../../../interfaces/state/station/AllThreadType';
import StationEventList from '../../../interfaces/state/station/StationEventList';
import DateMoment from '../../../interfaces/date/DateMoment';
import StationTime from '../../../interfaces/state/station/StationTime';
import {TransportType} from '../../transportType';

import getSuitableTimePeriod from '../getSuitableTimePeriod';

const threads = Array(100).fill({
    eventDt: {time: '11:00'},
    transportType: TransportType.plane,
}) as unknown as AllThreadType[];

const commonParams = {
    event: StationEventList.departure,
    threads,
    search: '',
    terminalName: '',
    isMobile: false,
    companiesById: {},
};

describe('getSuitableTimePeriod', () => {
    it(`Если ниток больше 99 и по заданным критериям фильтров и текущего интервала времени на станции подходит 
    хоть одна нитка, то покажем текущий интервал. Иначе покажем весь день`, () => {
        expect(
            getSuitableTimePeriod({
                ...commonParams,
                now: '2020-04-28T11:57:00+03:00' as DateMoment,
            }),
        ).toBe(StationTime['p10-12']);

        expect(
            getSuitableTimePeriod({
                ...commonParams,
                now: '2020-04-28T12:57:00+03:00' as DateMoment,
            }),
        ).toBe(StationTime.all);

        expect(
            getSuitableTimePeriod({
                ...commonParams,
                threads: commonParams.threads.concat([
                    {
                        eventDt: {time: '23:00'},
                        transportType: TransportType.plane,
                    },
                ] as unknown as AllThreadType[]),
                now: '2020-04-28T22:57:00+03:00' as DateMoment,
            }),
        ).toBe(StationTime['p22-00']);
    });

    it('Если ниток меньше ста, то показываем весь день', () => {
        expect(
            getSuitableTimePeriod({
                ...commonParams,
                threads: commonParams.threads.splice(0, 99),
                now: '2020-04-28T11:57:00+03:00' as DateMoment,
            }),
        ).toBe(StationTime.all);
    });
});
