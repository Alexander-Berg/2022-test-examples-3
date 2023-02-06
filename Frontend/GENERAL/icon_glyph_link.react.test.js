import React from 'react';
import { mount } from 'enzyme';

import Icon from 'b:icon m:glyph=link';

describe('Icon', () => {
    it('Should render link icon', () => {
        const wrapper = mount(
            <Icon
                glyph="link"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
