import React from 'react';
import { render } from 'enzyme';

import { Tags } from './Tags';

describe('Should render Tags Filter', () => {
    it('default', () => {
        const wrapper = render(
            <Tags
                filterValues={[1, 2]}
                onChange={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
