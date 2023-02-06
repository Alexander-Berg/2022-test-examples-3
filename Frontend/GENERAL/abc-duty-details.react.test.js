import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyDetails from 'b:abc-duty-details';

describe('Should render abc-duty-details', () => {
    const baseProps = {
        dutyShiftId: 0,
        person: {
            name: 'John Doe',
            login: 'john.doe',
        },
        startDate: new Date(2019, 1, 1),
        endDate: new Date(2019, 1, 15),
        canEditDutySettings: false,
        onOpenShiftEditClick: jest.fn(),
        scheduleId: 1000,
    };
    it('simple', () => {
        const wrapper = shallow(
            <AbcDutyDetails
                {...baseProps}
                isApproved
                replacements={[{
                    id: 1,
                    foo: 'bar',
                }]}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('in pending state', () => {
        const wrapper = shallow(
            <AbcDutyDetails
                {...baseProps}
                isApproved={false}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with edit permissions', () => {
        const wrapper = shallow(
            <AbcDutyDetails
                {...baseProps}
                canEditDutySettings
                isApproved={false}
                replacements={[{
                    id: 1,
                    foo: 'bar',
                }]}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('without person', () => {
        const wrapper = shallow(
            <AbcDutyDetails
                {...baseProps}
                canEditDutySettings
                isApproved={false}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with message about dutyOnHolidays', () => {
        const wrapper = shallow(
            <AbcDutyDetails
                {...baseProps}
                isApproved
                dutyOnHolidays
                replacements={[{
                    id: 1,
                    foo: 'bar',
                }]}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with message about dutyOnWeekends', () => {
        const wrapper = shallow(
            <AbcDutyDetails
                {...baseProps}
                isApproved
                dutyOnWeekends
                replacements={[{
                    id: 1,
                    foo: 'bar',
                }]}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with ability to assign to self', () => {
        const wrapper = shallow(
            <AbcDutyDetails
                {...baseProps}
                requesterInDuty
                person={{
                    name: 'John Doe',
                    login: 'john.doe',
                }}
                user={{
                    name: 'Dave Due',
                    login: 'dave.due',
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with ability to assign empty to self', () => {
        const wrapper = shallow(
            <AbcDutyDetails
                {...baseProps}
                requesterInDuty
                person={{
                    name: '',
                    login: '',
                }}
                user={{
                    name: 'Dave Due',
                    login: 'dave.due',
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
