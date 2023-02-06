import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { SearchableSelect } from './SearchableSelect';

describe('<SearchableSelect />', () => {
  it('should contains search input', () => {
    render(<SearchableSelect options={[]} searchText="some text" onSearchTextChange={jest.fn()} />);

    userEvent.click(screen.getByRole('listbox'));

    expect(screen.getByDisplayValue(/some text/i)).toBeInTheDocument();
  });

  it('should hide search input if onSearchTextChange is not passed', async () => {
    render(<SearchableSelect options={[]} searchText="some text" />);

    userEvent.click(screen.getByRole('listbox'));

    expect(screen.queryByText(/some text/i)).toBeNull();
  });

  it('should call onSearchTextChange', async () => {
    const onSearchTextChange = jest.fn();
    render(<SearchableSelect options={[]} searchText="some text" onSearchTextChange={onSearchTextChange} />);

    userEvent.click(screen.getByRole('listbox'));
    userEvent.type(screen.getByRole('textbox'), 'test');

    await waitFor(() => {
      expect(onSearchTextChange).toBeCalledWith('test');
    });

    expect(onSearchTextChange).toBeCalledTimes(1);
  });
});
