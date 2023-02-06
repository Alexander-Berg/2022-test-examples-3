import React from 'react';
import { render, screen } from '@testing-library/react';

import { IdSuggest } from '..';

describe('IdSuggest', () => {
    test('должен отобразить контент', () => {
        render(
            <IdSuggest
                url="https://example.com"
            />
        );

        expect(screen.getByText('suggest', { exact: false })).toHaveTextContent('suggest https://example.com');
    });
});
