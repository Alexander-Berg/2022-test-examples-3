import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { ExpandButton } from './ExpandButton';

describe('ExpandButton', () => {
  describe('props.text', () => {
    describe('when defined', () => {
      it('renders text', () => {
        render(<ExpandButton text="test text" />);

        expect(screen.queryByText('test text')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render text", () => {
        render(<ExpandButton />);

        expect(screen.getByTestId('ExpandButtonText')).toHaveTextContent('');
      });
    });
  });

  describe('props.onClick', () => {
    describe('when defined', () => {
      it('calls onClick', () => {
        const onExpandChange = jest.fn();
        render(<ExpandButton onClick={onExpandChange} expanded />);

        fireEvent.click(screen.getByTestId('ExpandButtonText'));
        expect(onExpandChange).toBeCalled();
      });
    });
  });

  describe('props.expanded', () => {
    describe('when true', () => {
      it('renders expanded icon', () => {
        const { container } = render(<ExpandButton expanded />);

        expect(container.getElementsByClassName('Icon')[0]).toHaveAttribute(
          'aria-expanded',
          'true',
        );
      });
    });

    describe('when false', () => {
      it("doesn't render expanded icon", () => {
        const { container } = render(<ExpandButton expanded={false} />);

        expect(container.getElementsByClassName('Icon')[0]).toHaveAttribute(
          'aria-expanded',
          'false',
        );
      });
    });
  });
});
