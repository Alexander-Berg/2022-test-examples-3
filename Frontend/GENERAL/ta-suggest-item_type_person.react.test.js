import React from 'react';
import { mount } from 'enzyme';

import TaSuggestItem from 'b:ta-suggest-item m:type=person';

describe('TaSuggestItem', () => {
    it('Should render ta-suggest-item_type_person', () => {
        const wrapper = mount(
            <TaSuggestItem
                data={{
                    _type: 'person',
                    name: {
                        ru: 'TEST'
                    },
                    login: 'TEST@'
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
