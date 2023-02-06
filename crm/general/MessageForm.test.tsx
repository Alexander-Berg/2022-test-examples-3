import { FormApi } from 'final-form';
import { Subject } from 'rxjs';
import React from 'react';
import {
  FactorData,
  FactorsChangeEvent,
  FactorStreamProvider,
} from 'services/FactorService/FactorService';
import { TestBed } from 'components/TestBed';
import { waitFor, render } from '@testing-library/react';
import { insertString, updateAttachedFiles, updateMessageText } from './MessageForm.utils';
import { FileInputApi } from '../../FileInputContext';

const factor: FactorData = {
  factorId: 1,
  templateText: 'Some text',
  files: [
    { id: 1, name: 'Name1' },
    { id: 2, name: 'Name2' },
  ],
};

describe('MessageForm.utils', () => {
  describe('insertString', () => {
    it('inserts string at the beginning', () => {
      const result = insertString('World', 'Hello', { selectionStart: 0, selectionEnd: 0 });

      expect(result).toEqual('HelloWorld');
    });

    it('inserts string at the end', () => {
      const result = insertString('Hello', 'World', { selectionStart: 5, selectionEnd: 5 });

      expect(result).toEqual('HelloWorld');
    });

    it('inserts string in the middle', () => {
      const result = insertString('HelloWorld', ',', { selectionStart: 5, selectionEnd: 5 });

      expect(result).toEqual('Hello,World');
    });

    it('replaces and inserts at the beginning', () => {
      const result = insertString('1ello', 'H', { selectionStart: 0, selectionEnd: 1 });

      expect(result).toEqual('Hello');
    });

    it('replaces and inserts in the middle', () => {
      const result = insertString('HelloExtraWorld', ', ', { selectionStart: 5, selectionEnd: 10 });

      expect(result).toEqual('Hello, World');
    });

    it('works with empty original string', () => {
      const result = insertString('', 'Hello!', { selectionStart: 0, selectionEnd: 0 });

      expect(result).toEqual('Hello!');
    });

    it('works works with empty inserted string', () => {
      const result = insertString('Hello', '', { selectionStart: 0, selectionEnd: 0 });

      expect(result).toEqual('Hello');
    });
  });

  describe('updateAttachedFiles', () => {
    const getCurrentFiles = jest.fn();

    const fileInputApi: FileInputApi = {
      updateFiles: jest.fn(),
      getCurrentFiles,
    };

    it('calls api function with correct args', () => {
      updateAttachedFiles(factor, fileInputApi);

      expect(fileInputApi.updateFiles).toBeCalledWith([1, 2], []);
    });

    it('does not add files with existing file name', () => {
      getCurrentFiles.mockReturnValueOnce([{ id: 1, name: 'Name1' }]);

      updateAttachedFiles(factor, fileInputApi);

      expect(fileInputApi.updateFiles).toBeCalledWith([2], []);
    });
  });

  describe('updateMessageText', () => {
    const getFieldState = jest.fn();
    const change = jest.fn();
    const formApi: Partial<FormApi<unknown>> = {
      getFieldState,
      change,
    };

    it('calls change function with correct args', () => {
      getFieldState.mockReturnValue({ value: '1 2 3' });

      updateMessageText(factor, formApi as FormApi<unknown>, {
        selectionStart: 2,
        selectionEnd: 3,
      });

      expect(change).toBeCalledWith('text', '1 Some text 3');
    });
  });
});

describe('MessageForm', () => {
  it('calls update functions when factor subject changes', async () => {
    const testSubject = new Subject<FactorsChangeEvent>();
    jest.doMock('services/FactorService/FactorService', () => ({
      useFactorStream: () => testSubject,
    }));
    const updateAttachedFiles = jest.fn();
    const updateMessageText = jest.fn();
    jest.doMock('./MessageForm.utils', () => ({
      updateAttachedFiles,
      updateMessageText,
    }));

    const { MessageForm } = require('./MessageForm');

    render(
      <TestBed>
        <FactorStreamProvider>
          <MessageForm />,
        </FactorStreamProvider>
      </TestBed>,
    );

    testSubject.next({ type: 'add', payload: factor });

    await waitFor(() => {
      expect(updateAttachedFiles).toBeCalledTimes(1);
      expect(updateMessageText).toBeCalledTimes(1);
    });
  });
});
