import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Attachment } from 'types/Attachment';
import { TestBed } from 'components/TestBed';
import { Mail2Stateful } from './Mail2Stateful';

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

window.fetch = jest.fn(
  () =>
    new Promise((resolve) => {
      resolve;
    }),
);

describe('Mail2Stateful', () => {
  describe('when click on expand button', () => {
    it('expands header', () => {
      render(<Mail2Stateful id="id" subject="subject" cc="cc" bcc="bcc" />);

      expect(screen.queryByText('subject')).not.toBeInTheDocument();
      expect(screen.queryByText('cc')).not.toBeInTheDocument();
      expect(screen.queryByText('bcc')).not.toBeInTheDocument();

      fireEvent.click(screen.getByTestId('ExpandButtonText'));

      expect(screen.getByText('subject')).toBeInTheDocument();
      expect(screen.getByText('cc')).toBeInTheDocument();
      expect(screen.getByText('bcc')).toBeInTheDocument();

      fireEvent.click(screen.getByTestId('ExpandButtonText'));

      expect(screen.queryByText('subject')).not.toBeInTheDocument();
      expect(screen.queryByText('cc')).not.toBeInTheDocument();
      expect(screen.queryByText('bcc')).not.toBeInTheDocument();
    });
  });

  describe('props.files', () => {
    describe('when defined', () => {
      it('renders files', async () => {
        const { container } = render(
          <TestBed>
            <Mail2Stateful id="id" files={getFileStubs(10)} />
          </TestBed>,
        );

        expect(container.getElementsByClassName('Mail2__files').length).toBe(1);
      });
    });

    describe('when undefined', () => {
      it("doesn't render files", async () => {
        const { container } = render(<Mail2Stateful id="id" />);

        expect(container.getElementsByClassName('Mail2__files').length).toBe(0);
      });
    });
  });
});
