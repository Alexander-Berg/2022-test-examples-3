import React from 'react';
import 'babel-polyfill';
import { shallow } from 'enzyme';

import AbcDutyScheduleEditContainer from 'b:abc-duty-schedule-edit-container';

const { BEM_LANG } = process.env;

describe('Should init properly', () => {
    let wrapper = null;
    let updateDutyShiftsRequest = null;

    beforeEach(() => {
        updateDutyShiftsRequest = jest.fn(() => Promise.resolve());

        wrapper = shallow(
            <AbcDutyScheduleEditContainer
                service={{ id: 42 }}
                dutyShifts={{
                    filters: {},
                    data: [],
                }}
                dutyAbsences={{
                    filters: {},
                    data: [],
                }}
                updateDutyShifts={updateDutyShiftsRequest}
            />
        );
    });

    afterEach(() => {
        wrapper.unmount();
    });

    it('Should prepare people data', () => {
        const input = [
            { person: { name: 'A', login: 'a' }, problems_count: 1 },
            { person: { name: 'B', login: 'b' }, problems_count: 2 },
            { person: { name: 'A', login: 'a' }, problems_count: 3 },
        ];
        const actual = wrapper.instance().getPeople(input);
        const expected = {
            a: {
                name: 'A',
                counter: 4,
            },
            b: {
                name: 'B',
                counter: 2,
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should prepare people data with a null person', () => {
        const input = [
            { person: null, problems_count: 1 },
            { person: null, problems_count: 2 },
            { person: null, problems_count: 3 },
        ];
        const actual = wrapper.instance().getPeople(input);
        const expected = {
            '': {
                name: { [BEM_LANG]: 'abc-duty-schedule-edit-container:no-person-on-duty' },
                counter: 6,
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should prepare schedules data', () => {
        const input = [
            {
                id: 1,
                person: { name: 'John Doe', login: 'john.doe' },
                schedule: { id: 1, name: 'schedule1' },
                start_datetime: '2010-01-01T15:00+03:00',
                end_datetime: '2010-01-11T15:00+03:00',
                is_approved: false,
                problems_count: 1,
            },
            {
                id: 2,
                person: { name: 'Mary Poppins', login: 'marry.poppins.yall' },
                schedule: { id: 1, name: 'schedule1' },
                start_datetime: '2010-01-11T15:00+03:00',
                end_datetime: '2010-01-21T15:00+03:00',
                is_approved: false,
                problems_count: 2,
            },
            {
                id: 3,
                person: { name: 'John Doe', login: 'john.doe' },
                schedule: { id: 1, name: 'schedule1' },
                start_datetime: '2010-01-21T15:00+03:00',
                end_datetime: '2010-01-31T15:00+03:00',
                is_approved: false,
                problems_count: 3,
            },
        ];
        const actual = wrapper.instance().getPersonSchedules(input, 'john.doe', [{ id: 1, name: 'schedule1' }]);
        const expected = {
            '1': {
                name: 'schedule1',
                dutyOnHolidays: false,
                dutyOnWeekends: false,
                shifts: [{
                    id: 1,
                    person: { name: 'John Doe', login: 'john.doe' },
                    start: new Date(2010, 0, 1, 15),
                    end: new Date(2010, 0, 11, 15),
                    isApproved: false,
                    problemsCount: 1,
                }, {
                    id: 3,
                    person: { name: 'John Doe', login: 'john.doe' },
                    start: new Date(2010, 0, 21, 15),
                    end: new Date(2010, 0, 31, 15),
                    isApproved: false,
                    problemsCount: 3,
                }],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should prepare schedules data with a null person', () => {
        const input = [
            {
                id: 1,
                person: null,
                schedule: { id: 1, name: 'schedule1' },
                start_datetime: '2010-01-01T15:00+03:00',
                end_datetime: '2010-01-11T15:00+03:00',
                is_approved: false,
                problems_count: 1,
            },
        ];
        const actual = wrapper.instance().getPersonSchedules(input, '', [{ id: 1, name: 'schedule1' }]);
        const expected = {
            '1': {
                name: 'schedule1',
                dutyOnHolidays: false,
                dutyOnWeekends: false,
                shifts: [{
                    id: 1,
                    person: null,
                    start: new Date(2010, 0, 1, 15),
                    end: new Date(2010, 0, 11, 15),
                    isApproved: false,
                    problemsCount: 1,
                }],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should prepare absences data', () => {
        const input = [
            { id: 1, person: { login: 'john.doe' }, workInAbsence: false },
            { id: 2, person: { login: 'mary.poppins.yall' }, workInAbsence: false },
            { id: 3, person: { login: 'john.doe' }, workInAbsence: true },
            { id: 4, person: { login: 'john.doe' }, workInAbsence: false },
        ];
        const actual = wrapper.instance().getPersonAbsences(input, 'john.doe');
        const expected = [
            expect.objectContaining({ id: 1 }),
            expect.objectContaining({ id: 4 }),
        ];

        expect(actual).toEqual(expected);
    });

    it('Should asynchronously throw provided error', () => {
        jest.useFakeTimers();

        const error = new Error();
        error.data = {
            message: '',
            detail: 'Invalid input',
        };

        let wrapper;

        expect(() => {
            wrapper = shallow(
                <AbcDutyScheduleEditContainer
                    service={{ id: 42 }}
                    dutyShifts={{
                        filters: {},
                        data: [],
                        error: error,
                    }}
                    dutyAbsences={{
                        filters: {},
                        data: [],
                    }}
                    updateDutyShifts={() => {}}
                />
            );

            jest.runAllTimers();
        }).toThrow();

        wrapper.unmount();
    });

    it('Should update on filter change', () => {
        const updateRequest = jest.fn(() => Promise.resolve());
        const wrapper = shallow(
            <AbcDutyScheduleEditContainer
                service={{ id: 42 }}
                dutyShifts={{
                    filters: {
                        scheduleId: 1,
                    },
                    data: [],
                }}
                dutyAbsences={{
                    filters: {},
                    data: [],
                }}
                schedules={[{
                    id: 1,
                    name: 'schedule1',
                }, {
                    id: 2,
                    name: 'schedule2',
                }]}
                updateDutyShifts={updateRequest}
            />
        );

        expect(updateRequest).not.toHaveBeenCalledWith(expect.objectContaining({ scheduleId: 2 }));
        wrapper.instance()._onFilterChange([2]);
        expect(updateRequest).toHaveBeenCalledWith(expect.objectContaining({ scheduleId: 2 }));

        wrapper.unmount();
    });
});

describe('Manages selected person', () => {
    let wrapper = null;

    beforeEach(() => {
        wrapper = shallow(
            <AbcDutyScheduleEditContainer
                service={{ id: 42 }}
                dutyShifts={{
                    filters: {},
                    data: [
                        {
                            id: 1,
                            person: { name: { [BEM_LANG]: 'John Doe' }, login: 'john.doe' },
                            schedule: { id: 1, name: 'schedule1' },
                            start: '2010-01-01',
                            end: '2010-01-10',
                            is_approved: false,
                            problems_count: 1,
                        },
                        {
                            id: 2,
                            person: { name: { [BEM_LANG]: 'Small Blue Guy' }, login: 'marry.poppins.yall' },
                            schedule: { id: 1, name: 'schedule1' },
                            start: '2010-01-11',
                            end: '2010-01-20',
                            is_approved: false,
                            problems_count: 2,
                        },
                        {
                            id: 3,
                            person: { name: { [BEM_LANG]: 'Big Blue Guy' }, login: 'tanos' },
                            schedule: { id: 1, name: 'schedule1' },
                            start: '2010-01-21',
                            end: '2010-01-30',
                            is_approved: false,
                            problems_count: 3,
                        },
                    ],
                }}
                dutyAbsences={{
                    filters: {},
                    data: [],
                }}
                updateDutyShifts={() => {}}
            />
        );
    });

    afterEach(() => {
        wrapper.unmount();
    });

    it('Selects the first person in list', () => {
        expect(wrapper.state('selectedPerson')).toEqual('john.doe');
    });

    it('Keeps selected person after update', () => {
        wrapper.setProps({
            dutyShifts: {
                filters: {},
                data: [
                    {
                        id: 10,
                        person: { name: { [BEM_LANG]: 'Medium Blue Guy' }, login: 'jinnee' },
                        schedule: { id: 1, name: 'schedule1' },
                        start: '2010-01-11',
                        end: '2010-01-20',
                        is_approved: false,
                        problems_count: 2,
                    },
                    {
                        id: 1,
                        person: { name: { [BEM_LANG]: 'John Doe' }, login: 'john.doe' },
                        schedule: { id: 1, name: 'schedule1' },
                        start: '2010-01-01',
                        end: '2010-01-10',
                        is_approved: false,
                        problems_count: 1,
                    },
                ],
            },
        });

        expect(wrapper.state('selectedPerson')).toEqual('john.doe');
    });

    it('Selects the first person after update if previously selected is not in list anymore', () => {
        wrapper.setProps({
            dutyShifts: {
                filters: {},
                data: [
                    {
                        id: 10,
                        person: { name: { [BEM_LANG]: 'Medium Blue Guy' }, login: 'jinnee' },
                        schedule: { id: 1, name: 'schedule1' },
                        start: '2010-01-11',
                        end: '2010-01-20',
                        is_approved: false,
                        problems_count: 2,
                    },
                ],
            },
        });

        expect(wrapper.state('selectedPerson')).toEqual('jinnee');
    });
});
