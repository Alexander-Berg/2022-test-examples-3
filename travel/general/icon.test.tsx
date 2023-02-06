import { render, screen } from '@testing-library/react';

import { Icon } from './icon';

describe('atoms/icon', () => {
    test('renders component successfully', () => {
        render(<Icon name="Activities" testId="test-icon" />);

        const element = screen.getByTestId('test-icon');

        expect(element).toBeInTheDocument();
    });
});
