import React from 'react';
import { render } from 'enzyme';

import { Footer } from './Footer';

describe('Should render footer', () => {
    it('with enabled submit', () => {
        const wrapper = render(
            <Footer
                onCancel={() => null}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('in loading state', () => {
        const wrapper = render(
            <Footer
                onCancel={() => null}
                disabled
                loading
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });
});
