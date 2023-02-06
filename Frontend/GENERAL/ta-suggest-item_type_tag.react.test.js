import React from 'react';
import { mount } from 'enzyme';

import TaSuggestItem from 'b:ta-suggest-item m:type=tag';

describe('Should render ta-suggest-item_type_tag', () => {
    it('default', () => {
        const wrapper = mount(
            <TaSuggestItem
                data={{
                    _type: 'tag',
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
