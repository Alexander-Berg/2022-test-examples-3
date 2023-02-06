import * as React from 'react';
import { render, RenderResult, screen } from '@testing-library/react';

import { ActionBar } from '.';

describe('ActionBar', () => {
    let wrapper: RenderResult;

    afterAll(() => {
        wrapper?.unmount();
    });

    it('should have default className', () => {
        wrapper = render(<ActionBar />, {
            wrapper: ({ children }) => <div data-testid="test">{children}</div>,
        });
        expect(screen.getByTestId('test').children[0]).toHaveClass('ui-action-bar');
    });

    it('should have "has-status" modifier in the className', () => {
        wrapper = render(<ActionBar status="test" />, {
            wrapper: ({ children }) => <div data-testid="test">{children}</div>,
        });
        expect(screen.getByTestId('test').children[0]).toHaveClass('ui-action-bar_has-status');
    });

    it('should render children', () => {
        const buttonNode = (
            <button>
                Submit
            </button>
        );

        wrapper = render(<ActionBar children={buttonNode} />, {
            wrapper: ({ children }) => <div data-testid="test">{children}</div>,
        });
        expect(screen.getByTestId('test').children[0]).toContainElement(screen.getByRole('button'));
    });

    it('should render "status" property', () => {
        const status = 'Lorem Impsum';

        wrapper = render(<ActionBar status={status} />, {
            wrapper: ({ children }) => <div data-testid="test">{children}</div>,
        });
        expect(screen.getByText(status)).toHaveClass('ui-action-bar__status');
    });
});
