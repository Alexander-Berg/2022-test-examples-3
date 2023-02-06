import { render, screen } from '@testing-library/react';

import { Button } from './button';

describe('molecules/button', () => {
    test('renders component successfully', () => {
        render(<Button testId="test-button" />);

        const element = screen.getByTestId('test-button');

        expect(element).toBeInTheDocument();
    });
});
