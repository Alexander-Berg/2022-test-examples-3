import React from 'react';
import { shallow } from 'enzyme';
import { DateTime, Duration } from 'luxon';
import { Calendar } from './Calendar';
import { EHolidayType, Filters } from '~/src/features/Duty/redux/DutyShifts.types';
import type { CalendarDutyShiftsData, CalendarService } from './Calendar.types';
import type { Schedule } from '../../redux/DutySchedules.types';
import type { User } from '~/src/common/context/types';
import type { AbcError } from '~/src/common/redux/common.types';

describe('Should render calendar', () => {
    it('filled', () => {
        const wrapper = shallow(
            <Calendar
                shiftEditId={1}
                user={{} as User}
                onDatePeriodChange={jest.fn()}
                dutyShifts={{
                    loading: false,
                    error: null,
                    data: [{} as CalendarDutyShiftsData],
                    filters: {
                        dateFrom: new Date(2019, 1, 26),
                    } as Filters,
                }}
                interval={Duration.fromObject({ months: 1 })}
                showChangeDutyScheduleButton
                canEditDutySettings
                openShiftEdit={() => null}
                closeShiftEdit={() => null}
                onDateFromChange={() => null}
                onRoleFilterChange={() => null}
                openScheduleEdit={() => null}
                closeScheduleEdit={() => null}
                gridData={{
                    schedules: {
                        '40': {
                            id: 40,
                            name: 'schedule0 name',
                            persons: {
                                person0Id: {
                                    name: {
                                        ru: 'имя0',
                                        en: 'name0',
                                    },
                                    shifts: [
                                        {
                                            id: 0,
                                            start: 1,
                                            length: 2,
                                            type: 'approved',
                                            replaces: [],
                                            startDate: new Date(2019, 1, 27),
                                            endDate: new Date(2019, 1, 28),
                                            scheduleId: 1,
                                            holidays: [],
                                        },
                                        {
                                            id: 1,
                                            start: 3,
                                            length: 1,
                                            type: 'pending',
                                            replaces: [],
                                            startDate: new Date(2019, 1, 29),
                                            endDate: new Date(2019, 1, 30),
                                            scheduleId: 1,
                                            holidays: [],
                                        },
                                    ],
                                    vteams: [],
                                },
                                person1Id: {
                                    name: {
                                        ru: 'имя1',
                                        en: 'name1',
                                    },
                                    shifts: [
                                        {
                                            id: 2,
                                            start: 2,
                                            length: 1,
                                            type: 'pending',
                                            replaces: [],
                                            startDate: new Date(2019, 1, 28),
                                            endDate: new Date(2019, 1, 29),
                                            scheduleId: 1,
                                            holidays: [],
                                        },
                                    ],
                                    vteams: [],
                                },
                            },
                            daysStatus: {},
                        },
                        '41': {
                            id: 41,
                            name: 'schedule1 name',
                            persons: {
                                person2Id: {
                                    name: {
                                        ru: 'имя2',
                                        en: 'name2',
                                    },
                                    shifts: [
                                        {
                                            id: 3,
                                            start: 1,
                                            length: 2,
                                            type: 'approved',
                                            replaces: [],
                                            startDate: new Date(2019, 1, 27),
                                            endDate: new Date(2019, 1, 29),
                                            scheduleId: 1,
                                            holidays: [],
                                        },
                                    ],
                                    vteams: [],
                                },
                            },
                            daysStatus: {},
                        },
                    },
                    personsCount: 3,
                    daysList: [
                        new Date(3000, 1, 26),
                        new Date(3000, 1, 27),
                        new Date(3000, 1, 28),
                        new Date(3000, 2, 1),
                        new Date(3000, 2, 2),
                        new Date(3000, 2, 3),
                        new Date(3000, 2, 4),
                    ],
                    holidaysList: [{
                        start: DateTime.fromJSDate(new Date(3000, 2, 2)),
                        end: DateTime.fromJSDate(new Date(3000, 2, 4)),
                        intervalType: EHolidayType.weekend,
                    }],
                    absences: {},
                }}
                settings={{
                    loading: false,
                    collection: [
                        { id: 40, name: 'schedule0 name' } as Schedule,
                        { id: 41, name: 'schedule1 name' } as Schedule,
                    ],
                    error: [],
                }}
                service={{ id: 1, slug: 'abc' }}
                userInDuty
                handleDutyPersonChange={() => null}
                handleDutyPersonMe={() => null}
                dutyPersons={[]}
                resetDutyShiftsError={jest.fn()}
                start={new Date(3000, 1, 26)}
                onScaleChange={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('empty', () => {
        const wrapper = shallow(
            <Calendar
                shiftEditId={1}
                user={{} as User}
                onDatePeriodChange={jest.fn()}
                service={{} as CalendarService}
                dutyShifts={{
                    loading: false,
                    error: null,
                    data: [],
                    filters: {} as Filters,
                }}
                interval={Duration.fromObject({ months: 0 })}
                openShiftEdit={() => null}
                closeShiftEdit={() => null}
                onDateFromChange={() => null}
                onRoleFilterChange={() => null}
                openScheduleEdit={() => null}
                closeScheduleEdit={() => null}
                gridData={{
                    schedules: {},
                    personsCount: 0,
                    daysList: [],
                    holidaysList: [],
                    absences: {},
                }}
                settings={{
                    collection: [],
                    loading: false,
                    error: [],
                }}
                userInDuty
                handleDutyPersonChange={() => null}
                handleDutyPersonMe={() => null}
                dutyPersons={[]}
                resetDutyShiftsError={jest.fn()}
                start={new Date(3000, 1, 26)}
                onScaleChange={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with error', () => {
        const error = new Error() as unknown as AbcError;

        error.data = {
            message: {
                ru: 'Текст ru message',
                en: 'Текст en message',
            },
        } as AbcError['data'];

        const wrapper = shallow(
            <Calendar
                shiftEditId={1}
                user={{} as User}
                onDatePeriodChange={jest.fn()}
                service={{} as CalendarService}
                dutyShifts={{
                    loading: false,
                    error,
                    data: [],
                    filters: {} as Filters,
                }}
                interval={Duration.fromObject({ months: 0 })}
                openShiftEdit={() => null}
                closeShiftEdit={() => null}
                onDateFromChange={() => null}
                onRoleFilterChange={() => null}
                openScheduleEdit={() => null}
                closeScheduleEdit={() => null}
                gridData={{
                    schedules: {},
                    personsCount: 0,
                    daysList: [],
                    holidaysList: [],
                    absences: {},
                }}
                settings={{
                    loading: false,
                    collection: [],
                    error: [],
                }}
                userInDuty
                handleDutyPersonChange={() => null}
                handleDutyPersonMe={() => null}
                dutyPersons={[]}
                resetDutyShiftsError={jest.fn()}
                start={new Date(3000, 1, 26)}
                onScaleChange={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
