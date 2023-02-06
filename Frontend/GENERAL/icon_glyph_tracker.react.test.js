import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=tracker';

describe('Icon', () => {
    it('Should render tracker icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="tracker"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
