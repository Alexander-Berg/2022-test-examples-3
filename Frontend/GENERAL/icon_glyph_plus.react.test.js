import React from 'react';
import { mount } from 'enzyme';

import Icon from 'b:icon m:glyph=plus';

describe('Icon', () => {
    it('Should render plus icon', () => {
        const wrapper = mount(
            <Icon
                glyph="plus"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
