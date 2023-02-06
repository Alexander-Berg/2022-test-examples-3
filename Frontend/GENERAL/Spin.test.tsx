import React from 'react';

import { shallow } from 'enzyme';
import { Spin } from './Spin';

describe('UI/Spin', () => {
    it('should render default Spin', () => {
        const wrapper = shallow(<Spin />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Spin with size l', () => {
        const wrapper = shallow(<Spin size="l" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render progress Spin', () => {
        const wrapper = shallow(<Spin progress />);
        expect(wrapper).toMatchSnapshot();
    });

    it('shoult render Spin with children', () => {
        const wrapper = shallow(<Spin>Children</Spin>);
        expect(wrapper).toMatchSnapshot();
    });
});
