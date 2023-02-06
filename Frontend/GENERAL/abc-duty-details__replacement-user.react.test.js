import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyDetails__ReplacementUser from 'b:abc-duty-details e:replacement-user';

describe('AbcDutyDetails__ReplacementUser', () => {
    it('Should render abc-duty-details__replacement-user', () => {
        const wrapper = shallow(
            <AbcDutyDetails__ReplacementUser>
                Hello world
            </AbcDutyDetails__ReplacementUser>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
