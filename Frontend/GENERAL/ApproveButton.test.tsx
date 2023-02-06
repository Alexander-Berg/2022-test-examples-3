import React from 'react';
import { shallow } from 'enzyme';

import { ApproveButton } from './ApproveButton';

describe('DutyApproveButton', () => {
    it('Should render approve button', () => {
        const wrapper = shallow(
            <ApproveButton
                onClick={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
