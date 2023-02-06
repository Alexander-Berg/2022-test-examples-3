import React from 'react';
import { shallow } from 'enzyme';

import AbcDndProtal from './index';

describe('AbcDndProtal', () => {
    it('Should render a portal elem', () => {
        const wrapper = shallow(<AbcDndProtal />);

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
