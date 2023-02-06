import React from 'react';
import { mount } from 'enzyme';

import TaSuggestItem from 'b:ta-suggest-item m:type=role';

describe('TaSuggestItem', () => {
    it('Should render ta-suggest-item_type_role', () => {
        const wrapper = mount(
            <TaSuggestItem
                data={{
                    _type: 'role',
                    name: {
                        ru: 'TEST'
                    }
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
