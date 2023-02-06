import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=random';

describe('Icon', () => {
    it('Should render "random" icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="random"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
