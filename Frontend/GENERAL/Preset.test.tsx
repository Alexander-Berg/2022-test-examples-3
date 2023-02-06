import React from 'react';
import { render } from 'enzyme';

import { Preset } from './Preset';

const items = [
    {
        text: 'text1',
        val: 'val1',
    },
    {
        text: 'text2',
        val: 'val2',
    },
    {
        text: 'text3',
        val: 'val3',
        link: '/link',
    },
];

describe('Should render preset', () => {
    it('default', () => {
        const wrapper = render(
            <Preset
                items={items}
                onSelect={() => {}}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('empty', () => {
        const wrapper = render(
            <Preset
                items={[]}
                onSelect={() => {}}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });
});
