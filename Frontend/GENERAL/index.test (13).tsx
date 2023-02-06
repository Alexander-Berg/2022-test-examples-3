import * as React from 'react';
import { render } from '@testing-library/react';

import { Portal } from '.';

describe('Portal', () => {
    it('should render children correctly', () => {
        const children = (
            <h1>Hi there!</h1>
        );

        const wrapper = render(
            <Portal>
                {children}
            </Portal>,
        );

        expect(wrapper.getByRole('heading')).not.toBeEmptyDOMElement();
    });

    it('should create DOM element', () => {
        const wrapper = render(
            <Portal />,
        );

        expect(wrapper.baseElement.children[1]).toBeInstanceOf(HTMLDivElement);
    });

    it('should create DOM element with "elementFactory"', () => {
        const expectedClassName = 'portal-root';

        const elementFactory = () => Object.assign(document.createElement('div'), {
            className: expectedClassName,
        });

        const wrapper = render(
            <Portal elementFactory={elementFactory} />,
        );

        expect(wrapper.baseElement.children[1]).toHaveClass(expectedClassName);
    });

    it('should remove element from the document body when "componentWillUnmount" calls', () => {
        const wrapper = render(
            <Portal />,
        );

        expect(wrapper.baseElement.children[1]).not.toBeUndefined();

        wrapper.unmount();

        expect(wrapper.baseElement.children[1]).toBeUndefined();
    });
});
