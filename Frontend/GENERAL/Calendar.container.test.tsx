/* eslint-disable @typescript-eslint/no-non-null-assertion */
import React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import { DateTime, Duration, DurationUnit } from 'luxon';

import { _Internal_CalendarContainer } from './Calendar.container';
import type { User } from '~/src/common/context/types';
import { Filters, EHolidayType, Person } from '~/src/features/Duty/redux/DutyShifts.types';
import { Schedule, EScheduleStatus, EAbsenceType, AbsentPerson } from '~/src/features/Duty/redux/DutySchedules.types';
import type { CalendarDutyShifts, DutyAbsenses } from './Calendar.types';
import { selectHolidayIntervals } from '../../redux/DutyShifts.selectors';
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
    let wrapper: ShallowWrapper<_Internal_CalendarContainer['props'], _Internal_CalendarContainer['state'], _Internal_CalendarContainer> | null = null;

    const dutyShifts: CalendarDutyShifts = {
        filters: {
            dateFrom: new Date(Date.UTC(2019, 0, 1)),
            dateTo: new Date(Date.UTC(2019, 1, 15)),
            scale: 'day',
        } as Filters,
        data: [],
        loading: false,
    };

    const absenceData = {
        id: 401,
        start: DateTime.local(2019, 1, 9),
        end: DateTime.local(2019, 1, 10),
        type: EAbsenceType.Illness,
        person: { login: 'person1' } as AbsentPerson,
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
    };

    const settings = { collection: [], loading: false, error: [] };

    beforeEach(() => {
        wrapper = shallow(
            <_Internal_CalendarContainer
                {...props}
                settings={settings}
                service={{ id: 101, slug: 'abc' }}
                dutyShifts={dutyShifts}
                holidayIntervals={[]}
                scale="day"
                dutyAbsences={{
                    filters: {},
                    data: [],
                }}
                interval={Duration.fromObject({ months: 2 })}
                updateDutyShifts={jest.fn()}
                updateDutyAbsences={jest.fn()}
                mergeDutyShifts={jest.fn()}
                resetDutyShiftsError={jest.fn()}
                resetDutyShifts={jest.fn()}
                updateQueryStr={jest.fn()}
                filterQueryStr={jest.fn()}
                resetScheduleEdit={jest.fn()}
                dutyPersons={[]}
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
                { ...absenceData, id: 3, person: { login: 'person2' } as AbsentPerson },
            ],
        };

        const { absences: actual } = wrapper!.instance()._getGridDataWrapper(dutyShifts, dutyAbsences, settings, []);
        const expected = {
            person1: [{
                id: 1,
                start: 8,
                length: 1,
                startDate: expect.toMatchDate(new Date(2019, 0, 9)),
                endDate: expect.toMatchDate(new Date(2019, 0, 10)),
                type: 'illness',
                fullDay: true,
                workInAbsence: true,
            }, {
                id: 2,
                start: 8,
                length: 1,
                startDate: expect.toMatchDate(new Date(2019, 0, 9)),
                endDate: expect.toMatchDate(new Date(2019, 0, 10)),
                type: 'illness',
                fullDay: true,
                workInAbsence: true,
            }],
            person2: [{
                id: 3,
                start: 8,
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
                wrapper!.instance()._getGridDataWrapper(dutyShifts, dutyAbsences, settings, []);
            const expected = {
                person1: [expect.objectContaining({ start: 8 })],
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
                wrapper!.instance()._getGridDataWrapper(dutyShifts, dutyAbsences, settings, []);
            const expected = {
                person1: [expect.objectContaining({ start: -1 })],
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
                wrapper!.instance()._getGridDataWrapper(dutyShifts, dutyAbsences, settings, []);
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
                wrapper!.instance()._getGridDataWrapper(dutyShifts, dutyAbsences, settings, []);
            const expected = {
                person1: [expect.objectContaining({ length: 1 })],
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
                wrapper!.instance()._getGridDataWrapper(dutyShifts, dutyAbsences, settings, []);
            const expected = {
                person1: [expect.objectContaining({ length: 2 })],
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
                wrapper!.instance()._getGridDataWrapper(dutyShifts, dutyAbsences, settings, []);
            const expected = {
                person1: [expect.objectContaining({ length: 1 })],
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
                person: { login: 'hasOwnProperty' } as AbsentPerson,
            }],
        };

        expect(() => {
            const { absences: actual } =
                wrapper!.instance()._getGridDataWrapper(dutyShifts, dutyAbsences, settings, []);
            const expected = { hasOwnProperty: [expect.objectContaining({ id: 42 })] };

            expect(actual).toEqual(expected);
        }).not.toThrow();
    });
});

describe('_getGridDataWrapper prepares proper duty shifts data', () => {
    let wrapper: ShallowWrapper<_Internal_CalendarContainer['props'], _Internal_CalendarContainer['state'], _Internal_CalendarContainer> | null = null;

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
                settings={{
                    collection: [{
                        id: 301,
                        name: 'schedule name',
                        description: 'schedule description',
                        requesterInDuty: false,
                    } as Schedule],
                    loading: false,
                    error: [],
                }}
                service={{ id: 101, slug: 'abc' }}
                dutyShifts={{
                    filters: {
                        dateFrom: new Date(Date.UTC(2019, 0, 1)),
                        dateTo: new Date(Date.UTC(2019, 1, 15)),
                        scale: 'day',
                    } as Filters,
                    data: [],
                    loading: false,
                }}
                holidayIntervals={selectHolidayIntervals({ dutyShifts: { holidays } } as DutyShiftsStoreField)}
                scale="day"
                dutyAbsences={{
                    filters: {},
                    data: [],
                }}
                interval={Duration.fromObject({ months: 2 })}
                updateDutyShifts={jest.fn()}
                updateDutyAbsences={jest.fn()}
                mergeDutyShifts={jest.fn()}
                resetDutyShiftsError={jest.fn()}
                resetDutyShifts={jest.fn()}
                updateQueryStr={jest.fn()}
                filterQueryStr={jest.fn()}
                resetScheduleEdit={jest.fn()}
                dutyPersons={[]}
            />,
        );
    });

    afterEach(() => {
        wrapper!.unmount();
    });

    it('for duty shift with replaces', () => {
        const dutyShifts: CalendarDutyShifts = {
            filters: {
                dateFrom: new Date(Date.UTC(2019, 0, 1)),
                dateTo: new Date(Date.UTC(2019, 1, 15)),
                scale: 'day',
            } as Filters,
            data: [{
                id: 201,
                person: {
                    login: 'person1',
                    name: {
                        en: 'person 1',
                        ru: 'человек 1',
                    },
                    vteams: [],
                },
                schedule: { id: 301 },
                replaces: [
                    {
                        id: 401,
                        start: '2019-01-01',
                        end: '2019-01-01',
                        start_datetime: '2019-01-01T00:00:00',
                        end_datetime: '2019-01-02T00:00:00',
                        person: { login: 'person1' } as Person,
                    },
                    {
                        id: 402,
                        start: '2019-01-03',
                        end: '2019-01-03',
                        start_datetime: '2019-01-03T00:00:00',
                        end_datetime: '2019-01-04T00:00:00',
                        person: { login: 'person2' } as Person,
                    },
                    {
                        id: 402,
                        start: '2019-01-04',
                        end: '2019-01-05',
                        start_datetime: '2019-01-04T00:00:00',
                        end_datetime: '2019-01-06T00:00:00',
                        person: { login: 'person3' } as Person,
                    },
                ],
                start_datetime: '2019-01-01T15:00+03:00',
                end_datetime: '2019-01-06T15:00+03:00',
            }],
            loading: false,
        };

        const { schedules: actual } = wrapper!.instance()._getGridDataWrapper(
            dutyShifts,
            dutyAbsences,
            {
                collection: [{
                    id: 301,
                    name: 'schedule name',
                    description: 'schedule description',
                    dutyOnHolidays: false,
                    dutyOnWeekends: true,
                    requesterInDuty: false,
                } as Schedule],
                loading: false,
                error: [],
            },
            selectHolidayIntervals({ dutyShifts: { holidays } } as DutyShiftsStoreField),
        );

        const expected = {
            '301': {
                id: 301,
                name: 'schedule name',
                description: 'schedule description',
                dutyOnHolidays: false,
                dutyOnWeekends: true,
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
                                holidays: [{
                                    start: expect.toMatchDate(DateTime.fromISO('2019-01-05T00:00:00.000+03:00').toJSDate()),
                                    end: expect.toMatchDate(DateTime.fromISO('2019-01-06T23:59:59.999+03:00').toJSDate()),
                                }],
                                replaces: [
                                    {
                                        id: 401,
                                        start: -0.5,
                                        length: 1,
                                        person: { login: 'person1' },
                                        startDate: expect.toMatchDate(new Date(2019, 0, 1)),
                                        endDate: expect.toMatchDate(new Date(2019, 0, 2)),
                                        isDeleted: false,
                                    },
                                    {
                                        id: 402,
                                        start: 1.5,
                                        length: 1,
                                        person: { login: 'person2' },
                                        startDate: expect.toMatchDate(new Date(2019, 0, 3)),
                                        endDate: expect.toMatchDate(new Date(2019, 0, 4)),
                                        isDeleted: false,
                                    },
                                    {
                                        id: 402,
                                        start: 2.5,
                                        length: 2,
                                        person: { login: 'person3' },
                                        startDate: expect.toMatchDate(new Date(2019, 0, 4)),
                                        endDate: expect.toMatchDate(new Date(2019, 0, 6)),
                                        isDeleted: false,
                                    },
                                ],
                                start: 0.5,
                                length: 5,
                                startDate: expect.toMatchDate(DateTime.fromISO('2019-01-01T15:00+03:00').toJSDate()),
                                endDate: expect.toMatchDate(DateTime.fromISO('2019-01-06T15:00+03:00').toJSDate()),
                                text: 'i18n:interval.dash.no-spaces, schedule name',
                                type: 'pending',
                                scheduleId: 301,
                            },
                        ],
                        vteams: [],
                    },
                },
                daysStatus: {
                    'Tue Jan 01 2019': EScheduleStatus.nothing,
                    'Wed Jan 02 2019': EScheduleStatus.nothing,
                    'Thu Jan 03 2019': EScheduleStatus.nothing,
                    'Fri Jan 04 2019': EScheduleStatus.nothing,
                    'Sat Jan 05 2019': EScheduleStatus.nothing,
                    'Sun Jan 06 2019': EScheduleStatus.nothing,
                },
                requesterInDuty: false,
            },
        };

        expect(actual).toEqual(expected);
    });

    it('for duty shift with several holidays', () => {
        const dutyShifts: CalendarDutyShifts = {
            filters: {
                dateFrom: new Date(Date.UTC(2019, 0, 1)),
                dateTo: new Date(Date.UTC(2019, 1, 15)),
                scale: 'day',
            } as Filters,
            data: [{
                id: 201,
                person: {
                    login: 'person1',
                    name: {
                        en: 'person 1',
                        ru: 'человек 1',
                    },
                    vteams: [],
                },
                schedule: { id: 301 },
                replaces: [],
                start_datetime: '2019-01-01T15:00+03:00',
                end_datetime: '2019-01-22T15:00+03:00',
            }],
            loading: false,
        };

        const { schedules: actual } = wrapper!.instance()._getGridDataWrapper(
            dutyShifts,
            dutyAbsences,
            {
                collection: [{
                    id: 301,
                    name: 'schedule name',
                    description: 'schedule description',
                    dutyOnHolidays: false,
                    dutyOnWeekends: true,
                    requesterInDuty: false,
                } as Schedule],
                loading: false,
                error: [],
            },
            selectHolidayIntervals({ dutyShifts: { holidays } } as DutyShiftsStoreField),
        );

        const expected = {
            '301': {
                id: 301,
                name: 'schedule name',
                description: 'schedule description',
                dutyOnHolidays: false,
                dutyOnWeekends: true,
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
                                holidays: [{
                                    start: expect.toMatchDate(DateTime.fromISO('2019-01-05T00:00:00.000+03:00').toJSDate()),
                                    end: expect.toMatchDate(DateTime.fromISO('2019-01-06T23:59:59.999+03:00').toJSDate()),
                                }, {
                                    start: expect.toMatchDate(DateTime.fromISO('2019-01-19T00:00:00.000+03:00').toJSDate()),
                                    end: expect.toMatchDate(DateTime.fromISO('2019-01-20T23:59:59.999+03:00').toJSDate()),
                                }],
                                replaces: [],
                                start: 0.5,
                                length: 21,
                                startDate: expect.toMatchDate(DateTime.fromISO('2019-01-01T15:00+03:00').toJSDate()),
                                endDate: expect.toMatchDate(DateTime.fromISO('2019-01-22T15:00+03:00').toJSDate()),
                                text: 'i18n:interval.dash.no-spaces, schedule name',
                                type: 'pending',
                                scheduleId: 301,
                            },
                        ],
                        vteams: [],
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
                requesterInDuty: false,
            },
        };

        expect(actual).toEqual(expected);
    });

    it('for duty shifts with holidays', () => {
        const dutyShifts: CalendarDutyShifts = {
            filters: {
                dateFrom: new Date(Date.UTC(2019, 0, 1)),
                dateTo: new Date(Date.UTC(2019, 1, 15)),
                scale: 'day',
            } as Filters,
            data: [{
                id: 201,
                person: {
                    login: 'person1',
                    name: {
                        en: 'person 1',
                        ru: 'человек 1',
                    },
                    vteams: [],
                },
                schedule: { id: 301 },
                replaces: [],
                start_datetime: '2019-01-01T15:00+03:00',
                end_datetime: '2019-01-08T15:00+03:00',
            }, {
                id: 202,
                person: {
                    login: 'person2',
                    name: {
                        en: 'person 2',
                        ru: 'человек 2',
                    },
                    vteams: [],
                },
                schedule: {
                    id: 301,
                    name: 'schedule name',
                    description: 'schedule description',
                } as Schedule,
                replaces: [],
                start_datetime: '2019-01-08T15:00+03:00',
                end_datetime: '2019-01-15T15:00+03:00',
            }, {
                id: 203,
                person: {
                    login: 'person1',
                    name: {
                        en: 'person 1',
                        ru: 'человек 1',
                    },
                    vteams: [],
                },
                schedule: {
                    id: 301,
                    name: 'schedule name',
                    description: 'schedule description',
                } as Schedule,
                replaces: [],
                start_datetime: '2019-01-15T15:00+03:00',
                end_datetime: '2019-01-22T15:00+03:00',
            }],
            loading: false,
        };

        const { schedules: actual } = wrapper!.instance()._getGridDataWrapper(
            dutyShifts,
            dutyAbsences,
            {
                collection: [{
                    id: 301,
                    name: 'schedule name',
                    description: 'schedule description',
                    dutyOnHolidays: false,
                    dutyOnWeekends: true,
                    requesterInDuty: false,
                } as Schedule],
                loading: false,
                error: [],
            },
            selectHolidayIntervals({ dutyShifts: { holidays } } as DutyShiftsStoreField),
        );

        const expected = {
            '301': {
                id: 301,
                name: 'schedule name',
                description: 'schedule description',
                dutyOnHolidays: false,
                dutyOnWeekends: true,
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
                                holidays: [
                                    {
                                        start: expect.toMatchDate(DateTime.fromISO('2019-01-05T00:00:00.000+03:00').toJSDate()),
                                        end: expect.toMatchDate(DateTime.fromISO('2019-01-06T23:59:59.999+03:00').toJSDate()),
                                    },
                                ],
                                replaces: [],
                                start: 0.5,
                                length: 7,
                                startDate: expect.toMatchDate(DateTime.fromISO('2019-01-01T15:00+03:00').toJSDate()),
                                endDate: expect.toMatchDate(DateTime.fromISO('2019-01-08T15:00+03:00').toJSDate()),
                                text: 'i18n:interval.dash.no-spaces, schedule name',
                                type: 'pending',
                                scheduleId: 301,
                            },
                            {
                                id: 203,
                                isApproved: false,
                                holidays: [
                                    {
                                        start: expect.toMatchDate(DateTime.fromISO('2019-01-19T00:00:00.000+03:00').toJSDate()),
                                        end: expect.toMatchDate(DateTime.fromISO('2019-01-20T23:59:59.999+03:00').toJSDate()),
                                    },
                                ],
                                replaces: [],
                                start: 14.5,
                                length: 7,
                                startDate: DateTime.fromISO('2019-01-15T15:00+03:00').toJSDate(),
                                endDate: DateTime.fromISO('2019-01-22T15:00+03:00').toJSDate(),
                                text: 'i18n:interval.dash.no-spaces, schedule name',
                                type: 'pending',
                                scheduleId: 301,
                            },
                        ],
                        vteams: [],
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
                                holidays: [],
                                replaces: [],
                                start: 7.5,
                                length: 7,
                                startDate: DateTime.fromISO('2019-01-08T15:00+03:00').toJSDate(),
                                endDate: DateTime.fromISO('2019-01-15T15:00+03:00').toJSDate(),
                                text: 'i18n:interval.dash.no-spaces, schedule name',
                                type: 'pending',
                                scheduleId: 301,
                            },
                        ],
                        vteams: [],
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
                requesterInDuty: false,
            },
        };

        expect(actual).toEqual(expected);
    });
});

describe('_getGridDataWrapper calculates daysStatus properly', () => {
    let wrapper: ShallowWrapper<_Internal_CalendarContainer['props'], _Internal_CalendarContainer['state'], _Internal_CalendarContainer> | null = null;

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
                settings={{ collection: [], loading: false, error: [] }}
                service={{ id: 101, slug: 'abc' }}
                dutyShifts={{
                    filters: {
                        dateFrom: new Date(Date.UTC(2019, 0, 1)),
                        dateTo: new Date(Date.UTC(2019, 1, 15)),
                        scale: 'day',
                    } as Filters,
                    data: [],
                    loading: false,
                }}
                holidayIntervals={[]}
                scale="day"
                dutyAbsences={{
                    filters: {},
                    data: [],
                }}
                interval={Duration.fromObject({ months: 2 })}
                updateDutyShifts={jest.fn()}
                updateDutyAbsences={jest.fn()}
                mergeDutyShifts={jest.fn()}
                resetDutyShiftsError={jest.fn()}
                resetDutyShifts={jest.fn()}
                updateQueryStr={jest.fn()}
                filterQueryStr={jest.fn()}
                resetScheduleEdit={jest.fn()}
                dutyPersons={[]}
            />,
        );
    });

    afterEach(() => {
        wrapper!.unmount();
    });

    it('for multiple schedules having issues on different days', () => {
        const dutyShifts: CalendarDutyShifts = {
            filters: {
                dateFrom: new Date(Date.UTC(2019, 0, 1)),
                dateTo: new Date(Date.UTC(2019, 1, 15)),
                scale: 'day',
            } as Filters,
            data: [{
                id: 201,
                person: {
                    login: 'person1',
                    name: {
                        en: 'person 1',
                        ru: 'человек 1',
                    },
                    vteams: [],
                },
                schedule: {
                    id: 301,
                    name: 'schedule name',
                    description: 'schedule description',
                } as Schedule,
                replaces: [],
                start_datetime: '2019-01-01T15:00+03:00',
                end_datetime: '2019-01-08T15:00+03:00',
                is_approved: true,
            }, {
                id: 202,
                person: {
                    login: 'person2',
                    name: {
                        en: 'person 2',
                        ru: 'человек 2',
                    },
                    vteams: [],
                },
                schedule: {
                    id: 301,
                    name: 'schedule name',
                    description: 'schedule description',
                } as Schedule,
                replaces: [],
                start_datetime: '2019-01-08T15:00+03:00',
                end_datetime: '2019-01-15T15:00+03:00',
                is_approved: true,
                problems_count: 1,
            }, {
                id: 203,
                person: {
                    login: 'person2',
                    name: {
                        en: 'person 2',
                        ru: 'человек 2',
                    },
                    vteams: [],
                },
                schedule: {
                    id: 302,
                    name: 'other schedule name',
                    description: 'other schedule description',
                } as Schedule,
                replaces: [],
                start_datetime: '2019-01-03T15:00+03:00',
                end_datetime: '2019-01-10T15:00+03:00',
                is_approved: true,
            }, {
                id: 204,
                person: {
                    login: 'person1',
                    name: {
                        en: 'person 1',
                        ru: 'человек 1',
                    },
                    vteams: [],
                },
                schedule: {
                    id: 302,
                    name: 'other schedule name',
                    description: 'other schedule description',
                } as Schedule,
                replaces: [],
                start_datetime: '2019-01-10T15:00+03:00',
                end_datetime: '2019-01-17T15:00+03:00',
                is_approved: false,
            }],
            loading: false,
        };

        const { schedules: actual } = wrapper!.instance()._getGridDataWrapper(
            dutyShifts,
            dutyAbsences,
            { collection: [{ id: 301 }, { id: 302 }] as Schedule[], loading: false, error: [] },
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
                    'Tue Jan 08 2019': EScheduleStatus.warning,
                    'Wed Jan 09 2019': EScheduleStatus.warning,
                    'Thu Jan 10 2019': EScheduleStatus.warning,
                    'Fri Jan 11 2019': EScheduleStatus.warning,
                    'Sat Jan 12 2019': EScheduleStatus.warning,
                    'Sun Jan 13 2019': EScheduleStatus.warning,
                    'Mon Jan 14 2019': EScheduleStatus.warning,
                    'Tue Jan 15 2019': EScheduleStatus.warning,
                },
            }),
            '302': expect.objectContaining({
                daysStatus: {
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

describe('DutyCalendarContainer functions', () => {
    const props = {
        settings: { collection: [], loading: false, error: [] },
        service: { id: 101, slug: 'abc' },
        dutyShifts: { filters: { scale: 'day' } as Filters, data: [], loading: false, error: null, hasChanges: false },
        scale: 'day' as DurationUnit,
        holidayIntervals: [],
        dutyAbsences: { filters: {}, data: [] },
        interval: Duration.fromObject({ months: 2 }),
        updateDutyAbsences: jest.fn(),
        mergeDutyShifts: jest.fn(),
        resetDutyShiftsError: jest.fn(),
        resetDutyShifts: jest.fn(),
        updateQueryStr: jest.fn(),
        filterQueryStr: jest.fn(),
        dutyPersons: [],
        dateFrom: 0,
        role: 0,
        user: {} as User,
        person: [],
        dutyPersonsLoading: false,
        updateHolidays: jest.fn(),
    };

    it('if schedule edit has not changes then no shift update', () => {
        const updateDutyShifts = jest.fn();
        const resetScheduleEdit = jest.fn();

        const wrapper = shallow<_Internal_CalendarContainer>(
            <_Internal_CalendarContainer
                {...props}

                updateDutyShifts={updateDutyShifts}
                resetScheduleEdit={resetScheduleEdit}
                hasScheduleEditChanges={false}
            />,
        );

        wrapper.instance().closeScheduleEdit();

        expect(updateDutyShifts).not.toHaveBeenCalled();
        expect(resetScheduleEdit).toHaveBeenCalled();
    });

    it('if schedule edit has changes then shift update', () => {
        const updateDutyShifts = jest.fn();
        const resetScheduleEdit = jest.fn();

        const wrapper = shallow<_Internal_CalendarContainer>(
            <_Internal_CalendarContainer
                {...props}

                updateDutyShifts={updateDutyShifts}
                resetScheduleEdit={resetScheduleEdit}
                hasScheduleEditChanges
            />,
        );

        wrapper.instance().closeScheduleEdit();

        expect(updateDutyShifts).toHaveBeenCalled();
        expect(resetScheduleEdit).toHaveBeenCalled();
    });
});
