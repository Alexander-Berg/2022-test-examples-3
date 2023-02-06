import React from 'react';
import { fireEvent, render, waitFor, screen } from '@testing-library/react';

import { UrlRequestModal } from './UrlRequestModal';

describe('UrlRequestModal', () => {
  it('renders', () => {
    render(<UrlRequestModal setUrl={jest.fn()} onClose={jest.fn()} />);
  });
  it('setUrl works', async () => {
    const setUrl = jest.fn();
    render(<UrlRequestModal setUrl={setUrl} onClose={jest.fn()} />);

    const input = screen.getByDisplayValue('') as HTMLInputElement; // get input, modal has only one input and by default it is empty
    const button = screen.getByRole('button') as HTMLButtonElement;

    expect(button.disabled).toBeTruthy();
    expect(input.value).toEqual('');
    fireEvent.change(input!, { target: { value: 'test' } });

    await waitFor(() => expect(input.value).toEqual('test'));
    expect(button!.disabled).toBeFalsy();

    fireEvent.click(button);

    expect(setUrl).toBeCalledTimes(1);
    expect(setUrl).toBeCalledWith('test');
  });
  it('enter submit works', async () => {
    const setUrl = jest.fn();
    render(<UrlRequestModal setUrl={setUrl} onClose={jest.fn()} />);

    const input = screen.getByDisplayValue('') as HTMLInputElement; // get input, modal has only one input and by default it is empty

    fireEvent.change(input, { target: { value: 'test' } });
    await waitFor(() => expect(input.value).toEqual('test'));

    fireEvent.keyPress(input, { key: 'Enter', code: 13, charCode: 13 });
    expect(setUrl).toBeCalledTimes(1);
    expect(setUrl).toBeCalledWith('test');
  });
});
