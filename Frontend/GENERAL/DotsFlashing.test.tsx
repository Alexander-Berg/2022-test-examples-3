import React from 'react';

import { shallow } from 'enzyme';
import { DotsFlashing } from './DotFlashing';

describe('UI/DotsFlashing', () => {
    it('should render flashing dots', () => {
        const wrapper = shallow(<DotsFlashing />);
        expect(wrapper).toMatchSnapshot();
    });
});
