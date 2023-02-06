import React from 'react';
import { render, waitFor, screen } from '@testing-library/react';
import Overlay from './Overlay';
import { overlayTestId, spinnerTestId } from './Overlay.constants';

describe('Overlay', () => {
  it('renders children inside', () => {
    render(
      <Overlay display hasSpinner={false}>
        test
      </Overlay>,
    );

    expect(screen.getByText('test')).toBeInTheDocument();
  });

  describe('when props.display', () => {
    describe('is falsy', () => {
      it(`doesn't render self`, () => {
        render(<Overlay display={false} />);

        expect(screen.queryByTestId(overlayTestId)).toBeNull();
      });
    });

    describe('is truthy', () => {
      it('renders self', () => {
        render(<Overlay display />);

        expect(screen.getByTestId(overlayTestId)).toBeInTheDocument();
      });
    });

    describe('changes from falsy to truthy', () => {
      it('appears', async () => {
        const { rerender } = render(<Overlay display={false} />);
        rerender(<Overlay display />);
        await waitFor(() => expect(screen.getByTestId(overlayTestId)).toBeInTheDocument());
      });
    });

    describe('changes from truthy to falsy', () => {
      it('disappears', async () => {
        const { rerender } = render(<Overlay display />);
        rerender(<Overlay display={false} />);

        await waitFor(() => {
          expect(screen.queryByTestId(overlayTestId)).not.toBeInTheDocument();
        });
      });
    });
  });

  describe('when props.hasSpinner', () => {
    describe('is falsy', () => {
      it(`doesn't render spinner`, () => {
        render(<Overlay display hasSpinner={false} />);

        expect(screen.queryByTestId(spinnerTestId)).toBeNull();
      });
    });

    describe('is truthy', () => {
      it('renders spinner', () => {
        render(<Overlay display hasSpinner />);

        expect(screen.queryByTestId(spinnerTestId)).toBeInTheDocument();
      });
    });
  });

  describe('when props.borderRadius', () => {
    describe('is undefined', () => {
      it(`doesn't set borderRadius style`, () => {
        render(<Overlay display />);

        const overlayNode = screen.getByTestId(overlayTestId);
        expect(overlayNode.style.borderRadius).toBe('');
      });
    });

    describe('is particular number', () => {
      it(`sets borderRadius style`, () => {
        render(<Overlay display borderRadius={10} />);

        const overlayNode = screen.getByTestId(overlayTestId);
        expect(overlayNode.style.borderRadius).toBe('10px');
      });
    });

    describe('is particular string', () => {
      it(`sets borderRadius style`, () => {
        render(<Overlay display borderRadius="20" />);

        const overlayNode = screen.getByTestId(overlayTestId);
        expect(overlayNode.style.borderRadius).toBe('20px');
      });
    });
  });
});
