import React from 'react';

import { shallow } from 'enzyme';
import { Shimmer } from './Shimmer';

describe('UI/Shimmer', () => {
    it('should render Shimmer', () => {
        const wrapper = shallow(<Shimmer />);
        expect(wrapper).toMatchSnapshot();
    });
});
