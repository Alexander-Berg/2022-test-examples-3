import React from 'react';
import { render } from 'enzyme';

import { IsSuspicious } from './IsSuspicious';

describe('Should render IsSuspicious Filter', () => {
    it('default', () => {
        const wrapper = render(
            <IsSuspicious
                filterValues={[true]}
                onChange={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
