import React from 'react';
import { shallow } from 'enzyme';

import { Interval } from './Interval';

describe('Render calendar grid interval', () => {
    it('Should duty calendar grid interval', () => {
        const wrapper = shallow(
            <Interval
                type="absence"
                start={5}
                view="classic"
                length={5}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
