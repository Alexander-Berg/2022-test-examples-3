import React from 'react';

import { shallow } from 'enzyme';
import { StaffAvatar } from '.';

describe('UI/StaffAvatar', () => {
    it('should render StaffAvatar', () => {
        const wrapper = shallow(<StaffAvatar login="Login" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render StaffAvatar with big size', () => {
        const wrapper = shallow(<StaffAvatar login="Login" size={200} />);
        expect(wrapper).toMatchSnapshot();
    });
});
