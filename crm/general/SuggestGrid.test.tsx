import React from 'react';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SuggestGrid } from './SuggestGrid';
import { selectedItems, form } from './SuggestGrid.examples/mocks/SuggestGrid.examples.config';

const server = setupServer(
  rest.get('/form', (req, res, ctx) => {
    return res(ctx.json(form));
  }),
);

describe('SuggestGrid', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  it('renders with selected tags', () => {
    render(<SuggestGrid provider="/form" onChange={jest.mock} value={selectedItems} />);

    expect(screen.getByText('Name 1')).toBeInTheDocument();
    expect(screen.getByTestId('suggest-grid-edit')).toBeInTheDocument();
  });

  it('removes item on click', () => {
    const onChange = jest.fn();
    const element = render(
      <SuggestGrid provider="/form" onChange={onChange} value={selectedItems} />,
    );

    userEvent.click(element.container.querySelector('.Bubble button')!);

    expect(onChange).toBeCalledWith(expect.not.arrayContaining([selectedItems[0]]));
  });

  it('selected items are checked in modal grid', async () => {
    const onChange = jest.fn();
    render(<SuggestGrid provider="/form" onChange={onChange} value={selectedItems} />);

    userEvent.click(screen.getByTestId('suggest-grid-edit'));

    const checkboxes = await waitFor(() => screen.getAllByRole('checkbox', { checked: true }));

    expect(checkboxes.length).toBe(2);
  });

  it('calles onChange with selected items', async () => {
    const onChange = jest.fn();
    render(<SuggestGrid provider="/form" onChange={onChange} value={[]} />);

    userEvent.click(screen.getByTestId('suggest-grid-edit'));

    const checkboxes = await waitFor(() => screen.getAllByRole('checkbox'));
    userEvent.click(checkboxes[1]);
    userEvent.click(screen.getByText('Готово'));

    expect(onChange).toBeCalledWith([{ id: '0', name: 'Name 0' }]);
  });
});
