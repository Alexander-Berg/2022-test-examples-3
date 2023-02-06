import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from '@testing-library/react';

import { ArrayInput } from '.';
import { DatePicker, NumberInput, TextInput } from 'src/components';

const formatDateToDDMMYYYY = (date: Date) =>
  `${date
    .getDate()
    .toString()
    .padStart(2, '0')}.${(date.getMonth() + 1).toString().padStart(2, '0')}.${date.getFullYear()}`;

describe('<ArrayInput />', () => {
  describe('for <TextInput>', () => {
    let inputs: (string | undefined)[] = ['Test 1', 'Test 2'];

    beforeEach(() => {
      inputs = ['Test 1', 'Test 2'];
      // eslint-disable-next-line testing-library/no-render-in-setup
      render(
        <ArrayInput<string | undefined>
          inputRender={props => <TextInput {...props} />}
          onAddInput={() => {
            inputs = [...inputs, 'New test'];
          }}
          defaultValue="New test"
          value={inputs}
          onChange={newValue => {
            inputs = newValue;
          }}
        />
      );
    });

    it('renders with text inputs', () => {
      const inputNode = screen.getAllByRole('textbox')[1] as HTMLInputElement;

      expect(inputNode.value).toBe('Test 2');
    });

    it('delete first input for text inputs', () => {
      const buttons = screen.getAllByRole('button');
      userEvent.click(buttons[0]);

      expect(inputs.length).toBe(1);
      expect(inputs[0]).toBe('Test 2');
    });

    it('add new input to text inputs', () => {
      const buttons = screen.getAllByRole('button');
      userEvent.click(buttons.pop()!);

      expect(inputs.length).toBe(3);
      expect(inputs[2]).toBe('New test');
    });

    it('change first item for text inputs', () => {
      const input = screen.getByDisplayValue('Test 1') as HTMLInputElement;
      userEvent.type(input, ' new text');
      expect(input.value).toBe('Test 1 new text');
    });
  });

  it('renders with number inputs', () => {
    render(
      <ArrayInput<number | undefined>
        inputRender={props => <NumberInput min={0} {...props} />}
        onAddInput={() => null}
        value={[1, 2, 3]}
        onChange={() => null}
        defaultValue={0}
      />
    );

    expect(screen.getByDisplayValue('2')).toBeInTheDocument();
  });

  it('renders with date inputs', () => {
    render(
      <ArrayInput<Date | null>
        inputRender={({ value, ...props }) => <DatePicker selected={value} {...props} />}
        onAddInput={() => null}
        value={[new Date()]}
        onChange={() => null}
        defaultValue={new Date()}
      />
    );

    const inputNode = screen.getAllByRole('textbox')[0] as HTMLInputElement;
    expect(inputNode.value).toBe(formatDateToDDMMYYYY(new Date()));
  });
});
