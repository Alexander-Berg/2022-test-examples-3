import { screen } from '@testing-library/react';

describe('atoms/modal', () => {
    test('renders component successfully', () => {
        // TODO
        // render(<Modal />);

        const element = screen.getByTestId('test-modal');

        expect(element).toBeInTheDocument();
    });
});
