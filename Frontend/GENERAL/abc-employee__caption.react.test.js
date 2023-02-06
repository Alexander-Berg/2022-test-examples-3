import React from 'react';
import { shallow } from 'enzyme';

import AbcEmployee__Caption from 'b:abc-employee e:caption';

describe('AbcEmployee__Caption', () => {
    it('Should render employee caption', () => {
        const wrapper = shallow(
            <AbcEmployee__Caption>
                hello world
            </AbcEmployee__Caption>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
