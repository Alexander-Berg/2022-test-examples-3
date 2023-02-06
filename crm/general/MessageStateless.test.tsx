import React from 'react';
import { render } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { getFileStubs } from 'components/AttachmentList/AttachmentList.stubs';
import { MessageStateless } from './MessageStateless';

describe('MessageStateless', () => {
  describe('props.files', () => {
    describe('when defined', () => {
      it('renders files', async () => {
        const { container } = render(
          <TestBed>
            <MessageStateless
              isOpen={false}
              onHiddenItemsChange={jest.fn()}
              hiddenFilesCount={0}
              files={getFileStubs(10)}
            />
          </TestBed>,
        );

        expect(container.getElementsByClassName('Message__files').length).toBe(1);
      });
    });

    describe('when undefined', () => {
      it("doesn't render files", async () => {
        const { container } = render(
          <MessageStateless isOpen={false} onHiddenItemsChange={jest.fn()} hiddenFilesCount={0} />,
        );

        expect(container.getElementsByClassName('Message__files').length).toBe(0);
      });
    });
  });

  describe('props.isOpen', () => {
    describe('when true', () => {
      it('renders all files', () => {
        const { container } = render(
          <TestBed>
            <MessageStateless
              isOpen
              onHiddenItemsChange={jest.fn()}
              hiddenFilesCount={0}
              files={getFileStubs(20)}
            />
          </TestBed>,
        );

        expect(container.getElementsByClassName('AttachmentListStateless_closed').length).toBe(0);
      });
    });

    describe('when false or undefined', () => {
      it("doesn't render all files", () => {
        const { container } = render(
          <TestBed>
            <MessageStateless
              isOpen={false}
              onHiddenItemsChange={jest.fn()}
              hiddenFilesCount={0}
              files={getFileStubs(20)}
            />
          </TestBed>,
        );

        expect(container.getElementsByClassName('AttachmentListStateless_closed').length).toBe(1);
      });
    });
  });
});
