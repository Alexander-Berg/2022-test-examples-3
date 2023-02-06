import { render, screen } from '@testing-library/react';

import { Text } from './text';

describe('atoms/text', () => {
    test('renders component successfully', () => {
        render(<Text testId="test-text" />);

        const element = screen.getByTestId('test-text');

        expect(element).toBeInTheDocument();
    });
});
