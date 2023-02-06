import * as React from 'react';
import { render } from '@testing-library/react';

import { Badge } from '.';

describe('components/Badge', () => {
    it('should be empty when received nothing', () => {
        expect(render(
            <Badge />,
        ).baseElement.children[0]).toBeEmptyDOMElement();
    });

    it('should render received value', () => {
        const value = 245;

        expect(render(
            <Badge>
                {value}
            </Badge>,
        ).baseElement.children[0].children[0]).toHaveTextContent(String(value));
    });

    it('should has default className', () => {
        const wrapper = render(
            <Badge>
                2
            </Badge>,
        );
        expect(wrapper.baseElement.children[0].children[0]).toHaveClass('ui-badge');
    });

    it('should has "primary" modifier in className', () => {
        expect(render(
            <Badge primary>
                2
            </Badge>,
        ).baseElement.children[0].children[0]).toHaveClass('ui-badge ui-badge_primary');
    });
});
