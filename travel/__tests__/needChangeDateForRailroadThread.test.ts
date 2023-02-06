import DateMoment from '../../../interfaces/date/DateMoment';
import IThreadRailroad from '../../../interfaces/state/station/IThreadRailroad';

import needChangeDateForRailroadThread from '../needChangeDateForRailroadThread';

const threadWithSameDays = {
    departureFrom: '2020-03-11T15:40:00+03:00' as DateMoment,
    eventDt: {datetime: '2020-03-11T15:40:00+03:00' as DateMoment},
} as IThreadRailroad;

const threadWithDifferingDays = {
    departureFrom: '2020-03-11T15:40:00+03:00' as DateMoment,
    eventDt: {datetime: '2020-03-12T15:40:00+03:00' as DateMoment},
} as IThreadRailroad;

describe('needChangeDateForRailroadThread', () => {
    it('Если фактическая дата НЕ отличается от запланированной, то зачеркивать время НЕ нужно (вернет FALSE)', () => {
        expect(needChangeDateForRailroadThread(threadWithSameDays)).toBe(false);
    });

    it('Если фактическая дата отличается от запланированной, то зачеркивать время нужно (вернет TRUE)', () => {
        expect(needChangeDateForRailroadThread(threadWithDifferingDays)).toBe(
            true,
        );
    });
});
