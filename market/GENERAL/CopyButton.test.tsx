import React from 'react';
import { act, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { CopyButton } from './CopyButton';

function customSelect(this: HTMLInputElement) {
  return this.value;
}

function mockedDocumentExec(_: string) {
  return true;
}

describe('<CopyButton />', () => {
  it('renders without errors', () => {
    const value = 'Qwert Qwertievich';

    const app = render(<CopyButton value={value} />);
    const btnClickArea = app.container.getElementsByTagName('svg')[0];
    expect(btnClickArea).toBeTruthy();

    HTMLInputElement.prototype.select = jest.fn(customSelect);
    document.appendChild = jest.fn();
    document.removeChild = jest.fn();
    document.execCommand = jest.fn(mockedDocumentExec);

    act(() => {
      userEvent.click(btnClickArea!);
    });

    expect(HTMLInputElement.prototype.select).toHaveLastReturnedWith('Qwert Qwertievich');
    expect(document.execCommand).toBeCalled();
  });
});
