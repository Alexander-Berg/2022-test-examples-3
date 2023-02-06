import { fireEvent, render, screen } from '@testing-library/react';
import React, { ChangeEvent } from 'react';

import { CategoryParameterDto, CategoryParameterType } from 'src/java/definitions';
import { ParameterConditionSelect } from './ParameterConditionSelect';

jest.mock('src/components/Select/Select.tsx', () => {
  return {
    Select: ({
      options,
      value,
      onChange,
    }: {
      options: Array<{ value: number; label: string }>;
      value: number;
      onChange: (v?: { value: number; label: string }) => void;
    }) => {
      function handleChange(event: ChangeEvent<HTMLSelectElement>) {
        const option = options.find(option => option.value.toString() === event.currentTarget.value);
        onChange(option);
      }

      return (
        <select data-testid="select" value={value} onChange={handleChange}>
          {options.map(({ label, value }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      );
    },
  };
});

describe('<ParameterConditionSelect/>', () => {
  it('Renders without errors. Simulate onChange', () => {
    const parameters: CategoryParameterDto[] = [
      {
        type: CategoryParameterType.ENUM,
        options: [
          { id: 0, name: 'name', parameterId: 1 },
          { id: 1, name: 'name2', parameterId: 1 },
        ],
        name: 'param name',
        id: 1,
        xslName: '',
      },
    ];
    let value: { parameter?: number; option?: number } = {
      parameter: 1,
      option: 1,
    };

    const onChange = jest.fn(v => {
      value = v;
    });

    const view = render(<ParameterConditionSelect parameters={parameters} value={value} onChange={onChange} />);

    const [paramInput, optionInput] = screen.getAllByTestId('select');

    fireEvent.change(paramInput, { target: { value: 1 } });
    view.rerender(<ParameterConditionSelect parameters={parameters} value={value} onChange={onChange} />);

    fireEvent.change(optionInput, { target: { value: 0 } });

    expect(value).toEqual({
      parameter: 1,
      value: 0,
    });

    view.rerender(<ParameterConditionSelect parameters={parameters} value={{ parameter: 2 }} />);

    // covers branches when no onChange callback is provided
    fireEvent.change(paramInput, { target: { value: 1 } });
    fireEvent.change(optionInput, { target: { value: 0 } });
    expect(onChange).toBeCalledTimes(2);
  });
});
