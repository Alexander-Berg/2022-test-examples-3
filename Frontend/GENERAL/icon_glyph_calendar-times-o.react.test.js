import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=calendar-times-o';

describe('Icon', () => {
    it('Should render calendar-times-o icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="calendar-times-o"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
