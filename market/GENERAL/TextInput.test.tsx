import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

import { TextInput } from './TextInput';

describe('<TextInput />', () => {
  it('renders without errors', () => {
    render(<TextInput onChange={jest.fn()} value="" />);
  });

  it('renders with mask', () => {
    const onChange = jest.fn();
    const app = render(<TextInput onChange={onChange} value="" mask={/^[0-9]*$/g} />);

    const input = app.container.getElementsByTagName('input')[0];

    userEvent.type(input, 'some text');

    expect(onChange).toBeCalledTimes(0);

    userEvent.type(input, '123');

    expect(onChange).toBeCalledTimes(3);
  });
});
