import React from 'react';
import { mount } from 'enzyme';

import Icon from 'b:icon m:glyph=type-flag';

describe('Icon', () => {
    it('Should render flag icon', () => {
        const wrapper = mount(
            <Icon
                glyph="type-flag"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
