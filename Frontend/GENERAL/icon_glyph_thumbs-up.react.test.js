import React from 'react';
import { mount } from 'enzyme';

import Icon from 'b:icon m:glyph=thumbs-up';

describe('Icon', () => {
    it('Should render thumbs-up icon', () => {
        const wrapper = mount(
            <Icon
                glyph="thumbs-up"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
