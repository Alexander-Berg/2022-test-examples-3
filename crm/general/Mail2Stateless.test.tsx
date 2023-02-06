import React from 'react';
import { render } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { Attachment } from 'types/Attachment';
import { Mail2Stateless } from './Mail2Stateless';

const fileStub: Attachment = {
  name: 'TestFile',
  id: 0,
  size: '0',
  type: 'Docviewer',
  urlName: 'TestFile.doc',
  extension: '',
};

const getFileStubs = (filesCount: number): Attachment[] => {
  return [...Array(filesCount)].map((_, index) => ({
    ...fileStub,
    id: index,
    size: `${index + 10} кб`,
    name: `${fileStub.name}_${index}`,
  }));
};

describe('Mail2Stateless', () => {
  describe('props.files', () => {
    describe('when defined', () => {
      it('renders files', async () => {
        const { container } = render(
          <TestBed>
            <Mail2Stateless
              isOpen={false}
              onHiddenItemsChange={jest.fn()}
              hiddenFilesCount={0}
              id="id"
              files={getFileStubs(10)}
            />
          </TestBed>,
        );

        expect(container.getElementsByClassName('Mail2__files').length).toBe(1);
      });
    });

    describe('when undefined', () => {
      it("doesn't render files", async () => {
        const { container } = render(
          <Mail2Stateless
            isOpen={false}
            onHiddenItemsChange={jest.fn()}
            hiddenFilesCount={0}
            id="id"
          />,
        );

        expect(container.getElementsByClassName('Mail2__files').length).toBe(0);
      });
    });
  });

  describe('props.isOpen', () => {
    describe('when true', () => {
      it('renders all files', () => {
        const { container } = render(
          <TestBed>
            <Mail2Stateless
              id="id"
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
            <Mail2Stateless
              id="id"
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
