import { DayOfWeek } from 'types/day-of-week';
import { convertTimetableToTimezoneOffset } from 'utils/iot-scenario-timetable';

describe('utils/iot-scenario-timetable/convertTimetableToTimezoneOffset', () => {
    it('Должна конвертировать смещение (восточнее зоны UTC)', () => {
        const timezoneOffset = -150;

        const days = [
            DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SUNDAY,
        ];
        expect(convertTimetableToTimezoneOffset({
            days_of_week: [...days],
            time_offset: 12345,
        }, timezoneOffset)).toEqual({
            days_of_week: [...days],
            time_offset: 21345,
        });
    });

    it('Должна конвертировать смещение (западнее зоны UTC)', () => {
        const timezoneOffset = 180;

        const days = [
            DayOfWeek.TUESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.SATURDAY,
        ];
        expect(convertTimetableToTimezoneOffset({
            days_of_week: [...days],
            time_offset: 12345,
        }, timezoneOffset)).toEqual({
            days_of_week: [...days],
            time_offset: 1545,
        });
    });

    it('Должна конвертировать смещение со сменой дня (восточнее зоны UTC)', () => {
        const timezoneOffset = -150;

        const result = convertTimetableToTimezoneOffset({
            days_of_week: [
                DayOfWeek.MONDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SUNDAY,
            ],
            time_offset: 81977,
        }, timezoneOffset);

        expect(result.days_of_week.sort()).toEqual([
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.SATURDAY,
        ].sort());
        expect(result.time_offset).toEqual(4577);
    });

    it('Должна конвертировать смещение со сменой дня (западнее зоны UTC)', () => {
        const timezoneOffset = 1020;

        const result = convertTimetableToTimezoneOffset({
            days_of_week: [
                DayOfWeek.MONDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SUNDAY,
            ],
            time_offset: 20987,
        }, timezoneOffset);

        expect(result.days_of_week.sort()).toEqual([
            DayOfWeek.SUNDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.SATURDAY,
        ].sort());
        expect(result.time_offset).toEqual(46187);
    });

    it('Должна конвертировать смещение на стыке дней (восточнее зоны UTC)', () => {
        const timezoneOffset = -270;

        const result = convertTimetableToTimezoneOffset({
            days_of_week: [
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.SUNDAY,
            ],
            time_offset: 70200,
        }, timezoneOffset);

        expect(result.days_of_week.sort()).toEqual([
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.MONDAY,
        ].sort());
        expect(result.time_offset).toEqual(0);
    });

    it('Должна конвертировать смещение на стыке дней (западнее зоны UTC)', () => {
        const timezoneOffset = 30;

        const result = convertTimetableToTimezoneOffset({
            days_of_week: [
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.SUNDAY,
            ],
            time_offset: 1800,
        }, timezoneOffset);

        expect(result.days_of_week.sort()).toEqual([
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.SUNDAY,
        ].sort());
        expect(result.time_offset).toEqual(0);
    });

    it('Должна нормализовать смещение', () => {
        const timezoneOffset = -180;

        const result = convertTimetableToTimezoneOffset({
            days_of_week: [
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.SUNDAY,
            ],
            time_offset: 185145, // 86400 + 86400 + 12345
        }, timezoneOffset);

        expect(result.days_of_week.sort()).toEqual([
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.TUESDAY,
        ].sort());
        expect(result.time_offset).toEqual(23145); // (86400 + 86400 + 12345 - (-180 * 60)) % 86400
    });
});
