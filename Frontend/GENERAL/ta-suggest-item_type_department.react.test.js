import React from 'react';
import { mount } from 'enzyme';

import TaSuggestItem from 'b:ta-suggest-item m:type=department';

describe('Should render ta-suggest-item_type_department', () => {
    it('default', () => {
        const wrapper = mount(
            <TaSuggestItem
                data={{
                    _type: 'department',
                    name: {
                        ru: 'test',
                    },
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
