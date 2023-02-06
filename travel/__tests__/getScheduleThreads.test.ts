import DateMoment from '../../../interfaces/date/DateMoment';

import getScheduleThreads from '../getScheduleThreads';

const canonicalUid = 'someCanonical';
const departureFrom = '2020-03-11T15:40:00+03:00' as DateMoment;
const interval = {
    density: 'микроавтобус раз в 15-25 минут',
    beginTime: '07:20',
    endTime: '22:00',
};
const intervalThread = {
    canonicalUid,
    interval,
};
const thread = {
    canonicalUid,
    departureFrom,
};

describe('getScheduleThreads', () => {
    it('split threads', () => {
        expect(
            getScheduleThreads(
                [intervalThread, thread, intervalThread, thread],
                null,
            ),
        ).toStrictEqual({
            threads: [thread, thread],
            intervalThreads: [intervalThread, intervalThread],
        });
    });
});
