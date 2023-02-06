import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=graduation-cap';

describe('Icon', () => {
    it('Should render graduation-cap icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="graduation-cap"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
