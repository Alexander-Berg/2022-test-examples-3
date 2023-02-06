import React from 'react';
import { shallow } from 'enzyme';

import AbcEmployee__NameWrap from 'b:abc-employee e:name-wrap';

describe('AbcEmployee__NameWrap', () => {
    it('Should render employee name wrapper', () => {
        const wrapper = shallow(
            <AbcEmployee__NameWrap>
                hello world
            </AbcEmployee__NameWrap>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
