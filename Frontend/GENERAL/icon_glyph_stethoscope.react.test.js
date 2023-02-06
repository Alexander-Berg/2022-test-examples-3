import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=stethoscope';

describe('Icon', () => {
    it('Should render stethoscope icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="stethoscope"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
