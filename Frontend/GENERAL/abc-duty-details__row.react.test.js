import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyDetails__Row from 'b:abc-duty-details e:row';

describe('AbcDutyDetails__Row', () => {
    it('Should render abc-duty-details__row', () => {
        const wrapper = shallow(
            <AbcDutyDetails__Row>
                Hello world
            </AbcDutyDetails__Row>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
