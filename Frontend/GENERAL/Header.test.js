import React from 'react';
import { shallow } from 'enzyme';

import { Header } from './Header';

describe('Header', () => {
    it('Should render settings form header', () => {
        const wrapper = shallow(
            <Header>Title</Header>,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
