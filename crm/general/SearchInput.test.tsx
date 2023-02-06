import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SearchInput } from './SearchInput';
import { useContextState as actualUseContextState } from '../../../State';
import { useContextState as useContextStateMock } from '../../../State/__mocks__/useContextState';

jest.mock('../../../State');
const useContextState = actualUseContextState as typeof useContextStateMock;

describe('TopBar/SearchInput', () => {
  it('binds search.text value', () => {
    const mockHandler = jest.fn();
    useContextState.mockImplementation(() => ({
      search: {
        text: 'test',
        handler: mockHandler,
      },
    }));

    render(<SearchInput />);

    const input = screen.getByDisplayValue('test');
    expect(input).toBeInTheDocument();

    userEvent.type(input, '1');
    expect(mockHandler.mock.calls[0][0]).toBe('test1');
  });
});
