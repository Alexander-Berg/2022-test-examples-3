import React from 'react';
import { render } from 'enzyme';

import { States } from './States';

describe('Should render States Filter', () => {
    it('default', () => {
        const wrapper = render(
            <States
                filterValues={['develop', 'supported']}
                onChange={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
