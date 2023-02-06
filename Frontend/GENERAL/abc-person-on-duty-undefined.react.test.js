import React from 'react';
import { shallow } from 'enzyme';

import AbcPersonOnDutyUndefined from 'b:abc-person-on-duty-undefined';

describe('AbcPersonOnDutyUndefined', () => {
    it('Should render resource AbcPersonOnDutyUndefined', () => {
        const wrapper = shallow(
            <AbcPersonOnDutyUndefined />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
