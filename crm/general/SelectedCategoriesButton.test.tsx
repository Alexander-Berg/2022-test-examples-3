import React from 'react';
import { render, screen } from '@testing-library/react';
import { SelectedCategoriesButton } from './SelectedCategoriesButton';
import { useContextState as actualUseContextState } from '../../../State';
import { useContextState as useContextStateMock } from '../../../State/__mocks__/useContextState';

jest.mock('../../../State');
const useContextState = actualUseContextState as typeof useContextStateMock;

describe('TopBar/SelectedCategoriesButton', () => {
  describe('when no selected categories', () => {
    it('disables self', () => {
      useContextState.mockImplementation(() => ({
        tree: {
          finiteSelected: [],
        },
        tabs: {
          current: '',
        },
      }));

      render(<SelectedCategoriesButton />);

      expect(screen.getByRole('button')).toBeDisabled();
    });
  });

  describe('when there are selected categories', () => {
    it('enables self', () => {
      useContextState.mockImplementation(() => ({
        tree: {
          finiteSelected: [1],
        },
        tabs: {
          current: '',
        },
      }));

      render(<SelectedCategoriesButton />);

      expect(screen.getByRole('button')).toBeEnabled();
    });
  });
});
