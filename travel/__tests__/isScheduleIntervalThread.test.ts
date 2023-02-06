import DateMoment from '../../../interfaces/date/DateMoment';

import isScheduleIntervalThread from '../isScheduleIntervalThread';

const canonicalUid = 'someCanonical';
const departureFrom = '2020-03-11T15:40:00+03:00' as DateMoment;
const interval = {
    density: 'микроавтобус раз в 15-25 минут',
    beginTime: '07:20',
    endTime: '22:00',
};

describe('isScheduleIntervalThread', () => {
    it('false', () => {
        expect(isScheduleIntervalThread({canonicalUid, departureFrom})).toBe(
            false,
        );
    });

    it('true', () => {
        expect(isScheduleIntervalThread({canonicalUid, interval})).toBe(true);
    });
});
