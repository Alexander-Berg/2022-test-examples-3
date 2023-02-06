import React from 'react';

import { shallow } from 'enzyme';
import { Sidebar } from './Sidebar';

describe('UI/Sidebar', () => {
    it('should render default Sidebar', () => {
        const wrapper = shallow(<Sidebar onClose={() => null}>Sidebar content</Sidebar>);
        expect(wrapper).toMatchSnapshot();
    });
});
