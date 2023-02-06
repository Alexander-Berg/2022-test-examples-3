import React from 'react';
import { shallow } from 'enzyme';
import { Shift } from './Shift';

describe('Should render DutyShift', () => {
    it('In pending status', () => {
        const wrapper = shallow(
            <Shift
                label="Hello world"
                onOpenShiftEditClick={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('In approved status', () => {
        const wrapper = shallow(
            <Shift
                isApproved
                label="Hello world"
                onOpenShiftEditClick={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
