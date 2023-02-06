import type { Shift } from './DutyShifts.types';
import { EScheduleStatus, Schedule, StoreForDutySchedules } from './DutySchedules.types';
import { getDutySchedulesMock, getDutyShiftsMock } from '~/test/jest/mocks/data/duty';

const shifts: Shift[] = [{
    id: 100,
    person: {
        id: 1,
        login: 'testUser',
        name: { ru: 'test', en: 'user' },
        vteams: [],
        shifts: [],
    },
    replaces: [],
    problems_count: 0,
    schedule: { id: 200 },
    is_approved: false,
    start: '2020-07-17',
    end: '2020-07-18',
    start_datetime: '2020-07-17T12:00:00+03:00',
    end_datetime: '2020-07-18T12:00:00+03:00',
}];
export const shiftsWithScheduleFields = [{
    ...shifts[0],
    schedule: { id: 200, name: 'Дежурство' },
}];

const schedule300: Schedule = {
    id: 300,
    service: { id: 2, slug: 'service', name: { ru: 'Сервис', en: 'service' }, parent: 3 },
    name: 'Дежурство2',
    description: 'Дежурство2 - описание',
    slug: 'duty2',
    personsCount: 2,
    startDate: new Date('2020-07-12'),
    startTime: '14:00',
    duration: 2,
    autoapproveTimedelta: 7,
    dutyOnHolidays: false,
    dutyOnWeekends: false,
    considerOtherSchedules: true,
    showInStaff: true,
    needOrder: true,
    persons: {},
    status: EScheduleStatus.nothing,
    daysStatus: {},
};

export const schedule200: Schedule = {
    id: 200,
    service: { id: 2, slug: 'service', name: { ru: 'Сервис', en: 'service' }, parent: 3 },
    name: 'Дежурство',
    description: 'Дежурство - описание',
    slug: 'duty',
    personsCount: 1,
    startDate: new Date('2020-07-10'),
    startTime: '12:00',
    duration: 1,
    autoapproveTimedelta: 0,
    dutyOnHolidays: true,
    dutyOnWeekends: true,
    considerOtherSchedules: false,
    showInStaff: false,
    needOrder: false,
    persons: {},
    status: EScheduleStatus.nothing,
    daysStatus: {},
};
const schedules: Schedule[] = [schedule300, schedule200];

export const state: StoreForDutySchedules = {
    dutyShifts: getDutyShiftsMock({ data: shifts }),
    dutySchedules: getDutySchedulesMock({ collection: schedules }),
};
