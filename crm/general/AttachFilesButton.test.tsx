import React from 'react';
import { render, fireEvent, screen, waitFor } from '@testing-library/react';
import { AttachFilesButton } from './AttachFilesButton';
import { attachFilesServiceStub, attachFileStub } from '../AttachFilesService';

describe('AttachFilesButton', () => {
  describe('props.onClick', () => {
    describe('when defined', () => {
      it('calls on click', () => {
        const onClick = jest.fn();
        render(<AttachFilesButton onClick={onClick} />);

        fireEvent.click(screen.getByRole('button'));

        expect(onClick).toBeCalled();
      });
    });
  });

  describe('props.onFilesChange', () => {
    describe('when defined', () => {
      it('calls with files', async () => {
        const onFilesChange = jest.fn();
        const { container } = render(<AttachFilesButton onFilesChange={onFilesChange} />);

        fireEvent.change(container.querySelector('input')!, {
          target: {
            files: [attachFileStub],
          },
        });

        await waitFor(() => {
          expect(onFilesChange).toBeCalledWith([attachFileStub]);
        });
      });

      it('calls with two same files', async () => {
        const onFilesChange = jest.fn();
        const { container } = render(<AttachFilesButton onFilesChange={onFilesChange} />);

        fireEvent.change(container.querySelector('input')!, {
          target: {
            files: [attachFileStub],
          },
        });

        await waitFor(() => {
          expect(onFilesChange).toBeCalledWith([attachFileStub]);
        });

        onFilesChange.mockClear();

        fireEvent.change(container.querySelector('input')!, {
          target: {
            files: [attachFileStub],
          },
        });

        await waitFor(() => {
          expect(onFilesChange).toBeCalledWith([attachFileStub]);
        });
      });
    });
  });

  describe('props.fileInputRef', () => {
    describe('when defined', () => {
      describe('when on button click', () => {
        it('calls with chosen files', async () => {
          attachFilesServiceStub.addFiles = jest.fn();

          const { container } = render(
            <AttachFilesButton fileInputRef={{ current: attachFilesServiceStub }} />,
          );

          fireEvent.change(container.querySelector('input')!, {
            target: {
              files: [attachFileStub],
            },
          });

          await waitFor(() => {
            expect(attachFilesServiceStub.addFiles).toBeCalledWith([attachFileStub]);
          });
        });
      });
    });
  });
});
