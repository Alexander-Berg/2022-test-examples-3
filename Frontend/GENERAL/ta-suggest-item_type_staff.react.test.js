import React from 'react';
import { mount } from 'enzyme';

import TaSuggestItem from 'b:ta-suggest-item m:type=staff';

describe('Should render ta-suggest-item_type_staff', () => {
    it('default', () => {
        const wrapper = mount(
            <TaSuggestItem
                data={{
                    _type: 'staff',
                    name: {
                        ru: 'test',
                    },
                    id: 'testlogin',
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
