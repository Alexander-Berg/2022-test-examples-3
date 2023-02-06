import React from 'react';
import { render } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { getFileStubs } from 'components/AttachmentList/AttachmentList.stubs';
import { MessageWithWikiFormatterStateful } from './MessageWithWikiFormatterStateful';

window.fetch = jest.fn(
  () =>
    new Promise((resolve) => {
      resolve;
    }),
);

describe('MessageWithWikiFormatterStateful', () => {
  describe('props.files', () => {
    describe('when defined', () => {
      it('renders files', async () => {
        const { container } = render(
          <TestBed>
            <MessageWithWikiFormatterStateful files={getFileStubs(10)} />
          </TestBed>,
        );

        expect(container.getElementsByClassName('MessageWithWikiFormatter__files').length).toBe(1);
      });
    });

    describe('when undefined', () => {
      it("doesn't render files", async () => {
        const { container } = render(<MessageWithWikiFormatterStateful />);

        expect(container.getElementsByClassName('MessageWithWikiFormatter__files').length).toBe(0);
      });
    });
  });
});
