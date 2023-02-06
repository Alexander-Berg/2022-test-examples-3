import { render } from 'enzyme';
import React from 'react';
import { MenuItem } from './MenuItem';

describe('MenuItem', () => {
    it('Should render full MenuItem', () => {
        const wrapper = render(
            <MenuItem
                title="This is MenuItem title"
                description="This is MenuItem test description"
                icon={<span>anyIcon</span>}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Should render MenuItem without description and icon', () => {
        const wrapper = render(
            <MenuItem
                title="This is MenuItem title"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
