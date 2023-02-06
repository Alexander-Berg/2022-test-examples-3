import * as React from 'react';
import { fireEvent, render } from '@testing-library/react';

import { Loader } from '.';

describe('Loader', () => {
    it('has default className', () => {
        const wrapper = render(
            <Loader />,
        );

        expect(wrapper.baseElement.children[0].children[0]).toHaveClass('ui-loader');
        expect(wrapper.baseElement.children[0].children[0]).not.toHaveClass('ui-loader_solid');
        expect(wrapper.baseElement.children[0].children[0]).not.toHaveClass('ui-loader_visible');
    });

    it('has "solid" modifier in the className', () => {
        const wrapper = render(
            <Loader solid />,
        );

        expect(wrapper.baseElement.children[0].children[0]).toHaveClass('ui-loader_solid');
    });

    it('has "visible" modifier in the className', () => {
        const wrapper = render(
            <Loader visible />,
        );

        expect(wrapper.baseElement.children[0].children[0]).toHaveClass('ui-loader_visible');
    });

    it('show spinner immediately', () => {
        const wrapper = render(
            <Loader />,
        );

        expect(wrapper.baseElement.querySelector('.ui-spinner_visible')).toBeNull();

        wrapper.rerender(<Loader visible />);

        expect(wrapper.baseElement.querySelector('.ui-spinner_visible')).not.toBeNull();
    });

    it('hide spinner when animation has finished', async () => {
        const wrapper = render(
            <Loader visible />,
        );

        expect(wrapper.baseElement.querySelector('.ui-spinner_visible')).not.toBeNull();

        wrapper.rerender(<Loader visible={false} />);
        fireEvent.transitionEnd(wrapper.baseElement.children[0].children[0]);

        expect(wrapper.baseElement.querySelector('.ui-spinner_visible')).toBeNull();
    });
});
