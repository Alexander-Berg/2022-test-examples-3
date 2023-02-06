import React from 'react';
import { mount, shallow } from 'enzyme';

import { ShiftActions } from './ShiftActions';

describe('ShiftActions', () => {
    it('Should render schedule edit shift actions', () => {
        const wrapper = shallow(
            <ShiftActions
                id={42}
                edit={() => { }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});

describe('Should handle actions', () => {
    it('edit', () => {
        const editHandler = jest.fn();

        const wrapper = mount(
            <ShiftActions
                id={42}
                edit={editHandler}
            />,
        );

        wrapper.find('.DutyScheduleEdit__shift-actions-edit').simulate('click');
        expect(editHandler).toBeCalledWith(42);

        wrapper.unmount();
    });
});
