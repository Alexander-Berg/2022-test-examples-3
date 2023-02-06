import { areSchedulesEqual, getSchedulesDiff } from './duty-schedules';

describe('areSchedulesEqual works properly', () => {
    it('for empty object', () => {
        const scheduleA = { service: {} };
        const scheduleB = { service: {} };

        expect(areSchedulesEqual(scheduleA, scheduleB)).toBe(true);
    });

    it('for same filled objects', () => {
        const scheduleA = {
            id: 1,
            service: { id: 2 },
            name: 'schedule',
            slug: 'schedule',
            role: { id: 3 },
            roleOnDuty: 4,
            personsCount: 1,
            considerOtherSchedules: true,
            dutyOnHolidays: false,
            dutyOnWeekends: false,
            startDate: new Date(Date.UTC(2019, 1, 1)),
            duration: 100,
        };
        const scheduleB = {
            id: 1,
            service: { id: 2 },
            name: 'schedule',
            slug: 'schedule',
            role: { id: 3 },
            roleOnDuty: 4,
            personsCount: 1,
            considerOtherSchedules: true,
            dutyOnHolidays: false,
            dutyOnWeekends: false,
            startDate: new Date(Date.UTC(2019, 1, 1)),
            duration: 100,
        };

        expect(areSchedulesEqual(scheduleA, scheduleB)).toBe(true);
    });

    it('for different filled objects', () => {
        const scheduleA = {
            id: 1,
            service: { id: 2 },
            name: 'schedule',
            slug: 'schedule',
            role: { id: 3 },
            roleOnDuty: 4,
            personsCount: 1,
            considerOtherSchedules: true,
            dutyOnHolidays: false,
            dutyOnWeekends: true,
            startDate: new Date(Date.UTC(2019, 1, 1)),
            duration: 100,
        };
        const scheduleB = {
            id: 11,
            service: { id: 21 },
            name: 'schedule1',
            slug: 'schedule1',
            role: { id: 31 },
            roleOnDuty: 41,
            personsCount: 11,
            considerOtherSchedules: false,
            dutyOnHolidays: true,
            dutyOnWeekends: false,
            startDate: new Date(Date.UTC(2019, 1, 2)),
            duration: 101,
        };

        expect(areSchedulesEqual(scheduleA, scheduleB)).toEqual(false);
    });

    it('for ignored id and service', () => {
        const scheduleA = {
            id: 1,
            service: { id: 2 },
        };
        const scheduleB = {
            id: 11,
            service: { id: 21 },
        };

        expect(areSchedulesEqual(scheduleA, scheduleB, true, true)).toEqual(true);
    });
});

describe('getSchedulesDiff', () => {
    const prevSchedule = {
        duration: 42,
        dutyOnHolidays: false,
        dutyOnWeekends: false,
    };
    const nextSchedule = {
        duration: 146,
        dutyOnHolidays: false,
        dutyOnWeekends: false,
    };

    it('Should include changed fields in the result', () => {
        expect(getSchedulesDiff(prevSchedule, nextSchedule)).toHaveProperty('duration');
    });

    it('Should not include not changed fields in the result', () => {
        expect(getSchedulesDiff(prevSchedule, nextSchedule)).not.toHaveProperty('dutyOnHolidays');
        expect(getSchedulesDiff(prevSchedule, nextSchedule)).not.toHaveProperty('dutyOnWeekends');
    });

    it('Should process all fields', () => {
        const actual = getSchedulesDiff({}, {
            id: 42,
            service: 'service',
            name: 'name',
            slug: 'slug',
            role: { id: 12 },
            roleOnDuty: 13,
            personsCount: 146,
            needOrder: true,
            orders: [1, 2, 3],
            startDate: new Date(),
            startTime: '12:34',
            duration: 1,
            considerOtherSchedules: true,
            dutyOnHolidays: false,
            dutyOnWeekends: false,
            foo: 'bar',
        });

        expect(Object.keys(actual)).toEqual([
            'id',
            'service',
            'name',
            'slug',
            'role',
            'roleOnDuty',
            'personsCount',
            'needOrder',
            'orders',
            'startDate',
            'startTime',
            'duration',
            'considerOtherSchedules',
            'dutyOnHolidays',
            'dutyOnWeekends',
        ]);
        expect(actual).not.toHaveProperty('foo');
    });

    it('Should opt in to ignore id and service', () => {
        const actual = getSchedulesDiff({}, {
            id: 42,
            service: 'service',
        }, true, true);

        expect(actual).not.toHaveProperty('id');
        expect(actual).not.toHaveProperty('service');
    });
});
