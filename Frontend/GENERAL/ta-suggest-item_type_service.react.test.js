import React from 'react';
import { mount } from 'enzyme';

import TaSuggestItem from 'b:ta-suggest-item m:type=service';

describe('ta-suggest-item_type_service', () => {
    it('Should render ta-suggest-item_type_service', () => {
        const wrapper = mount(
            <TaSuggestItem
                data={{
                    _type: 'service',
                    name: {
                        ru: 'TEST',
                    },
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
