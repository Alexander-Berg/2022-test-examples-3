import React from 'react';
import { setupServer } from 'msw/node';
import { render, waitFor, act, screen } from '@testing-library/react';
import { TableLoadable } from '../TableLoadable';
import { handlers } from './handlers';

describe('TableLoadable', () => {
  const server = setupServer(...handlers);

  beforeAll(() => server.listen());
  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());

  describe('when endpoint contains filled table', () => {
    it('renders table', async () => {
      act(() => {
        render(<TableLoadable url="/filledTable" />);
      });

      expect(screen.queryByTestId('Table')).not.toBeInTheDocument();

      await waitFor(() => {
        expect(screen.getAllByText('LinkText')).toHaveLength(4);
      });
    });
  });

  describe('when endpoint contains empty table', () => {
    it('renders table', async () => {
      act(() => {
        render(<TableLoadable url="/emptyTable" />);
      });

      expect(screen.queryByTestId('EmptyTable')).not.toBeInTheDocument();

      await waitFor(() => {
        expect(screen.getByTestId('EmptyTable')).toBeInTheDocument();
      });
    });
  });

  describe('when endpoint does not implement table interface', () => {
    it('does not render table', async () => {
      act(() => {
        render(<TableLoadable url="/notTableInterface" />);
      });

      expect(screen.queryByTestId('Table')).not.toBeInTheDocument();

      await waitFor(() => {
        expect(screen.queryByTestId('Table')).not.toBeInTheDocument();
      });
    });
  });
});
