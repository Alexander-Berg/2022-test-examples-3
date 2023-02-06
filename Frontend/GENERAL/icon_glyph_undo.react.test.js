import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=undo';

describe('Icon', () => {
    it('Should render undo icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="undo"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
