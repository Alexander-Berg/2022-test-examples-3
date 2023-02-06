import React from 'react';
import { mount } from 'enzyme';

import Icon from 'b:icon m:glyph=dots';

describe('Render', () => {
    it('Should render dots icon', () => {
        const wrapper = mount(
            <Icon
                glyph="dots"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
