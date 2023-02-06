import React from 'react';
import { shallow } from 'enzyme';

import AbcEmployee__Name from 'b:abc-employee e:name';

describe('AbcEmployee__Name', () => {
    it('Should render employee name', () => {
        const wrapper = shallow(
            <AbcEmployee__Name>
                hello world
            </AbcEmployee__Name>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
