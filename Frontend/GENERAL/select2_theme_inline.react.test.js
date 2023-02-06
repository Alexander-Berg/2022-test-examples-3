import React from 'react';
import { shallow } from 'enzyme';

import Select from 'b:select2 m:type=radio m:theme=inline';

describe('Inline Select Block tests', () => {
    it('Should render inline select without elements', () => {
        const props = {
            items: [],
            val: undefined,
            onChange: () => {},
        };
        const wrapper = shallow(
            <Select
                theme="inline"
                size="s"
                type="radio"
                {...props}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render inline select with some elements', () => {
        const props = {
            items: [
                'first',
                'second',
                'third',
            ],
            val: 'second',
            onChange: () => {},
        };
        const wrapper = shallow(
            <Select
                theme="inline"
                size="s"
                type="radio"
                {...props}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
