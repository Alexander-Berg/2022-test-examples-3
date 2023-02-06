import React from 'react';
import { render } from 'enzyme';

import { Footer } from './Footer';

describe('Should render settings form footer', () => {
    it('in creation mode', () => {
        const wrapper = render(
            <Footer
                onCancel={() => {}}
                onRemove={() => {}}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('in edit mode', () => {
        const wrapper = render(
            <Footer
                isEdit
                onCancel={() => {}}
                onRemove={() => {}}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('in loading state', () => {
        const wrapper = render(
            <Footer
                loading
                onCancel={() => {}}
                onRemove={() => {}}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('disabled', () => {
        const wrapper = render(
            <Footer
                disabled
                permittedToEdit
                onCancel={() => {}}
                onRemove={() => {}}
            />,
        );

        expect(wrapper).toMatchSnapshot();
    });
});
