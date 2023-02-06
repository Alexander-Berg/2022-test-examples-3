/* eslint-disable @typescript-eslint/no-non-null-assertion */
import React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import { DateTime, Duration, DurationUnit } from 'luxon';
import { EScaleType } from '~/src/features/Duty2/redux/DutyShifts.types';
import { _Internal_CalendarContainer } from './Calendar.container';
import { ICalendarPageFilters, EHolidayType, IDutyShiftListItem } from '~/src/features/Duty2/redux/DutyShifts.types';
import {
    EScheduleStatus,
    EAbsenceType,
    IAbsentPerson,
    IScheduleListItem,
} from '~/src/features/Duty2/redux/DutySchedules.types';
import type { User } from './Calendar.types';
import { DutyAbsenses } from '~/src/features/Duty2/components/CalendarGrid/CalendarGrid.lib';
import { selectHolidayIntervals } from '~/src/features/Duty/redux/DutyShifts.selectors';
import { DutyShiftsStoreField } from '~/src/abc/react/redux/types';

declare global {
    namespace jest {
        interface Expect {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            toMatchDate(date: Date): any;
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            toMatchTime(date: Date): any;
        }
    }
}

describe('Prepares absences data correctly', () => {
    let wrapper: ShallowWrapper<
    _Internal_CalendarContainer['props'],
     _Internal_CalendarContainer['state'],
      _Internal_CalendarContainer
      > | null = null;

    const dutyShifts = {
        filters: {
            dateFrom: new Date(Date.UTC(2019, 0, 1)),
            dateTo: new Date(Date.UTC(2019, 1, 15)),
            scale: EScaleType.day,
        } as ICalendarPageFilters,
        data: {} as Record<string, IDutyShiftListItem>,
        loading: false,
    };

    const absenceData = {
        id: 401,
        start: DateTime.local(2019, 1, 9),
        end: DateTime.local(2019, 1, 10),
        type: EAbsenceType.Illness,
        person: { login: 'person1' } as IAbsentPerson,
        workInAbsence: true,
        fullDay: true,
    };

    const props = {
        dutyPersons: [],
        dateFrom: 0,
        role: 0,
        user: {} as User,
        person: [],
        dutyPersonsLoading: false,
        updateHolidays: jest.fn(),
        hasScheduleEditChanges: false,
        queryObj: {},
        filters: dutyShifts.filters,
        schedulesIds: [],
        schedulesList: {},
        duty2Shifts: {
            dutyShiftList: {},
            dutyShiftIds: [],
            filters: dutyShifts.filters,
        },
        next: null,
        prev: null,
        dutyShiftsLoading: false,
        dutyShiftsError: null,
        dutySchedulesLoading: false,
        dutySchedulesError: null,
    };

    beforeEach(() => {
        wrapper = shallow(
            <_Internal_CalendarContainer
                {...props}
                service={{ id: 101, slug: 'abc' }}
                holidayIntervals={[]}
                scale="day"
                dutyAbsences={{
                    filters: {},
                    data: [],
                }}
                setFilters={jest.fn()}
                interval={Duration.fromObject({ months: 2 })}
                getMemberFromService={jest.fn()}
                updateDutyAbsences={jest.fn()}
                fetchDutyShiftsList={jest.fn()}
                mergeDutyShifts={jest.fn()}
                deleteSchedule={jest.fn()}
                resetDutyShifts={jest.fn()}
                updateQueryStr={jest.fn()}
                filterQueryStr={jest.fn()}
                resetScheduleEdit={jest.fn()}
                requesterInService={false}
            />,
        );
    });

    afterEach(() => {
        wrapper!.unmount();
    });

    it('Properly maps data', () => {
        const dutyAbsences: DutyAbsenses = {
            filters: {},
            data: [
                { ...absenceData, id: 1 },
                { ...absenceData, id: 2 },
                { ...absenceData, id: 3, person: { login: 'person2' } as IAbsentPerson },
            ],
        };

        const { absences: actual } = wrapper!.instance()._getGridDataWrapper(
            dutyShifts.filters,
            {
                dutyShiftList: dutyShifts.data,
                dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                filters: dutyShifts.filters,
            },
            dutyAbsences,
            [301],
            { '301': {} as IScheduleListItem },
            [],
        );
        const expected = {
            person1: [{
                id: 1,
                start: Duration.fromObject({ days: 7, hours: 21 }).as('days'),
                length: 1,
                startDate: expect.toMatchDate(new Date(2019, 0, 9)),
                endDate: expect.toMatchDate(new Date(2019, 0, 10)),
                type: 'illness',
                fullDay: true,
                workInAbsence: true,
            }, {
                id: 2,
                start: Duration.fromObject({ days: 7, hours: 21 }).as('days'),
                length: 1,
                startDate: expect.toMatchDate(new Date(2019, 0, 9)),
                endDate: expect.toMatchDate(new Date(2019, 0, 10)),
                type: 'illness',
                fullDay: true,
                workInAbsence: true,
            }],
            person2: [{
                id: 3,
                start: Duration.fromObject({ days: 7, hours: 21 }).as('days'),
                length: 1,
                startDate: expect.toMatchDate(new Date(2019, 0, 9)),
                endDate: expect.toMatchDate(new Date(2019, 0, 10)),
                type: 'illness',
                fullDay: true,
                workInAbsence: true,
            }],
        };

        expect(actual).toEqual(expected);
    });

    describe('Properly calculates start index', () => {
        it('for regular absences', () => {
            const dutyAbsences: DutyAbsenses = {
                filters: {},
                data: [{
                    ...absenceData,
                    start: DateTime.local(2019, 1, 9),
                }],
            };

            const { absences: actual } =
                wrapper!.instance()._getGridDataWrapper(
                    dutyShifts.filters,
                    {
                        dutyShiftList: dutyShifts.data,
                        dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                        filters: dutyShifts.filters,
                    },
                    dutyAbsences,
                    [301],
                    { '301': {} as IScheduleListItem },
                    [],
                );
            const expected = {
                person1: [expect.objectContaining({ start: Duration.fromObject({ days: 7, hours: 21 }).as('days') })],
            };

            expect(actual).toEqual(expected);
        });

        it('for absences with a start before the calendar start date', () => {
            const dutyAbsences: DutyAbsenses = {
                filters: {},
                data: [{
                    ...absenceData,
                    start: DateTime.local(2018, 12, 31),
                }],
            };

            const { absences: actual } =
                wrapper!.instance()._getGridDataWrapper(
                    dutyShifts.filters,
                    {
                        dutyShiftList: dutyShifts.data,
                        dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                        filters: dutyShifts.filters,
                    },
                    dutyAbsences,
                    [301],
                    { '301': {} as IScheduleListItem },
                    [],
                );
            const expected = {
                person1: [expect.objectContaining({ start: Duration.fromObject({ days: -2, hours: 21 }).as('days') })],
            };

            expect(actual).toEqual(expected);
        });
    });

    describe('Properly counts days', () => {
        it('for full-day', () => {
            const dutyAbsences: DutyAbsenses = {
                filters: {},
                data: [{
                    ...absenceData,
                    start: DateTime.local(2019, 1, 9),
                    end: DateTime.local(2019, 1, 19),
                    fullDay: true,
                }],
            };

            const { absences: actual } =
                wrapper!.instance()._getGridDataWrapper(
                    dutyShifts.filters,
                    {
                        dutyShiftList: dutyShifts.data,
                        dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                        filters: dutyShifts.filters,
                    },
                    dutyAbsences,
                    [301],
                    { '301': {} as IScheduleListItem },
                    [],
                );
            const expected = {
                person1: [expect.objectContaining({ length: 10 })],
            };

            expect(actual).toEqual(expected);
        });

        it('for a few hours', () => {
            const dutyAbsences: DutyAbsenses = {
                filters: {},
                data: [{
                    ...absenceData,
                    start: DateTime.local(2019, 1, 13, 10, 30, 0),
                    end: DateTime.local(2019, 1, 13, 12, 0, 0),
                    fullDay: false,
                }],
            };

            const { absences: actual } =
                wrapper!.instance()._getGridDataWrapper(
                    dutyShifts.filters,
                    {
                        dutyShiftList: dutyShifts.data,
                        dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                        filters: dutyShifts.filters,
                    },
                    dutyAbsences,
                    [301],
                    { '301': {} as IScheduleListItem },
                    [],
                );
            const expected = {
                person1: [expect.objectContaining({ length: Duration.fromObject({ hours: 1, minutes: 30 }).as('days') })],
            };

            expect(actual).toEqual(expected);
        });

        it('for a few hours with day change', () => {
            const dutyAbsences: DutyAbsenses = {
                filters: {},
                data: [{
                    ...absenceData,
                    start: DateTime.local(2019, 1, 13, 20, 0, 0),
                    end: DateTime.local(2019, 1, 14, 2, 0, 0),
                    fullDay: false,
                }],
            };

            const { absences: actual } =
                wrapper!.instance()._getGridDataWrapper(
                    dutyShifts.filters,
                    {
                        dutyShiftList: dutyShifts.data,
                        dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                        filters: dutyShifts.filters,
                    },
                    dutyAbsences,
                    [301],
                    { '301': {} as IScheduleListItem },
                    [],
                );
            const expected = {
                person1: [expect.objectContaining({ length: Duration.fromObject({ hours: 6 }).as('days') })],
            };

            expect(actual).toEqual(expected);
        });

        it('for absence until midnight with fullDay option turned off', () => {
            const dutyAbsences: DutyAbsenses = {
                filters: {},
                data: [{
                    ...absenceData,
                    start: DateTime.local(2019, 1, 13, 22, 0, 0),
                    end: DateTime.local(2019, 1, 14, 0, 0, 0),
                    fullDay: false,
                }],
            };

            const { absences: actual } =
                wrapper!.instance()._getGridDataWrapper(
                    dutyShifts.filters,
                    {
                        dutyShiftList: dutyShifts.data,
                        dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                        filters: dutyShifts.filters,
                    },
                    dutyAbsences,
                    [301],
                    { '301': {} as IScheduleListItem },
                    [],
                );
            const expected = {
                person1: [expect.objectContaining({ length: Duration.fromObject({ hours: 2 }).as('days') })],
            };

            expect(actual).toEqual(expected);
        });
    });

    it('for person with "hasOwnProperty" login', () => {
        const dutyAbsences: DutyAbsenses = {
            filters: {},
            data: [{
                ...absenceData,
                id: 42,
                person: { login: 'hasOwnProperty' } as IAbsentPerson,
            }],
        };

        expect(() => {
            const { absences: actual } =
                wrapper!.instance()._getGridDataWrapper(
                    dutyShifts.filters,
                    {
                        dutyShiftList: dutyShifts.data,
                        dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                        filters: dutyShifts.filters,
                    },
                    dutyAbsences,
                    [301],
                    { '301': {} as IScheduleListItem },
                    [],
                );
            const expected = { hasOwnProperty: [expect.objectContaining({ id: 42 })] };

            expect(actual).toEqual(expected);
        }).not.toThrow();
    });
});

describe('_getGridDataWrapper prepares proper duty shifts data', () => {
    let wrapper: ShallowWrapper<
    _Internal_CalendarContainer['props'],
     _Internal_CalendarContainer['state'],
      _Internal_CalendarContainer
      > | null = null;

    const dutyAbsences = {
        filters: {},
        data: [],
    };

    const holidays = [
        { start: new Date('2019-01-05'), end: new Date('2019-01-06'), intervalType: EHolidayType.holiday },
        { start: new Date('2019-01-12'), end: new Date('2019-01-13'), intervalType: EHolidayType.weekend },
        { start: new Date('2019-01-19'), end: new Date('2019-01-20'), intervalType: EHolidayType.holiday },
    ];

    const props = {
        dutyPersons: [],
        dateFrom: 0,
        role: 0,
        user: {} as User,
        person: [],
        dutyPersonsLoading: false,
        updateHolidays: jest.fn(),
        hasScheduleEditChanges: false,
        queryObj: {},
        dutySchedulesLoading: false,
        dutySchedulesError: null,
        prev: null,
        next: null,
    };

    expect.extend({
        toMatchAsString(received, expected) {
            if (String(received) !== expected) {
                return { pass: false, message() { return `'${String(received)}' was expected to be '${expected}'` } };
            }

            return { pass: true, message() { return EScheduleStatus.ok } };
        },
    });

    beforeEach(() => {
        wrapper = shallow(
            <_Internal_CalendarContainer
                {...props}
                filters={{
                    serviceId: 1,
                    dateFrom: new Date(Date.UTC(2019, 0, 1)),
                    dateTo: new Date(Date.UTC(2019, 1, 15)),
                    scheduleId: 2,
                    dutyPerson: ['alimpiev'],
                    scale: EScaleType.day as DurationUnit,
                }}
                schedulesIds={[]}
                schedulesList={{}}
                deleteSchedule={jest.fn()}
                duty2Shifts={{
                    dutyShiftList: {},
                    dutyShiftIds: [],
                    filters: {
                        serviceId: 1,
                        dateFrom: new Date(Date.UTC(2019, 0, 1)),
                        dateTo: new Date(Date.UTC(2019, 1, 15)),
                        scheduleId: 2,
                        dutyPerson: ['alimpiev'],
                        scale: EScaleType.day as DurationUnit,
                    }
                }}
                fetchDutyShiftsList={jest.fn()}
                setFilters={jest.fn()}
                dutyShiftsLoading={false}
                dutyShiftsError={null}
                service={{ id: 101, slug: 'abc' }}
                holidayIntervals={selectHolidayIntervals({ dutyShifts: { holidays } } as DutyShiftsStoreField)}
                scale="day"
                dutyAbsences={{
                    filters: {},
                    data: [],
                }}
                interval={Duration.fromObject({ months: 2 })}
                updateDutyAbsences={jest.fn()}
                getMemberFromService={jest.fn()}
                mergeDutyShifts={jest.fn()}
                resetDutyShifts={jest.fn()}
                updateQueryStr={jest.fn()}
                filterQueryStr={jest.fn()}
                resetScheduleEdit={jest.fn()}
                requesterInService={false}
            />,
        );
    });

    afterEach(() => {
        wrapper!.unmount();
    });

    it('for duty shift with several holidays', () => {
        const dutyShifts = {
            filters: {
                dateFrom: new Date(Date.UTC(2019, 0, 1)),
                dateTo: new Date(Date.UTC(2019, 1, 15)),
                scale: EScaleType.day,
            } as ICalendarPageFilters,
            data: { [201]: {
                id: 201,
                staffId: 1,
                scheduleId: 301,
                start: '2019-01-01T15:00+03:00',
                end: '2019-01-22T15:00+03:00',
                slot: { isPrimary: false },
                staff: {
                    id: 1,
                    login: 'person1',
                    name: {
                        en: 'person 1',
                        ru: 'человек 1',
                    },
                },
            } as unknown } as Record<string, IDutyShiftListItem>,
            loading: false,
        };

        const { schedules: actual } = wrapper!.instance()._getGridDataWrapper(
            dutyShifts.filters,
            {
                dutyShiftList: dutyShifts.data,
                dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                filters: dutyShifts.filters,
            },
            dutyAbsences,
            [301],
            { '301': {
                id: 301,
                name: 'schedule name',
                description: 'schedule description',
            } as IScheduleListItem },
            selectHolidayIntervals({ dutyShifts: { holidays } } as DutyShiftsStoreField),
        );

        const expected = {
            '301': {
                id: 301,
                name: 'schedule name',
                description: 'schedule description',
                persons: {
                    person1: {
                        name: {
                            en: 'person 1',
                            ru: 'человек 1',
                        },
                        shifts: [
                            {
                                id: 201,
                                isApproved: false,
                                start: 0.5,
                                length: 21,
                                startDate: expect.toMatchDate(DateTime.fromISO('2019-01-01T15:00+03:00').toJSDate()),
                                endDate: expect.toMatchDate(DateTime.fromISO('2019-01-22T15:00+03:00').toJSDate()),
                                text: 'i18n:interval.dash.no-spaces, schedule name',
                                type: 'pending',
                                scheduleId: 301,
                                isPrimary: false,
                            },
                        ],
                    },
                },
                daysStatus: {
                    'Tue Jan 01 2019': EScheduleStatus.nothing,
                    'Wed Jan 02 2019': EScheduleStatus.nothing,
                    'Thu Jan 03 2019': EScheduleStatus.nothing,
                    'Fri Jan 04 2019': EScheduleStatus.nothing,
                    'Sat Jan 05 2019': EScheduleStatus.nothing,
                    'Sun Jan 06 2019': EScheduleStatus.nothing,
                    'Mon Jan 07 2019': EScheduleStatus.nothing,
                    'Tue Jan 08 2019': EScheduleStatus.nothing,
                    'Wed Jan 09 2019': EScheduleStatus.nothing,
                    'Thu Jan 10 2019': EScheduleStatus.nothing,
                    'Fri Jan 11 2019': EScheduleStatus.nothing,
                    'Sat Jan 12 2019': EScheduleStatus.nothing,
                    'Sun Jan 13 2019': EScheduleStatus.nothing,
                    'Mon Jan 14 2019': EScheduleStatus.nothing,
                    'Tue Jan 15 2019': EScheduleStatus.nothing,
                    'Wed Jan 16 2019': EScheduleStatus.nothing,
                    'Thu Jan 17 2019': EScheduleStatus.nothing,
                    'Fri Jan 18 2019': EScheduleStatus.nothing,
                    'Sat Jan 19 2019': EScheduleStatus.nothing,
                    'Sun Jan 20 2019': EScheduleStatus.nothing,
                    'Mon Jan 21 2019': EScheduleStatus.nothing,
                    'Tue Jan 22 2019': EScheduleStatus.nothing,
                },
            },
        };

        expect(actual).toEqual(expected);
    });

    it('for duty shifts with holidays', () => {
        const dutyShifts = {
            filters: {
                dateFrom: new Date(Date.UTC(2019, 0, 1)),
                dateTo: new Date(Date.UTC(2019, 1, 15)),
                scale: EScaleType.day,
            } as ICalendarPageFilters,
            data: {
                [201]: {
                    id: 201,
                    staffId: 1,
                    scheduleId: 301,
                    start: '2019-01-01T15:00+03:00',
                    end: '2019-01-08T15:00+03:00',
                    staff: {
                        id: 1,
                        login: 'person1',
                        name: {
                            en: 'person 1',
                            ru: 'человек 1',
                        },
                    },
                    slot: { isPrimary: false },
                } as unknown,
                [202]: {
                    id: 202,
                    staffId: 2,
                    scheduleId: 301,
                    start: '2019-01-08T15:00+03:00',
                    end: '2019-01-15T15:00+03:00',
                    staff: {
                        id: 2,
                        login: 'person2',
                        name: {
                            en: 'person 2',
                            ru: 'человек 2',
                        },
                    },
                    slot: { isPrimary: false },
                } as unknown,
                [203]: {
                    id: 203,
                    staffId: 1,
                    scheduleId: 301,
                    start: '2019-01-15T15:00+03:00',
                    end: '2019-01-22T15:00+03:00',
                    staff: {
                        id: 1,
                        login: 'person1',
                        name: {
                            en: 'person 1',
                            ru: 'человек 1',
                        },
                    },
                    slot: { isPrimary: false },
                } as unknown,
            } as Record<string, IDutyShiftListItem>,
            loading: false,
        };

        const { schedules: actual } = wrapper!.instance()._getGridDataWrapper(
            dutyShifts.filters,
            {
                dutyShiftList: dutyShifts.data,
                dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                filters: dutyShifts.filters,
            },
            dutyAbsences,
            [301],
            { '301': {
                id: 301,
                name: 'schedule name',
                description: 'schedule description',
            } as IScheduleListItem },
            selectHolidayIntervals({ dutyShifts: { holidays } } as DutyShiftsStoreField),
        );

        const expected = {
            '301': {
                id: 301,
                name: 'schedule name',
                description: 'schedule description',
                persons: {
                    person1: {
                        name: {
                            en: 'person 1',
                            ru: 'человек 1',
                        },
                        shifts: [
                            {
                                id: 201,
                                isApproved: false,
                                start: 0.5,
                                length: 7,
                                startDate: expect.toMatchDate(DateTime.fromISO('2019-01-01T15:00+03:00').toJSDate()),
                                endDate: expect.toMatchDate(DateTime.fromISO('2019-01-08T15:00+03:00').toJSDate()),
                                text: 'i18n:interval.dash.no-spaces, schedule name',
                                type: 'pending',
                                scheduleId: 301,
                                isPrimary: false,
                            },
                            {
                                id: 203,
                                isApproved: false,
                                start: 14.5,
                                length: 7,
                                startDate: DateTime.fromISO('2019-01-15T15:00+03:00').toJSDate(),
                                endDate: DateTime.fromISO('2019-01-22T15:00+03:00').toJSDate(),
                                text: 'i18n:interval.dash.no-spaces, schedule name',
                                type: 'pending',
                                scheduleId: 301,
                                isPrimary: false,
                            },
                        ],
                    },
                    person2: {
                        name: {
                            en: 'person 2',
                            ru: 'человек 2',
                        },
                        shifts: [
                            {
                                id: 202,
                                isApproved: false,
                                start: 7.5,
                                length: 7,
                                startDate: DateTime.fromISO('2019-01-08T15:00+03:00').toJSDate(),
                                endDate: DateTime.fromISO('2019-01-15T15:00+03:00').toJSDate(),
                                text: 'i18n:interval.dash.no-spaces, schedule name',
                                type: 'pending',
                                scheduleId: 301,
                                isPrimary: false,
                            },
                        ],
                    },
                },
                daysStatus: {
                    'Tue Jan 01 2019': EScheduleStatus.nothing,
                    'Wed Jan 02 2019': EScheduleStatus.nothing,
                    'Thu Jan 03 2019': EScheduleStatus.nothing,
                    'Fri Jan 04 2019': EScheduleStatus.nothing,
                    'Sat Jan 05 2019': EScheduleStatus.nothing,
                    'Sun Jan 06 2019': EScheduleStatus.nothing,
                    'Mon Jan 07 2019': EScheduleStatus.nothing,
                    'Tue Jan 08 2019': EScheduleStatus.nothing,
                    'Wed Jan 09 2019': EScheduleStatus.nothing,
                    'Thu Jan 10 2019': EScheduleStatus.nothing,
                    'Fri Jan 11 2019': EScheduleStatus.nothing,
                    'Sat Jan 12 2019': EScheduleStatus.nothing,
                    'Sun Jan 13 2019': EScheduleStatus.nothing,
                    'Mon Jan 14 2019': EScheduleStatus.nothing,
                    'Tue Jan 15 2019': EScheduleStatus.nothing,
                    'Wed Jan 16 2019': EScheduleStatus.nothing,
                    'Thu Jan 17 2019': EScheduleStatus.nothing,
                    'Fri Jan 18 2019': EScheduleStatus.nothing,
                    'Sat Jan 19 2019': EScheduleStatus.nothing,
                    'Sun Jan 20 2019': EScheduleStatus.nothing,
                    'Mon Jan 21 2019': EScheduleStatus.nothing,
                    'Tue Jan 22 2019': EScheduleStatus.nothing,
                },
            },
        };

        expect(actual).toEqual(expected);
    });
});

describe('_getGridDataWrapper calculates daysStatus properly', () => {
    let wrapper: ShallowWrapper<
    _Internal_CalendarContainer['props'],
     _Internal_CalendarContainer['state'],
      _Internal_CalendarContainer
      > | null = null;

    const dutyAbsences = {
        filters: {},
        data: [],
    };

    const props = {
        dutyPersons: [],
        dateFrom: 0,
        role: 0,
        user: {} as User,
        person: [],
        dutyPersonsLoading: false,
        updateHolidays: jest.fn(),
        hasScheduleEditChanges: false,
        filters: {
            serviceId: 1,
            dateFrom: new Date(Date.UTC(2019, 0, 1)),
            dateTo: new Date(Date.UTC(2019, 1, 15)),
            scheduleId: 2,
            dutyPerson: ['alimpiev'],
            scale: EScaleType.day as DurationUnit,
        },
        schedules: [],
        duty2Shifts: {
            dutyShiftList: {},
            dutyShiftIds: [],
            filters: {
                serviceId: 1,
                dateFrom: new Date(Date.UTC(2019, 0, 1)),
                dateTo: new Date(Date.UTC(2019, 1, 15)),
                scheduleId: 2,
                dutyPerson: ['alimpiev'],
                scale: EScaleType.day as DurationUnit,
            },
        },
        dutyShiftsLoading: false,
        dutyShiftsError: null,
        next: null,
        prev: null,
        queryObj: {},
        dutySchedulesLoading: false,
        dutySchedulesError: null,
    };

    expect.extend({
        toMatchAsString(received, expected) {
            if (String(received) !== expected) {
                return { pass: false, message() { return `'${String(received)}' was expected to be '${expected}'` } };
            }

            return { pass: true, message() { return EScheduleStatus.ok } };
        },
    });

    beforeEach(() => {
        wrapper = shallow<_Internal_CalendarContainer>(
            <_Internal_CalendarContainer
                {...props}
                filters={{
                    dateFrom: new Date(Date.UTC(2019, 0, 1)),
                    dateTo: new Date(Date.UTC(2019, 1, 15)),
                    scale: EScaleType.day,
                } as ICalendarPageFilters}
                service={{ id: 101, slug: 'abc' }}
                holidayIntervals={[]}
                scale="day"
                dutyAbsences={{
                    filters: {},
                    data: [],
                }}
                interval={Duration.fromObject({ months: 2 })}
                schedulesIds={[]}
                schedulesList={{}}
                deleteSchedule={jest.fn()}
                updateDutyAbsences={jest.fn()}
                getMemberFromService={jest.fn()}
                setFilters={jest.fn()}
                fetchDutyShiftsList={jest.fn()}
                mergeDutyShifts={jest.fn()}
                resetDutyShifts={jest.fn()}
                updateQueryStr={jest.fn()}
                filterQueryStr={jest.fn()}
                resetScheduleEdit={jest.fn()}
                requesterInService={false}
            />,
        );
    });

    afterEach(() => {
        wrapper!.unmount();
    });

    it('for multiple schedules having issues on different days', () => {
        const dutyShifts = {
            filters: {
                dateFrom: new Date(Date.UTC(2019, 0, 1)),
                dateTo: new Date(Date.UTC(2019, 1, 15)),
                scale: EScaleType.day,
            } as ICalendarPageFilters,
            data: {
                [201]: {
                    id: 201,
                    staffId: 1,
                    scheduleId: 301,
                    start: '2019-01-01T15:00+03:00',
                    end: '2019-01-08T15:00+03:00',
                    approved: true,
                    slot: { isPrimary: false },
                } as unknown,
                [202]: {
                    id: 202,
                    staffId: 2,
                    scheduleId: 301,
                    start: '2019-01-08T15:00+03:00',
                    end: '2019-01-15T15:00+03:00',
                    approved: true,
                    slot: { isPrimary: false },
                } as unknown,
                [203]: {
                    id: 203,
                    staffId: 2,
                    scheduleId: 301,
                    start: '2019-01-03T15:00+03:00',
                    end: '2019-01-10T15:00+03:00',
                    approved: true,
                    slot: { isPrimary: false },
                } as unknown,
                [204]: {
                    id: 204,
                    staffId: 1,
                    scheduleId: 301,
                    start: '2019-01-10T15:00+03:00',
                    end: '2019-01-17T15:00+03:00',
                    approved: false,
                    slot: { isPrimary: false },
                } as unknown,
            } as Record<string, IDutyShiftListItem>,
            loading: false,
        };

        const { schedules: actual } = wrapper!.instance()._getGridDataWrapper(
            dutyShifts.filters,
            {
                dutyShiftList: dutyShifts.data,
                dutyShiftIds: Object.keys(dutyShifts.data).map(Number),
                filters: dutyShifts.filters,
            },
            dutyAbsences,
            [301],
            { '301': {
                id: 301,
                name: 'schedule name',
                description: 'schedule description',
            } as IScheduleListItem },
            [],
        );

        const expected = {
            '301': expect.objectContaining({
                daysStatus: {
                    'Tue Jan 01 2019': EScheduleStatus.ok,
                    'Wed Jan 02 2019': EScheduleStatus.ok,
                    'Thu Jan 03 2019': EScheduleStatus.ok,
                    'Fri Jan 04 2019': EScheduleStatus.ok,
                    'Sat Jan 05 2019': EScheduleStatus.ok,
                    'Sun Jan 06 2019': EScheduleStatus.ok,
                    'Mon Jan 07 2019': EScheduleStatus.ok,
                    'Tue Jan 08 2019': EScheduleStatus.ok,
                    'Wed Jan 09 2019': EScheduleStatus.ok,
                    'Thu Jan 10 2019': EScheduleStatus.nothing,
                    'Fri Jan 11 2019': EScheduleStatus.nothing,
                    'Sat Jan 12 2019': EScheduleStatus.nothing,
                    'Sun Jan 13 2019': EScheduleStatus.nothing,
                    'Mon Jan 14 2019': EScheduleStatus.nothing,
                    'Tue Jan 15 2019': EScheduleStatus.nothing,
                    'Wed Jan 16 2019': EScheduleStatus.nothing,
                    'Thu Jan 17 2019': EScheduleStatus.nothing,
                },
            }),
        };

        expect(actual).toEqual(expected);
    });
});
