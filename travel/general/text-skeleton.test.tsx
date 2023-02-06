import { screen } from '@testing-library/react';

describe.skip('atoms/text-skeleton', () => {
    test('renders component successfully', () => {
        // TODO
        // render(<TextSkeleton testId="test-text-skeleton" />);

        const element = screen.getByTestId('test-text-skeleton');

        expect(element).toBeInTheDocument();
    });
});
