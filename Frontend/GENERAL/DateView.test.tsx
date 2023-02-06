import React from 'react';

import { shallow } from 'enzyme';
import { DateView } from './DateView';

describe('UI/DateView', () => {
    it('should render default DateView', () => {
        const wrapper = shallow(<DateView date="2022-05-25 13:00:31" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render multiline DateView', () => {
        const wrapper = shallow(<DateView multiline date="2022-05-25 13:00:31" />);
        expect(wrapper).toMatchSnapshot();
    });
});
