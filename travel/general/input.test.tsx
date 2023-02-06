import { screen } from '@testing-library/react';

describe('molecules/input', () => {
    test('renders component successfully', () => {
        // TODO
        // render(<Input testId="test-input" />);

        const element = screen.getByTestId('test-input');

        expect(element).toBeInTheDocument();
    });
});
