import React from 'react';
import { mount } from 'enzyme';

import Icon from 'b:icon m:glyph=trash';

describe('Icon', () => {
    it('Should render trash icon', () => {
        const wrapper = mount(
            <Icon
                glyph="trash"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
