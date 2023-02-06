import React from 'react';

import { shallow } from 'enzyme';
import { Header } from './Header';

describe('UI/Header', () => {
    it('should render h3 header', () => {
        const wrapper = shallow(<Header level={3} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render default header', () => {
        const wrapper = shallow(<Header />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render big header', () => {
        const wrapper = shallow(<Header level={1} />);
        expect(wrapper).toMatchSnapshot();
    });
});
