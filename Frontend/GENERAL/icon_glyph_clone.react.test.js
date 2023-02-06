import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=clone';

describe('Icon', () => {
    it('Should render clone icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="clone"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
