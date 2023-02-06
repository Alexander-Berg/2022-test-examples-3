import React from 'react';
import { shallow } from 'enzyme';

import Icon from 'b:icon m:glyph=suitcase';

describe('Icon', () => {
    it('Should render suitcase icon', () => {
        const wrapper = shallow(
            <Icon
                glyph="suitcase"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
