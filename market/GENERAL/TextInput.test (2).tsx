import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { Form } from 'react-final-form';

import { TextInput } from './TextInput';

describe('TextInput', () => {
  it('main flow', () => {
    expect(() => render(<Form onSubmit={() => undefined} render={() => <TextInput name="123" />} />)).not.toThrow();
  });

  it('renders with mask', () => {
    const onSubmit = jest.fn(res => res);

    const app = render(
      <Form
        onSubmit={onSubmit}
        render={props => (
          <form onSubmit={props.handleSubmit}>
            <TextInput name="text" mask={/^[0-9]*$/g} />
            <input type="submit" value="Сохранить" />
          </form>
        )}
      />
    );

    const input = app.container.getElementsByTagName('input')[0];

    userEvent.type(input, 'Some text');

    userEvent.click(app.getByText('Сохранить'));

    expect(onSubmit).toHaveLastReturnedWith({});

    userEvent.type(input, '-123');

    userEvent.click(app.getByText('Сохранить'));

    expect(onSubmit).toHaveLastReturnedWith({ text: '123' });
  });
});
