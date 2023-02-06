import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyScheduleEdit__Main from 'b:abc-duty-schedule-edit e:main'; // eslint-disable-line camelcase

describe('Should render schedule edit main area', () => {
    it('simple', () => {
        const wrapper = shallow(
            <AbcDutyScheduleEdit__Main
                schedules={{
                    '42': {
                        name: 'schedule1',
                        shifts: [{
                            id: 1,
                            start: new Date(Date.UTC(2010, 1, 1)),
                            end: new Date(Date.UTC(2010, 1, 10)),
                            isApproved: true,
                            problemsCount: 1,
                        }],
                    },
                    '146': {
                        name: 'schedule2',
                        shifts: [{
                            id: 2,
                            start: new Date(Date.UTC(2010, 1, 11)),
                            end: new Date(Date.UTC(2010, 1, 20)),
                            isApproved: false,
                            problemsCount: 0,
                        }],
                    },
                    '404': {
                        name: 'schedule3',
                        shifts: [],
                    },
                }}
                absences={[{
                    id: 10,
                    type: 'vacation',
                    start: new Date(Date.UTC(2010, 1, 5)),
                    end: new Date(Date.UTC(2010, 1, 15)),
                    person: { login: 'john.doe' },
                    fullDay: true,
                    workInAbsence: false,
                }]}
                dateFrom={new Date(Date.UTC(2019, 1, 1))}
                dateTo={new Date(Date.UTC(2019, 1, 7))}
                onOpenShiftEditClick={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with error', () => {
        const error = new Error();
        error.data = {
            message: '',
            detail: 'Invalid input',
        };

        const wrapper = shallow(
            <AbcDutyScheduleEdit__Main
                error={error}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with messages about dutyOnHolidays && dutyOnWeekends', () => {
        const wrapper = shallow(
            <AbcDutyScheduleEdit__Main
                schedules={{
                    '42': {
                        name: 'schedule1',
                        dutyOnHolidays: false,
                        dutyOnWeekends: false,
                        shifts: [{
                            id: 1,
                            start: new Date(Date.UTC(2010, 1, 1)),
                            end: new Date(Date.UTC(2010, 1, 10)),
                            isApproved: true,
                            problemsCount: 1,
                        }],
                    },
                }}
                absences={[]}
                dateFrom={new Date(Date.UTC(2019, 1, 1))}
                dateTo={new Date(Date.UTC(2019, 1, 7))}
                onOpenShiftEditClick={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
