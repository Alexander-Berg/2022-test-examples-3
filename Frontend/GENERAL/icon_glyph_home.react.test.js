import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=home';

describe('Icon', () => {
    it('Should render home icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="home"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
