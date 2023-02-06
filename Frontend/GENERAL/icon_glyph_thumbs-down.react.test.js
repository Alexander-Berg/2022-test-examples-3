import React from 'react';
import { mount } from 'enzyme';

import Icon from 'b:icon m:glyph=thumbs-down';

describe('Test for thumbs down icon', () => {
    it('Should render thumbs-down icon', () => {
        const wrapper = mount(
            <Icon
                glyph="thumbs-down"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
