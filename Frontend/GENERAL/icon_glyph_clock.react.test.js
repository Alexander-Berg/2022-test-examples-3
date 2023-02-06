import React from 'react';
import { mount } from 'enzyme';

import Icon from 'b:icon m:glyph=clock';

describe('Icon', () => {
    it('Should render clock icon', () => {
        const wrapper = mount(
            <Icon
                glyph="clock"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
