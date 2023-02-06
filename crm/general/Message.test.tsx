import React from 'react';
import { render, screen } from '@testing-library/react';
import { MessageStateless } from './MessageStateless';

describe('MessageStateless', () => {
  describe('props.text', () => {
    describe('when defined', () => {
      it('renders text', () => {
        render(
          <MessageStateless
            isOpen={false}
            onHiddenItemsChange={jest.fn()}
            hiddenFilesCount={0}
            text={'text'}
          />,
        );

        expect(screen.getByText('text')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render text", () => {
        render(
          <MessageStateless isOpen={false} onHiddenItemsChange={jest.fn()} hiddenFilesCount={0} />,
        );

        expect(screen.queryByText('text')).not.toBeInTheDocument();
      });
    });
  });
});
