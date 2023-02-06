import React from 'react';
import { mount } from 'enzyme';

import AbcResourceState from 'b:abc-resource-state';

describe('AbcResourceState', () => {
    it('Should render resource state', () => {
        const wrapper = mount(
            <AbcResourceState
                data={{
                    state: 'some-state',
                    state_display: { ru: 'state_name' }
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
