import React from 'react';
import { shallow } from 'enzyme';

import { Tooltip } from './Tooltip';

describe('Should render tooltip', () => {
    it('with text', () => {
        const wrapper = shallow(
            <Tooltip>
                Text
            </Tooltip>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with header', () => {
        const wrapper = shallow(
            <Tooltip>
                <h2>Header</h2>
            </Tooltip>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
