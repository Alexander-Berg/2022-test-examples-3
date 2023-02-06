import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { QuoteWrapper } from './QuoteWrapper';

describe('QuoteWrapper', () => {
  describe('props.expanded', () => {
    describe('when true', () => {
      it('renders quote by default', () => {
        render(<QuoteWrapper expanded html={'html'} />);

        expect(screen.queryByText('html')).toBeInTheDocument();
      });
    });

    describe('when false', () => {
      it("doesn't render quote by default", () => {
        render(<QuoteWrapper expanded={false} html={'html'} />);

        expect(screen.queryByText('html')).not.toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render quote by default", () => {
        render(<QuoteWrapper html={'html'} />);

        expect(screen.queryByText('html')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onQuoteClick', () => {
    describe('when defined', () => {
      it('calls on more button click with id and opposite expanded value', () => {
        const onQuoteClick = jest.fn();

        render(
          <QuoteWrapper id={'id'} expanded={false} onQuoteClick={onQuoteClick} html={'html'} />,
        );

        fireEvent.click(screen.getByRole('button'));

        expect(onQuoteClick).toBeCalledWith(true, 'id');
      });
      it('calls on more button click and show quote', () => {
        const onQuoteClick = jest.fn();

        render(<QuoteWrapper onQuoteClick={onQuoteClick} html={'html'} />);

        expect(screen.queryByText('html')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button'));

        expect(screen.queryByText('html')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't call on more button click", () => {
        const onQuoteClick = jest.fn();

        render(<QuoteWrapper html={'html'} />);

        fireEvent.click(screen.getByRole('button'));

        expect(onQuoteClick).not.toBeCalled();
      });
    });
  });
});
