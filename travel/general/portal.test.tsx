import { screen } from '@testing-library/react';

describe.skip('atoms/portal', () => {
    test('renders component successfully', () => {
        // TODO
        // render(<Portal testId="test-portal" />);

        const element = screen.getByTestId('test-portal');

        expect(element).toBeInTheDocument();
    });
});
