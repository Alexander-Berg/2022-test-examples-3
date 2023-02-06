import React from 'react';

import { shallow } from 'enzyme';
import { StaffCardUi } from './StaffCard';

describe('UI/StaffCard', () => {
    it('should render StaffCard', () => {
        const wrapper = shallow(<StaffCardUi login="Login">Children</StaffCardUi>);
        expect(wrapper).toMatchSnapshot();
    });
});
