import React from 'react';
import { render } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { getFileStubs } from 'components/AttachmentList/AttachmentList.stubs';
import { MessageStateful } from './MessageStateful';

describe('MessageStateful', () => {
  describe('props.files', () => {
    describe('when defined', () => {
      it('renders files', async () => {
        const { container } = render(
          <TestBed>
            <MessageStateful files={getFileStubs(10)} />
          </TestBed>,
        );

        expect(container.getElementsByClassName('Message__files').length).toBe(1);
      });
    });

    describe('when undefined', () => {
      it("doesn't render files", async () => {
        const { container } = render(<MessageStateful />);

        expect(container.getElementsByClassName('Message__files').length).toBe(0);
      });
    });
  });
});
