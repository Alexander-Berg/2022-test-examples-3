import { act, render } from '@testing-library/react';
import { Form } from 'react-final-form';
import userEvent from '@testing-library/user-event';
import React from 'react';

import { NumberInput } from './NumberInput';

describe('<NumberInput />', () => {
  it('renders without errors', () => {
    const onSubmit = jest.fn(res => res);
    const app = render(
      <Form
        onSubmit={onSubmit}
        render={props => (
          <form onSubmit={props.handleSubmit}>
            <NumberInput name="numberInput" />
            <input type="submit" value="Сохранить" />
          </form>
        )}
      />
    );

    act(() => {
      userEvent.type(app.container.getElementsByTagName('input')[0], 'String');
    });

    userEvent.click(app.getByText('Сохранить'));

    expect(onSubmit).toHaveLastReturnedWith({});

    act(() => {
      userEvent.paste(app.container.getElementsByTagName('input')[0], '123');
    });

    userEvent.click(app.getByText('Сохранить'));

    expect(onSubmit).toHaveLastReturnedWith({ numberInput: 123 });
  });

  it('renders with mask', () => {
    const onSubmit = jest.fn(res => res);

    const app = render(
      <Form
        onSubmit={onSubmit}
        render={props => (
          <form onSubmit={props.handleSubmit}>
            <NumberInput name="numberInput" mask={/^[0-9]*$/g} />
            <input type="submit" value="Сохранить" />
          </form>
        )}
      />
    );

    const input = app.container.getElementsByTagName('input')[0];

    userEvent.type(input, '-0.');

    userEvent.click(app.getByText('Сохранить'));

    expect(onSubmit).toHaveLastReturnedWith({});

    userEvent.type(input, '123');

    userEvent.click(app.getByText('Сохранить'));

    expect(onSubmit).toHaveLastReturnedWith({ numberInput: 123 });
  });
});
