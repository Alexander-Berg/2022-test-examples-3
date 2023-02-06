import * as React from 'react';
import { render } from '@testing-library/react';

import { Input } from './index';

describe('Input', () => {
    it('should have default className', () => {
        expect(render(
            <Input />,
        ).baseElement.children[0].children[0]).toHaveClass('ui-input');
    });

    it('should have "transparent" modifier in the className', () => {
        expect(render(
            <Input transparent />,
        ).baseElement.children[0].children[0]).toHaveClass('ui-input_transparent');
    });
});
