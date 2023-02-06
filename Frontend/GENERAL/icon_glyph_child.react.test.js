import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=child';

describe('Icon', () => {
    it('Should render child icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="child"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
