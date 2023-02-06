import React from 'react';
import { render } from 'enzyme';

import { Externals } from './Externals';

describe('Should render Externals Filter', () => {
    it('default', () => {
        const wrapper = render(
            <Externals
                filterValues={[true]}
                onChange={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
