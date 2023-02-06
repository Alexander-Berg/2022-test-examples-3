import React from 'react';
import { shallow } from 'enzyme';

import { DateTime } from 'luxon';
import { AbcGap } from './CalendarGrid.legacy';
import { CalendarGrid } from './CalendarGrid';
import { EScheduleStatus, EAbsenceType } from '../../redux/DutySchedules.types';
import { EHolidayType, Person } from '../../redux/DutyShifts.types';

function doNothing() { }

type AbcGapType = { getCaption(...args: Array<{ valueof(): string }>): string };

describe('Should render DutyCalendarGrid', () => {
    it('usual', () => {
        const dateIntervalSpy = jest
            .spyOn<AbcGapType, 'getCaption'>(AbcGap, 'getCaption')
            .mockImplementation((...args) =>
                args.map(arg =>
                    arg.valueOf(),
                ).join(','),
            );

        const wrapper = shallow(
            <CalendarGrid
                data={{
                    schedule: {
                        id: 100,
                        name: 'scheduleName',
                        persons: {
                            'login1@': {
                                name: { ru: 'Имя1', en: 'Name1' },
                                shifts: [
                                    {
                                        id: 1,
                                        text: 'text',
                                        start: 0,
                                        length: 2,
                                        type: 'pending',
                                        isApproved: false,
                                        replaces: [],
                                        startDate: new Date(Date.UTC(2019, 1, 1)),
                                        endDate: new Date(Date.UTC(2019, 1, 2)),
                                        holidays: [],
                                        scheduleId: 1,
                                    },
                                    {
                                        id: 2,
                                        text: 'text',
                                        start: 3,
                                        length: 2,
                                        type: 'approved',
                                        isApproved: true,
                                        replaces: [],
                                        startDate: new Date(Date.UTC(2019, 1, 3)),
                                        endDate: new Date(Date.UTC(2019, 1, 4)),
                                        holidays: [],
                                        scheduleId: 1,
                                    },
                                ],
                                vteams: [],
                            },
                        },
                        daysStatus: {
                            'Fri Feb 01 2019': EScheduleStatus.warning,
                            'Sat Feb 02 2019': EScheduleStatus.warning,
                            'Sun Feb 03 2019': EScheduleStatus.ok,
                            'Mon Feb 04 2019': EScheduleStatus.ok,
                        },
                    },
                    daysList: [
                        new Date(Date.UTC(2019, 1, 1)),
                        new Date(Date.UTC(2019, 1, 2)),
                        new Date(Date.UTC(2019, 1, 3)),
                        new Date(Date.UTC(2019, 1, 4)),
                    ],
                    personsCount: 1,
                    holidaysList: [{
                        start: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 2))),
                        end: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 3))),
                        intervalType: EHolidayType.weekend,
                    }, {
                        start: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 3))),
                        end: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 4))),
                        intervalType: EHolidayType.weekend,
                    }],
                    absences: {
                        ['login1@']: [{
                            id: 1,
                            start: 0,
                            length: 1,
                            type: EAbsenceType.Absence,
                            startDate: new Date(Date.UTC(2019, 1, 1)),
                            endDate: new Date(Date.UTC(2019, 1, 2)),
                            workInAbsence: false,
                            fullDay: true,
                        }],
                    },
                }}
                service={{ id: 1, slug: 'someslug' }}
                shiftDetails={{}}
                onShiftHoverChange={doNothing}
                showDutyDetails={doNothing}
                hideDutyDetails={doNothing}
                user={{ login: 'login' }}
                hoveredShiftId={null}
                onOpenShiftEditClick={jest.fn()}
                start={new Date(Date.UTC(2019, 1, 1))}
                scale="day"
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();

        dateIntervalSpy.mockRestore();
    });

    it('with replaces', () => {
        const wrapper = shallow(
            <CalendarGrid
                data={{
                    schedule: {
                        id: 100,
                        name: 'scheduleName',
                        description: 'scheduleDescription',
                        persons: {
                            ['person1']: {
                                name: { ru: 'Имя1', en: 'Name1' },
                                shifts: [
                                    {
                                        id: 1,
                                        text: 'text',
                                        start: 0,
                                        length: 2,
                                        type: 'pending',
                                        isApproved: false,
                                        replaces: [
                                            {
                                                id: 201,
                                                startDate: new Date(Date.UTC(2019, 1, 1)),
                                                endDate: new Date(Date.UTC(2019, 1, 5)),
                                                isDeleted: false,
                                                start: 0,
                                                length: 1,
                                                person: {
                                                    login: 'login2@',
                                                    name: { ru: 'Имя2', en: 'Name2' },
                                                } as Person,
                                            },
                                            {
                                                id: 202,
                                                startDate: new Date(Date.UTC(2019, 1, 7)),
                                                endDate: new Date(Date.UTC(2019, 1, 12)),
                                                isDeleted: false,
                                                start: 2,
                                                length: 2,
                                                person: {
                                                    login: 'login3@',
                                                    name: { ru: 'Имя3', en: 'Name3' },
                                                } as Person,
                                            },
                                        ],
                                        startDate: new Date(Date.UTC(2019, 1, 1)),
                                        endDate: new Date(Date.UTC(2019, 1, 5)),
                                        holidays: [],
                                        scheduleId: 1,
                                    },
                                ],
                                vteams: [],
                            },
                        },
                        daysStatus: {
                            'Fri Feb 01 2019': EScheduleStatus.ok,
                            'Sat Feb 02 2019': EScheduleStatus.ok,
                            'Sun Feb 03 2019': EScheduleStatus.ok,
                            'Mon Feb 04 2019': EScheduleStatus.ok,
                            'Mon Feb 05 2019': EScheduleStatus.ok,
                        },
                    },
                    daysList: [
                        new Date(Date.UTC(2019, 1, 1)),
                        new Date(Date.UTC(2019, 1, 2)),
                        new Date(Date.UTC(2019, 1, 3)),
                        new Date(Date.UTC(2019, 1, 4)),
                        new Date(Date.UTC(2019, 1, 5)),
                    ],
                    personsCount: 1,
                    holidaysList: [{
                        start: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 2))),
                        end: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 3))),
                        intervalType: EHolidayType.weekend,
                    }, {
                        start: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 3))),
                        end: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 4))),
                        intervalType: EHolidayType.weekend,
                    }],
                    absences: {},
                }}
                service={{ id: 1, slug: 'someslug' }}
                shiftDetails={{}}
                showDutyDetails={doNothing}
                hideDutyDetails={doNothing}
                user={{ login: 'login' }}
                hoveredShiftId={null}
                onOpenShiftEditClick={jest.fn()}
                onShiftHoverChange={jest.fn()}
                start={new Date(Date.UTC(2019, 1, 1))}
                scale="day"
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with holidays', () => {
        const days = Array(21).fill(1)
            .map((v, i) => new Date(Date.UTC(2019, 1, v + i)));
        const wrapper = shallow(
            <CalendarGrid
                data={{
                    schedule: {
                        id: 100,
                        name: 'scheduleName',
                        description: 'scheduleDescription',
                        persons: {
                            person1: {
                                name: { ru: 'Имя1', en: 'Name1' },
                                shifts: [
                                    {
                                        id: 1,
                                        text: 'text',
                                        start: 0,
                                        length: 7,
                                        type: 'pending',
                                        isApproved: false,
                                        replaces: [],
                                        startDate: new Date(Date.UTC(2019, 1, 1)),
                                        endDate: new Date(Date.UTC(2019, 1, 7)),
                                        holidays: [{
                                            start: DateTime.utc(2019, 1, 1),
                                            end: DateTime.utc(2019, 1, 2),
                                        }, {
                                            start: DateTime.utc(2019, 1, 2),
                                            end: DateTime.utc(2019, 1, 3),
                                        }],
                                        scheduleId: 1,
                                    },
                                    {
                                        id: 3,
                                        text: 'text',
                                        start: 14,
                                        length: 7,
                                        type: 'approved',
                                        isApproved: false,
                                        replaces: [],
                                        startDate: new Date(Date.UTC(2019, 1, 15)),
                                        endDate: new Date(Date.UTC(2019, 1, 21)),
                                        holidays: [{
                                            start: DateTime.utc(2019, 1, 15),
                                            end: DateTime.utc(2019, 1, 16),
                                        }, {
                                            start: DateTime.utc(2019, 1, 16),
                                            end: DateTime.utc(2019, 1, 17),
                                        }],
                                        scheduleId: 1,
                                    },
                                ],
                                vteams: [],
                            },
                            person2: {
                                name: { ru: 'Имя2', en: 'Name2' },
                                shifts: [
                                    {
                                        id: 2,
                                        text: 'text',
                                        start: 7,
                                        length: 7,
                                        type: 'pending',
                                        isApproved: false,
                                        replaces: [],
                                        startDate: new Date(Date.UTC(2019, 1, 8)),
                                        endDate: new Date(Date.UTC(2019, 1, 14)),
                                        holidays: [{
                                            start: DateTime.utc(2019, 1, 9),
                                            end: DateTime.utc(2019, 1, 14),
                                        }],
                                        scheduleId: 1,
                                    },
                                ],
                                vteams: [],
                            },
                        },
                        daysStatus: days.map(day =>
                            day.toDateString()).reduce<Record<string, EScheduleStatus.ok>>((acc, value) => {
                                acc[value] = EScheduleStatus.ok;
                                return acc;
                            }, {}),
                    },
                    daysList: days,
                    personsCount: 2,
                    holidaysList: [{
                        start: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 1))),
                        end: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 1 + 1))),
                        intervalType: EHolidayType.weekend,
                    }, {
                        start: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 2))),
                        end: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 2 + 1))),
                        intervalType: EHolidayType.weekend,
                    }, {
                        start: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 9))),
                        end: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 9 + 4))),
                        intervalType: EHolidayType.weekend,
                    }, {
                        start: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 15))),
                        end: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 15 + 1))),
                        intervalType: EHolidayType.weekend,
                    }, {
                        start: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 16))),
                        end: DateTime.fromJSDate(new Date(Date.UTC(2019, 1, 1 + 16 + 1))),
                        intervalType: EHolidayType.weekend,
                    }],
                    absences: {},
                }}
                service={{ id: 1, slug: 'someslug' }}
                shiftDetails={{}}
                showDutyDetails={doNothing}
                hideDutyDetails={doNothing}
                user={{ login: 'login' }}
                hoveredShiftId={null}
                onOpenShiftEditClick={jest.fn()}
                onShiftHoverChange={jest.fn()}
                start={new Date(Date.UTC(2019, 1, 1))}
                scale="day"
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
