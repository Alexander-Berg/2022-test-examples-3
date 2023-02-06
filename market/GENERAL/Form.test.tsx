import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { shallow } from 'enzyme';

import { TextInput } from '../SimpleInput';
import { Form } from './Form';

describe('Form', () => {
  it('renders without errors', () => {
    const onSubmit = jest.fn(res => res);
    const app = render(
      <Form onSubmit={onSubmit}>
        <>
          <TextInput name="text" />
          <input type="submit" value="Сохранить" />
        </>
      </Form>
    );

    userEvent.type(app.container.getElementsByTagName('input')[0], 'test');

    userEvent.click(app.getByText('Сохранить'));

    expect(onSubmit).toHaveLastReturnedWith({ text: 'test' });
  });

  it('renders with onChange', () => {
    const onChange = jest.fn(res => res);
    const app = render(
      <Form onSubmit={jest.fn()} onChange={onChange}>
        <>
          <TextInput name="text" />
          <input type="submit" value="Сохранить" />
        </>
      </Form>
    );

    expect(onChange).toBeCalledTimes(0);

    userEvent.type(app.container.getElementsByTagName('input')[0], 'test');

    expect(onChange).toBeCalledTimes(4);
    expect(onChange).toHaveLastReturnedWith({ text: 'test' });
  });

  it('main flow', () => {
    expect(() =>
      shallow(
        <Form onSubmit={() => undefined}>
          <TextInput name="123" />
        </Form>
      )
    ).not.toThrow();
  });
});
