import React from 'react';
import { render, screen } from '@testing-library/react';
import { Label } from './Label';

describe('Label', () => {
  describe('props.title', () => {
    describe('when defined', () => {
      it('renders title', () => {
        render(<Label title="test title" />);

        expect(screen.queryByText('test title:')).toBeInTheDocument();
      });
    });
  });

  describe('props.text', () => {
    describe('when defined', () => {
      it('renders text', () => {
        render(<Label title="test title" text="test text" />);

        expect(screen.queryByText('test text')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render text", () => {
        const { container } = render(<Label title="test title" />);

        expect(container.getElementsByClassName('Label__text')[0].innerHTML).toBe('');
      });
    });
  });
});
