import React from 'react';
import { render, screen, act, waitFor, cleanup, fireEvent } from '@testing-library/react/pure';
import { setupServer } from 'msw/node';
import { TableEditable } from './TableEditable';
import { tableData } from './mocks/tableData';
import { handlers } from './mocks/handlers';

jest.mock('react-intl', () => {
  const reactIntl = jest.requireActual('react-intl');

  return {
    ...reactIntl,
    FormattedNumber: ({ value }) => value,
  };
});

const confirm = jest.fn();

const getEditRowButton = () => screen.getByTestId('editRowButton');
const getRemoveRowButton = () => screen.getByTestId('removeRowButton');
const getAddRowButton = () => screen.getByTestId('addRowButton');

const getSaveButton = () => screen.getByRole('button', { name: /сохранить/i });

const getInput = (text = 'Text value') => screen.getByDisplayValue(text);

const server = setupServer(...handlers);

describe('TableEditable', () => {
  beforeAll(() => {
    server.listen();
    window.confirm = confirm;
  });

  beforeEach(async () => {
    await act(async () => {
      render(<TableEditable baseUrl="/table?" tableData={tableData} />);
    });
  });

  afterEach(() => {
    server.resetHandlers();
    jest.clearAllMocks();
    cleanup();
  });

  afterAll(() => server.close());

  it('renders table', async () => {
    await waitFor(() => expect(screen.getByText('Text Value 1')).toBeInTheDocument());
  });

  it('renders add button', async () => {
    await waitFor(() => expect(screen.getByText('Добавить строку')).toBeInTheDocument());
  });

  describe('when clicks edit button', () => {
    beforeEach(async () => {
      await act(async () => {
        fireEvent.click(getEditRowButton());
      });
    });
    it('shows edit form', async () => {
      await waitFor(() => expect(screen.getByText('Редактирование строки')).toBeInTheDocument());
    });

    it('updates row', async () => {
      const input = getInput('Text Value 1');
      await waitFor(() => expect(input).toBeInTheDocument());
      await act(async () => {
        fireEvent.change(input, { target: { value: 'Text Value 2' } });
      });
      await act(async () => {
        fireEvent.click(getSaveButton());
      });
      await waitFor(() => expect(screen.getByText('Text Value 2')).toBeInTheDocument());
    });
  });

  describe('when clicks add button', () => {
    beforeEach(async () => {
      await act(async () => {
        fireEvent.click(getAddRowButton());
      });
    });
    it('shows add form', async () => {
      await waitFor(() => expect(screen.getByText('Добавление строки')).toBeInTheDocument());
    });
    it('creates row', async () => {
      const input = getInput('default Text');
      await waitFor(() => expect(input).toBeInTheDocument());
      await act(async () => {
        fireEvent.change(input, { target: { value: 'New Text' } });
      });
      await act(async () => {
        fireEvent.click(getSaveButton());
      });
      await waitFor(() => expect(screen.getByText('New Text')).toBeInTheDocument());
    });
  });

  describe('when clicks remove button', () => {
    beforeEach(async () => {
      confirm.mockReturnValueOnce(true);
      await act(async () => {
        fireEvent.click(getRemoveRowButton());
      });
    });
    it('shows confirm message', async () => {
      expect(confirm).toBeCalledTimes(1);
    });
    it('removes row', async () => {
      await waitFor(() => expect(screen.queryByText('Text Value 1')).not.toBeInTheDocument());
    });
  });
});
