import React from 'react';
import { render, screen } from '@testing-library/react';

import { Example } from '..';

describe('Example', () => {
    test('должен отобразить контент', () => {
        render(
            <Example
                headerText="Header Text"
                creationDate={new Date(2021, 1, 1)}
                text="Text"
            />,
        );

        expect(screen.getByRole('heading')).toHaveTextContent('Header Text');
    });
});
