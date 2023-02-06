import React from 'react';
import { Form } from 'react-final-form';
import { act, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { BaseSelectInput } from './BaseSelectInput';

const options = [
  {
    value: 1,
    label: 'Option 1',
  },
  {
    value: 2,
    label: 'Option 2',
  },
];

describe('<BaseSelectInput />', () => {
  it('processes undefined props without errors', () => {
    render(
      <Form
        onSubmit={() => undefined}
        render={props => (
          <form onSubmit={props.handleSubmit}>
            <BaseSelectInput name="selectInput" />
          </form>
        )}
      />
    );
  });

  it('single value process', () => {
    const onSumbit = jest.fn(res => res);
    const app = render(
      <Form
        onSubmit={onSumbit}
        render={props => (
          <form onSubmit={props.handleSubmit}>
            <BaseSelectInput name="selectInput" options={options} />
            <input type="submit" value="Сохранить" />
          </form>
        )}
      />
    );

    act(() => {
      userEvent.click(app.container.getElementsByTagName('input')[0]);
    });

    act(() => {
      userEvent.click(app.getByText('Option 2'));
    });

    userEvent.click(app.getByText('Сохранить'));

    expect(onSumbit).toHaveLastReturnedWith({ selectInput: 2 });
  });

  it('multiple values process', () => {
    const onSumbit = jest.fn(res => res);
    const app = render(
      <Form
        onSubmit={onSumbit}
        render={props => (
          <form onSubmit={props.handleSubmit}>
            <BaseSelectInput name="selectInput" options={options} isMulti />
            <input type="submit" value="Сохранить" />
          </form>
        )}
      />
    );

    act(() => {
      userEvent.click(app.container.getElementsByTagName('input')[0]);
    });

    act(() => {
      userEvent.click(app.getByText('Option 2'));
    });

    userEvent.click(app.getByText('Сохранить'));
    expect(onSumbit).toHaveLastReturnedWith({ selectInput: [2] });

    act(() => {
      userEvent.click(app.container.getElementsByTagName('input')[0]);
    });

    act(() => {
      userEvent.click(app.getByText('Option 1'));
    });

    userEvent.click(app.getByText('Сохранить'));
    expect(onSumbit).toHaveLastReturnedWith({ selectInput: [2, 1] });
  });
});
