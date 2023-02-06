import React from 'react';
import { render } from 'enzyme';

import { ServiceStateIcon } from './ServiceStateIcon';

describe('Should render icon with type', () => {
    it('develop', () => {
        const wrapper = render(
            <ServiceStateIcon type="develop" />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('supported', () => {
        const wrapper = render(
            <ServiceStateIcon type="supported" />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('need-info', () => {
        const wrapper = render(
            <ServiceStateIcon type="need-info" />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('closed', () => {
        const wrapper = render(
            <ServiceStateIcon type="closed" />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
