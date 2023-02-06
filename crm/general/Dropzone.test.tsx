import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { Dropzone } from './Dropzone';

const attachFileStub = new File(['(⌐□_□)'], 'chucknorris.png', { type: 'image/png' });
const attachTextFileStub = new File(['(⌐□_□)'], 'chucknorris.html', { type: 'text/html' });
const zoneStub = {
  text: 'zone text',
  accept: 'zone accept',
  onDrop: jest.fn(),
};

describe('Dropzone', () => {
  describe('props.children', () => {
    describe('when defined', () => {
      it('renders children', () => {
        render(
          <TestBed>
            <Dropzone onDrop={jest.fn()} children={zoneStub.text} />
          </TestBed>,
        );

        expect(screen.getByText(zoneStub.text)).toBeInTheDocument();
      });
    });
  });

  describe('props.onDrop', () => {
    describe('when defined', () => {
      it('calls on drop event', async () => {
        const onDrop = jest.fn();
        render(
          <TestBed>
            <Dropzone onDrop={onDrop} children={zoneStub.text} />
          </TestBed>,
        );

        fireEvent.drop(screen.getByText(zoneStub.text), {
          target: {
            files: [attachFileStub],
          },
        });

        await waitFor(() => {
          expect(onDrop).toBeCalled();
        });
      });
    });
  });

  describe('props.accept', () => {
    describe('when defined', () => {
      it('rejects dropped files with type not equal accept', async () => {
        const onDrop = jest.fn();
        render(
          <TestBed>
            <Dropzone onDrop={onDrop} accept="image/png" children={zoneStub.text} />
          </TestBed>,
        );

        fireEvent.drop(screen.getByText(zoneStub.text), {
          target: {
            files: [attachFileStub, attachTextFileStub],
          },
        });

        await waitFor(() => {
          const acceptedFiles = onDrop.mock.calls[0][0];
          const rejectedFiles = onDrop.mock.calls[0][1];

          expect(acceptedFiles[0].path).toBe(attachFileStub.name);
          expect(rejectedFiles[0].path).toBe(attachTextFileStub.name);
        });
      });
    });

    describe('when undefined', () => {
      it('accepts all dropped files', async () => {
        const onDrop = jest.fn();
        render(
          <TestBed>
            <Dropzone onDrop={onDrop} children={zoneStub.text} />
          </TestBed>,
        );

        fireEvent.drop(screen.getByText(zoneStub.text), {
          target: {
            files: [attachFileStub, attachTextFileStub],
          },
        });

        await waitFor(() => {
          const acceptedFiles = onDrop.mock.calls[0][0];

          expect(acceptedFiles[0].path).toBe(attachFileStub.name);
          expect(acceptedFiles[1].path).toBe(attachTextFileStub.name);
        });
      });
    });
  });

  describe('props.disabled', () => {
    describe('when true', () => {
      it('disables drop zone', async () => {
        const onDrop = jest.fn();
        render(
          <TestBed>
            <Dropzone disabled onDrop={onDrop} children={zoneStub.text} />
          </TestBed>,
        );

        fireEvent.drop(screen.getByText(zoneStub.text), {
          target: {
            files: [attachFileStub, attachTextFileStub],
          },
        });

        await waitFor(() => {
          expect(onDrop).not.toBeCalled();
        });
      });
    });
  });
});
