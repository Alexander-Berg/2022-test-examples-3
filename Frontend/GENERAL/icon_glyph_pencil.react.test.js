import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=pencil';

describe('Icon', () => {
    it('Should render pencil icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="pencil"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
