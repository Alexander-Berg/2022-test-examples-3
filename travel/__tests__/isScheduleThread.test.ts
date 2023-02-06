import DateMoment from '../../../interfaces/date/DateMoment';

import isScheduleThread from '../isScheduleThread';

const canonicalUid = 'someCanonical';
const departureFrom = '2020-03-11T15:40:00+03:00' as DateMoment;
const interval = {
    density: 'микроавтобус раз в 15-25 минут',
    beginTime: '07:20',
    endTime: '22:00',
};

describe('isScheduleThread', () => {
    it('false', () => {
        expect(isScheduleThread({canonicalUid, interval})).toBe(false);
    });

    it('true', () => {
        expect(isScheduleThread({canonicalUid, departureFrom})).toBe(true);
    });
});
