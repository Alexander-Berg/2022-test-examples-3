import React from 'react';
import { render } from 'enzyme';

import { ServiceWarningsIcon } from './ServiceWarningsIcon';

describe('Should render icon with type', () => {
    it('has-externals', () => {
        const wrapper = render(
            <ServiceWarningsIcon type="has-external-members" />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
