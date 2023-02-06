import * as React from 'react';
import { render } from '@testing-library/react';

import { ListHeader } from '.';

const text = 'Lorem Impsum';

describe('ListHeader', () => {
    it('should render "text" property', () => {
        expect(render(
            <ListHeader text={text} />,
        ).baseElement.children[0]).toHaveTextContent(text);
    });

    it('should have the className', () => {
        expect(render(
            <ListHeader text={text} />,
        ).baseElement.children[0].children[0]).toHaveClass('ui-list-header');
    });
});
