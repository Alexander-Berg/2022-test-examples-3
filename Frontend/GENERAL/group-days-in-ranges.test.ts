import { DayOfWeek } from 'types/day-of-week';

import { groupDaysInRanges } from './group-days-in-ranges';

describe('utils/day-of-week/groupDaysInRanges', () => {
    it('Должна группировать дни', () => {
        expect(groupDaysInRanges([
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
        ])).toEqual([
            [DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY],
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
        ]);
    });

    it('Должна не группировать дни', () => {
        expect(groupDaysInRanges([
            DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
        ])).toEqual([
            DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
        ]);
    });

    it('Должна давать пустой результат', () => {
        expect(groupDaysInRanges([])).toEqual([]);
    });

    it('Должна группировать все дни недели', () => {
        expect(groupDaysInRanges([
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
        ])).toEqual([
            [DayOfWeek.MONDAY, DayOfWeek.SUNDAY],
        ]);
    });
});
