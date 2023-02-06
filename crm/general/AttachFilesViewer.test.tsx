import React from 'react';
import { render, screen } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { attachFilesServiceInitialStub } from '../AttachFilesService';
import { AttachFilesViewer } from './AttachFilesViewer';

const backendFileStub = {
  id: 123,
  size: 'backend file size',
  name: 'backend file name',
  type: 'backend file type',
  urlName: 'backend file  url name',
};

const fileInputRef = {
  current: {
    addFiles: jest.fn(),
    updateFiles: jest.fn(),
    removeFile: jest.fn(),
    getCurrentFiles: jest.fn(),
  },
};

describe('AttachFilesViewerField', () => {
  describe('props.files', () => {
    describe('when defined', () => {
      it('renders files', () => {
        render(
          <TestBed>
            <AttachFilesViewer {...attachFilesServiceInitialStub} files={[backendFileStub]} />
          </TestBed>,
        );

        expect(screen.getAllByText(backendFileStub.name).length).toBe(1);
      });
    });
  });

  describe('props.fileInputRef', () => {
    describe('when defined', () => {
      it('receives fileInputRef api', () => {
        render(
          <TestBed>
            <AttachFilesViewer
              {...attachFilesServiceInitialStub}
              files={[backendFileStub]}
              fileInputRef={fileInputRef}
            />
          </TestBed>,
        );

        expect(fileInputRef.current.addFiles).toBeDefined();
        expect(fileInputRef.current.getCurrentFiles()).toStrictEqual([backendFileStub]);
        expect(fileInputRef.current.removeFile).toBeDefined();
        expect(fileInputRef.current.updateFiles).toBeDefined();
      });
    });
  });
});
