import React from 'react';
import { render, screen } from '@testing-library/react';
import { InfoTextBlock } from './InfoTextBlock';

describe('InfoTextBlock', () => {
  describe('props.text', () => {
    describe('when defined', () => {
      it('renders text', () => {
        render(<InfoTextBlock text="test text" />);
        expect(screen.getByText('test text')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render text", () => {
        render(<InfoTextBlock />);
        expect(screen.queryByText('test text')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.theme', () => {
    describe('when is info', () => {
      it('renders info theme', () => {
        const { container } = render(<InfoTextBlock theme="info" />);
        expect(container.querySelector('blockquote')).toHaveClass('InfoTextBlock_theme_info');
      });
    });

    describe('when is warning', () => {
      it('renders warning theme', () => {
        const { container } = render(<InfoTextBlock theme="warning" />);
        expect(container.querySelector('blockquote')).toHaveClass('InfoTextBlock_theme_warning');
      });
    });

    describe('when is error', () => {
      it('renders error theme', () => {
        const { container } = render(<InfoTextBlock theme="error" />);
        expect(container.querySelector('blockquote')).toHaveClass('InfoTextBlock_theme_error');
      });
    });

    describe('when is undefined', () => {
      it('renders info theme', () => {
        const { container } = render(<InfoTextBlock />);
        expect(container.querySelector('blockquote')).toHaveClass('InfoTextBlock_theme_info');
      });
    });
  });
});
