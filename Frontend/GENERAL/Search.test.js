import React from 'react';
import { render } from 'enzyme';

import { Search } from './Search';

describe('Should render Search Filter', () => {
    it('default', () => {
        const wrapper = render(
            <Search
                filterValue="ABC"
                onChange={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
