import { screen } from '@testing-library/react';

describe.skip('atoms/transition', () => {
    test('renders component successfully', () => {
        // TODO
        // render(<Transition testId="test-transition" />);

        const element = screen.getByTestId('test-transition');

        expect(element).toBeInTheDocument();
    });
});
