import React from 'react';
import { render, screen, waitFor, cleanup } from '@testing-library/react/pure';
import { TodoTable } from './TodoTable';
const mockFetchMethod = jest.fn();
const mockSetBaseUrl = jest.fn();
const mockSetExtendedQueryParams = jest.fn();
jest.mock('components/Table/services/TableController', () => {
  return {
    TableController: class {
      tableData: {};
      fetch = mockFetchMethod;
      setBaseUrl = (url) => {
        mockSetBaseUrl(url);
        return this;
      };
      setExtendedQueryParams = mockSetExtendedQueryParams;
    },
  };
});

jest.mock('components/Table/TableEditable', () => {
  return {
    TableEditable: () => <div>MockTable</div>,
  };
});

const extendedParams = { test: 'testValue' };

describe('TodoTable', () => {
  afterEach(() => {
    jest.clearAllMocks();
    cleanup();
  });

  it('renders', () => {
    render(<TodoTable url="/table?" />);
    expect(mockSetBaseUrl).toBeCalledTimes(1);
    expect(mockFetchMethod).toBeCalledTimes(1);
    waitFor(() => {
      expect(screen.getByText('MockTable')).toBeInTheDocument();
    });
  });
  describe('when extendedQueryParams passed', () => {
    it('sets extendedQueryParams ', async () => {
      render(<TodoTable extendedQueryParams={extendedParams} url="/table?" />);
      await waitFor(() => expect(mockSetExtendedQueryParams).toBeCalledTimes(1));
      await waitFor(() => expect(mockSetExtendedQueryParams).toBeCalledWith(extendedParams));
    });
  });
});
