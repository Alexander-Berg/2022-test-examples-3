import React from 'react';
import { shallow } from 'enzyme';
import { DateTime, Duration } from 'luxon';
import { Calendar } from './Calendar';
import { EHolidayType, ICalendarPageFilters } from '~/src/features/Duty2/redux/DutyShifts.types';
import { EScheduleState } from '../../redux/DutySchedules.types';
import type { User } from './Calendar.types';
import type { IScheduleListItem } from '../../redux/DutySchedules.types';
import type { AbcError } from '~/src/common/redux/common.types';

describe('Should render calendar', () => {
    it('filled', () => {
        const wrapper = shallow(
            <Calendar
                shiftEdit={{
                    id: null,
                    replacementForId: null,
                }}
                user={{} as User}
                onDatePeriodChange={jest.fn()}
                filters={{
                    dateFrom: new Date(2019, 1, 26),
                } as ICalendarPageFilters}
                loading={false}
                error={null}
                interval={Duration.fromObject({ months: 1 })}
                showChangeDutyScheduleButton
                canEditDutySettings
                onDeleteSchedule={() => null}
                openShiftEdit={() => null}
                closeShiftEdit={() => null}
                onDateFromChange={() => null}
                onRoleFilterChange={() => null}
                openScheduleEdit={() => null}
                closeScheduleEdit={() => null}
                next={null}
                prev={null}
                dutyShiftsLoading={false}
                gridData={{
                    schedules: {
                        '40': {
                            id: 40,
                            name: 'schedule0 name',
                            revision: null,
                            recalculationInProcess: false,
                            serviceId: 1,
                            state: EScheduleState.active,
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
                                            startDate: new Date(2019, 1, 27),
                                            endDate: new Date(2019, 1, 28),
                                            scheduleId: 1,
                                            isPrimary: false,
                                            replacementForId: null,
                                        },
                                        {
                                            id: 1,
                                            start: 3,
                                            length: 1,
                                            type: 'pending',
                                            startDate: new Date(2019, 1, 29),
                                            endDate: new Date(2019, 1, 30),
                                            scheduleId: 1,
                                            isPrimary: false,
                                            replacementForId: null,
                                        },
                                    ],
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
                                            startDate: new Date(2019, 1, 28),
                                            endDate: new Date(2019, 1, 29),
                                            scheduleId: 1,
                                            isPrimary: false,
                                            replacementForId: null,
                                        },
                                    ],
                                },
                            },
                            daysStatus: {},
                        },
                        '41': {
                            id: 41,
                            name: 'schedule1 name',
                            revision: null,
                            recalculationInProcess: false,
                            serviceId: 1,
                            state: EScheduleState.active,
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
                                            startDate: new Date(2019, 1, 27),
                                            endDate: new Date(2019, 1, 29),
                                            scheduleId: 1,
                                            isPrimary: false,
                                            replacementForId: null,
                                        },
                                    ],
                                },
                            },
                            daysStatus: {},
                        },
                    },
                    schedulesIds: [40, 41],
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
                schedulesIds={[40]}
                schedulesList={{ '40': { id: 40, name: 'schedule0 name' } as IScheduleListItem }}
                serviceId={1}
                userInDuty
                handleDutyPersonChange={() => null}
                handleDutyPersonMe={() => null}
                start={new Date(3000, 1, 26)}
                onScaleChange={jest.fn()}
                containBackupDuty
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('empty', () => {
        const wrapper = shallow(
            <Calendar
                shiftEdit={{
                    id: null,
                    replacementForId: null,
                }}
                user={{} as User}
                onDatePeriodChange={jest.fn()}
                serviceId={1}
                filters={{} as ICalendarPageFilters}
                loading={false}
                error={null}
                interval={Duration.fromObject({ months: 0 })}
                onDeleteSchedule={() => null}
                openShiftEdit={() => null}
                closeShiftEdit={() => null}
                onDateFromChange={() => null}
                onRoleFilterChange={() => null}
                openScheduleEdit={() => null}
                closeScheduleEdit={() => null}
                next={null}
                prev={null}
                dutyShiftsLoading={false}
                gridData={{
                    schedules: {},
                    schedulesIds: [],
                    personsCount: 0,
                    daysList: [],
                    holidaysList: [],
                    absences: {},
                }}
                schedulesIds={[]}
                schedulesList={{}}
                userInDuty
                handleDutyPersonChange={() => null}
                handleDutyPersonMe={() => null}
                start={new Date(3000, 1, 26)}
                onScaleChange={jest.fn()}
                containBackupDuty={false}
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
                shiftEdit={{
                    id: null,
                    replacementForId: null,
                }}
                user={{} as User}
                onDatePeriodChange={jest.fn()}
                serviceId={1}
                filters={{} as ICalendarPageFilters}
                loading={false}
                error={null}
                interval={Duration.fromObject({ months: 0 })}
                onDeleteSchedule={() => null}
                openShiftEdit={() => null}
                closeShiftEdit={() => null}
                onDateFromChange={() => null}
                onRoleFilterChange={() => null}
                openScheduleEdit={() => null}
                closeScheduleEdit={() => null}
                next={null}
                prev={null}
                dutyShiftsLoading={false}
                gridData={{
                    schedules: {},
                    schedulesIds: [],
                    personsCount: 0,
                    daysList: [],
                    holidaysList: [],
                    absences: {},
                }}
                schedulesIds={[]}
                schedulesList={{}}
                userInDuty
                handleDutyPersonChange={() => null}
                handleDutyPersonMe={() => null}
                start={new Date(3000, 1, 26)}
                onScaleChange={jest.fn()}
                containBackupDuty={false}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
