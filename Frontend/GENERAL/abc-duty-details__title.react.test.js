import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyDetails__Title from 'b:abc-duty-details e:title';

describe('AbcDutyDetails__Title', () => {
    it('Should render abc-duty-details__title', () => {
        const wrapper = shallow(
            <AbcDutyDetails__Title>
                Hello world
            </AbcDutyDetails__Title>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
