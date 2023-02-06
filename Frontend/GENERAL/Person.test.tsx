import React from 'react';
import { shallow } from 'enzyme';

import { Person } from './Person';

describe('DutyCalendarGrid-Person', () => {
    it('Should render DutyCalendarGrid-Person', () => {
        const wrapper = shallow(
            <Person
                login="login"
                name="name"
                vteams={[]}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render DutyCalendarGrid-Person without person', () => {
        const wrapper = shallow(
            <Person
                login=""
                name=""
                vteams={[]}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
