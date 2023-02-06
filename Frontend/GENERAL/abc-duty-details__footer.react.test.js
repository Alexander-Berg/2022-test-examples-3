import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyDetails__Footer from 'b:abc-duty-details e:footer';

describe('AbcDutyDetails__Footer', () => {
    it('Should render abc-duty-details__footer', () => {
        const wrapper = shallow(
            <AbcDutyDetails__Footer>
                Hello world
            </AbcDutyDetails__Footer>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
