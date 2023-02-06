import React from 'react';
import {render} from '@testing-library/react';

import {FormatDiff, makeDelta} from './FormatDiff';

describe('FormatDiff', () => {
    test('has noo diff', async () => {
        const screen = render(<FormatDiff />);

        expect(
            screen.getByText(/No changes/i).closest('div'),
        ).toBeInTheDocument();
    });
    test('expected render diff', async () => {
        const screen = render(
            <FormatDiff
                delta={makeDelta({
                    diff: {a: 1, b: 2, c: 3},
                    origin: {a: 2, b: 2, c: 3},
                })}
            />,
        );

        expect(screen.container).toMatchSnapshot();
    });

    test('has chackbox with "Show unchanged values" label', async () => {
        const screen = render(
            <FormatDiff
                delta={makeDelta({
                    diff: {a: 1, b: 2, c: 3},
                    origin: {a: 2, b: 2, c: 3},
                })}
            />,
        );

        const checkbox = await screen.getByLabelText(/Show unchanged values/i);

        expect(checkbox).toBeInTheDocument();
    });
});
