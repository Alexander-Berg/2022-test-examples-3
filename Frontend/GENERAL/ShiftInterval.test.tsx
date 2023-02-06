import React from 'react';
import { shallow } from 'enzyme';

import { ShiftInterval } from './ShiftInterval';

describe('ShiftInterval', () => {
    it('Should render schedule edit shift actions', () => {
        const wrapper = shallow(
            <ShiftInterval
                start={new Date(Date.UTC(2010, 1, 1))}
                end={new Date(Date.UTC(2010, 1, 10))}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
