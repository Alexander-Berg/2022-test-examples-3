import React from 'react';
import { shallow } from 'enzyme';

import AbcPersonOnDutyUndefined__Avatar from 'b:abc-person-on-duty-undefined e:avatar';

describe('AbcPersonOnDutyUndefined__Avatar', () => {
    it('Should render resource AbcPersonOnDutyUndefined', () => {
        const wrapper = shallow(
            <AbcPersonOnDutyUndefined__Avatar />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
