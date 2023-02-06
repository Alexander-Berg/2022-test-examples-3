import React from 'react';
import { mount } from 'enzyme';
import { User } from '~/src/common/context/types';
import { withContext } from '~/src/common/hoc';
import { OnDuty as OnDutyBase } from './OnDuty';

const onDutyShifts = {
    '1': {
        schedule: {
            id: 1,
            name: 'ScheduleName',
            slug: 'schedule-slug',
        },
        shifts: [{
            id: 123,
            isApproved: true,
            start: '2019-11-09',
            startDatetime: '2019-11-09T00:00:00+03:00',
            end: '2019-11-13',
            endDatetime: '2019-11-14T00:00:00+03:00',
            person: null,
            schedule: {
                id: 1,
                name: 'ScheduleName',
                slug: 'schedule-slug',
            },
        }],
    },
    '2': {
        schedule: {
            id: 2,
            name: 'Schedule',
            slug: 'schedule',
        },
        shifts: [{
            id: 111,
            isApproved: true,
            start: '2019-11-10',
            startDatetime: '2019-11-10T00:00:00+03:00',
            end: '2019-11-14',
            endDatetime: '2019-11-15T00:00:00+03:00',
            person: {
                id: 123,
                login: 'somelogin',
                firstName: {
                    ru: 'Name',
                    en: 'Name',
                },
                lastName: {
                    ru: 'Last Name',
                    en: 'Last Name',
                },
            },
            schedule: {
                id: 2,
                name: 'Schedule',
                slug: 'schedule',
            },
        }, {
            id: 321,
            isApproved: true,
            start: '2019-11-08',
            startDatetime: '2019-11-08T00:00:00+03:00',
            end: '2019-11-18',
            endDatetime: '2019-11-19T00:00:00+03:00',
            person: null,
            schedule: {
                id: 2,
                name: 'Schedule',
                slug: 'schedule',
            },
        }],
    },
};

describe('OnDuty', () => {
    const abcContextMock = {
        configs: {
            hosts: {
                centerClient: { protocol: 'https:', hostname: 'center.y-t.ru' },
                staff: { protocol: 'https:', hostname: 'staff.y-t.ru' },
            },
        },
        user: {} as User,
    };

    const OnDuty = withContext(OnDutyBase, abcContextMock);

    it('Should render message when no active shifts', () => {
        const wrapper = mount(
            <OnDuty
                serviceId={123}
                error={undefined}
                onDutyShifts={{}}
            />,
        );

        expect(wrapper.find('.OnDuty-Schedule').length).toBe(0);
        expect(wrapper.find('.OnDuty-NoDataText').length).toBe(1);
    });

    it('Should not render message when loading', () => {
        const wrapper = mount(
            <OnDuty
                serviceId={123}
                error={undefined}
                loading
                onDutyShifts={[]}
            />,
        );

        expect(wrapper.find('.OnDuty-Schedule').length).toBe(0);
        expect(wrapper.find('.OnDuty-NoDataText').length).toBe(0);
    });

    it('Should not render message when error', () => {
        const wrapper = mount(
            <OnDuty
                serviceId={123}
                error={{ data: 'error' }}
                onDutyShifts={{}}
            />,
        );

        expect(wrapper.find('.OnDuty-Schedule').length).toBe(0);
        expect(wrapper.find('.OnDuty-NoDataText').length).toBe(0);
    });

    it('Should render info about active shifts', () => {
        const wrapper = mount(
            <OnDuty
                serviceId={123}
                error={undefined}
                onDutyShifts={onDutyShifts}
            />,
        );

        expect(wrapper.find('.OnDuty-Schedule').length).toBe(2);
        expect(wrapper.find('.OnDuty-ScheduleName').at(1).prop('href')).toBe('/services/123/duty/?role=1&lang=ru');
        expect(wrapper.find('.OnDuty-Schedule .Employee').length).toBe(3);
        expect(wrapper.find('.OnDuty-NoDataText').length).toBe(0);
    });
});
