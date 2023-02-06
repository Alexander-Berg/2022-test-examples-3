import React from 'react';
import { render } from '@testing-library/react';
import { mocked } from 'ts-jest/utils';
import { saveStateToIndexedDb } from 'modules/localStorage';
import RumProvider from 'services/Rum';
import { PersistState } from './PersistState';

jest.mock('services/Rum', () => ({
  sendTimeMark: jest.fn(),
}));
const sendTimeMarkMock = mocked(RumProvider.sendTimeMark);

jest.mock('modules/localStorage/localStorage', () => {
  const { saveStateToIndexedDb } = jest.requireActual('modules/localStorage/localStorage');
  return {
    saveStateToIndexedDb: jest.fn(saveStateToIndexedDb),
  };
});
const saveStateToIndexedDbMock = mocked(saveStateToIndexedDb);

describe('PersistState', () => {
  describe('when page unloads', () => {
    it('saves state', () => {
      render(<PersistState />);

      window.dispatchEvent(new Event('beforeunload'));
      expect(saveStateToIndexedDbMock).toBeCalled();
      saveStateToIndexedDbMock.mockClear();
    });

    it('logs localstorage keys', () => {
      render(<PersistState />);

      expect(sendTimeMarkMock).toBeCalled();
      sendTimeMarkMock.mockClear();
    });
  });
});
