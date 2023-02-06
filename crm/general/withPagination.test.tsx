import React from 'react';
import { render, screen, act, waitFor, cleanup, fireEvent } from '@testing-library/react/pure';
import { paginationMock } from './stubs/paginationMock';
import { withPagination } from './withPagination';

const TestComponent = withPagination(() => {
  return <div>TestComponent</div>;
});

const paginationItemClick = jest.fn();

describe('withPagination', () => {
  beforeEach(() => {
    act(() => {
      render(
        <TestComponent paginationItemClick={paginationItemClick} pagination={paginationMock} />,
      );
    });
  });

  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders', async () => {
    await waitFor(() => {
      expect(screen.getByText('9')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText('TestComponent')).toBeInTheDocument();
    });
  });

  describe('when current page is last', () => {
    it('does not renders next button', async () => {
      const newPagination = { ...paginationMock, page: 9 };
      cleanup();
      act(() => {
        render(
          <TestComponent paginationItemClick={paginationItemClick} pagination={newPagination} />,
        );
      });
      await waitFor(() => {
        expect(screen.queryByTestId('paginationNextButton')).not.toBeInTheDocument();
      });
    });
  });

  describe('when click on pagination item', () => {
    it('calls .paginationItemClick', async () => {
      fireEvent.click(screen.getByText('9'));
      await waitFor(() => {
        expect(paginationItemClick).toBeCalledWith({ caption: '9', url: 'url9' });
      });
    });
  });

  describe('when click on "next" button', () => {
    it('calls .paginationItemClick with next page', async () => {
      fireEvent.click(screen.getByTestId('paginationNextButton'));
      await waitFor(() => {
        expect(paginationItemClick).toBeCalledWith({ caption: '4', url: 'url4' });
      });
    });
  });
});
