import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=plane';

describe('Icon', () => {
    it('Should render plane icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="plane"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
