import React from 'react';
import { mount } from 'enzyme';

import Icon from 'b:icon m:glyph=info-circle';

describe('Should render', () => {
    it('info-circle icon', () => {
        const wrapper = mount(
            <Icon
                glyph="info-circle"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
