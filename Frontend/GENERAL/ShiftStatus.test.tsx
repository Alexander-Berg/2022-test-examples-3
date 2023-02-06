import React from 'react';
import { shallow } from 'enzyme';

import { ShiftStatus } from './ShiftStatus';

describe('ShiftStatus', () => {
    it('Should render schedule edit shift status in unapproved state', () => {
        const wrapper = shallow(
            <ShiftStatus
                id={42}
                person
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render schedule edit shift status in unapproved state with no person', () => {
        const wrapper = shallow(
            <ShiftStatus
                id={42}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render schedule edit shift status in approved state', () => {
        const wrapper = shallow(
            <ShiftStatus
                id={42}
                isApproved
                person
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render schedule edit shift status in approved state with no person', () => {
        const wrapper = shallow(
            <ShiftStatus
                id={42}
                isApproved
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
