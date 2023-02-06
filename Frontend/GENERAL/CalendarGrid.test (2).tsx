import React from 'react';
import { shallow } from 'enzyme';

import { DateTime } from 'luxon';
import { AbcGap } from './CalendarGrid.legacy';
import { CalendarGrid } from './CalendarGrid';
import { EScheduleStatus, EAbsenceType, EScheduleState } from '../../redux/DutySchedules.types';
import { EHolidayType } from '../../redux/DutyShifts.types';

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
                        revision: null,
                        recalculationInProcess: false,
                        serviceId: 1,
                        state: EScheduleState.active,
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
                                        startDate: new Date(Date.UTC(2019, 1, 1)),
                                        endDate: new Date(Date.UTC(2019, 1, 2)),
                                        scheduleId: 1,
                                        isPrimary: false,
                                        replacementForId: null,
                                    },
                                    {
                                        id: 2,
                                        text: 'text',
                                        start: 3,
                                        length: 2,
                                        type: 'approved',
                                        isApproved: true,
                                        startDate: new Date(Date.UTC(2019, 1, 3)),
                                        endDate: new Date(Date.UTC(2019, 1, 4)),
                                        scheduleId: 1,
                                        isPrimary: false,
                                        replacementForId: null,
                                    },
                                ],
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
                serviceId={1}
                shiftDetails={{}}
                onShiftHoverChange={doNothing}
                showDutyDetails={doNothing}
                hideDutyDetails={doNothing}
                user={{ login: 'login', id: 1, abc_id: 2 }}
                hoveredShiftId={null}
                onOpenShiftEditClick={jest.fn()}
                start={new Date(Date.UTC(2019, 1, 1))}
                scale="day"
                deleteScheduleLoading={false}
                onDeleteSchedule={jest.fn()}
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
                        revision: null,
                        recalculationInProcess: false,
                        serviceId: 1,
                        state: EScheduleState.active,
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
                                        startDate: new Date(Date.UTC(2019, 1, 1)),
                                        endDate: new Date(Date.UTC(2019, 1, 5)),
                                        scheduleId: 1,
                                        isPrimary: false,
                                        replacementForId: null,
                                    },
                                ],
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
                serviceId={1}
                shiftDetails={{}}
                showDutyDetails={doNothing}
                hideDutyDetails={doNothing}
                user={{ login: 'login', id: 1, abc_id: 2 }}
                hoveredShiftId={null}
                onOpenShiftEditClick={jest.fn()}
                onShiftHoverChange={jest.fn()}
                start={new Date(Date.UTC(2019, 1, 1))}
                scale="day"
                deleteScheduleLoading={false}
                onDeleteSchedule={jest.fn()}
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
                        revision: null,
                        recalculationInProcess: false,
                        serviceId: 1,
                        state: EScheduleState.active,
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
                                        startDate: new Date(Date.UTC(2019, 1, 1)),
                                        endDate: new Date(Date.UTC(2019, 1, 7)),
                                        scheduleId: 1,
                                        isPrimary: false,
                                        replacementForId: null,
                                    },
                                    {
                                        id: 3,
                                        text: 'text',
                                        start: 14,
                                        length: 7,
                                        type: 'approved',
                                        isApproved: false,
                                        startDate: new Date(Date.UTC(2019, 1, 15)),
                                        endDate: new Date(Date.UTC(2019, 1, 21)),
                                        scheduleId: 1,
                                        isPrimary: false,
                                        replacementForId: null,
                                    },
                                ],
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
                                        startDate: new Date(Date.UTC(2019, 1, 8)),
                                        endDate: new Date(Date.UTC(2019, 1, 14)),
                                        scheduleId: 1,
                                        isPrimary: false,
                                        replacementForId: null,
                                    },
                                ],
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
                serviceId={1}
                shiftDetails={{}}
                showDutyDetails={doNothing}
                hideDutyDetails={doNothing}
                user={{ login: 'login', id: 1, abc_id: 2 }}
                hoveredShiftId={null}
                onOpenShiftEditClick={jest.fn()}
                onShiftHoverChange={jest.fn()}
                start={new Date(Date.UTC(2019, 1, 1))}
                scale="day"
                deleteScheduleLoading={false}
                onDeleteSchedule={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
