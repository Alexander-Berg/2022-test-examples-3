import { render, screen } from '@testing-library/react';

import { Card } from './card';

describe('atoms/card', () => {
    test('renders component successfully', () => {
        render(<Card testId="test-card" />);

        const element = screen.getByTestId('test-card');

        expect(element).toBeInTheDocument();
    });
});
