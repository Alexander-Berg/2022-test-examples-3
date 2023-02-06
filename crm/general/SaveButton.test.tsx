import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SaveButton } from './SaveButton';
import { useContextState as actualUseContextState } from '../../../State';
import { useContextState as useContextStateMock } from '../../../State/__mocks__/useContextState';

jest.mock('../../../State');
const useContextState = actualUseContextState as typeof useContextStateMock;

describe('TopBar/SaveButton', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('on click', () => {
    it('calls store.save', () => {
      const mockSave = jest.fn();
      useContextState.mockImplementation(() => ({
        hasChanges: true,
        save: mockSave,
        emit: jest.fn(),
      }));
      render(<SaveButton />);

      userEvent.click(screen.getByRole('button'));

      expect(mockSave).toBeCalledTimes(1);
    });

    it('calls store.emit', () => {
      const mockEmit = jest.fn();
      useContextState.mockImplementation(() => ({
        hasChanges: true,
        save: jest.fn(),
        emit: mockEmit,
      }));
      render(<SaveButton />);

      userEvent.click(screen.getByRole('button'));

      expect(mockEmit).toBeCalledTimes(1);
      expect(mockEmit).toBeCalledWith('save');
    });
  });

  it('has disabled state by default', () => {
    useContextState.mockImplementation(() => ({
      save: jest.fn(),
      emit: jest.fn(),
    }));
    render(<SaveButton />);

    expect(screen.getByRole('button')).toBeDisabled();
  });

  describe('on values change', () => {
    it('changes disabled state', () => {
      useContextState.mockImplementation(() => ({
        hasChanges: true,
        save: jest.fn(),
        emit: jest.fn(),
      }));
      render(<SaveButton />);

      expect(screen.getByRole('button')).toBeEnabled();
    });
  });
});
