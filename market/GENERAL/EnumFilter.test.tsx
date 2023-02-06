import userEvent from '@testing-library/user-event';
import React from 'react';
import { render, screen } from '@testing-library/react';
import selectEvent from 'react-select-event';

import { CategoryParameterType, FilterCondition, ParameterDtoWithValues } from 'src/java/definitions';
import { EnumFilter } from './EnumFilter';

const filterCondition = {
  id: 0,
} as FilterCondition;

const parameter = {
  id: 0,
  type: CategoryParameterType.ENUM,
  values: {
    1: 'foo',
    2: 'bar',
  } as { [index: string]: string },
} as ParameterDtoWithValues;

function addValue() {
  userEvent.click(screen.getByRole('button', { name: 'Добавить' }));
}

describe('<EnumFilter />', () => {
  it('select two values', async () => {
    const onChange = jest.fn();

    render(<EnumFilter parameter={parameter} value={filterCondition} onChange={onChange} />);

    addValue();

    await selectEvent.select(screen.getByRole('textbox'), 'foo');

    addValue();

    await selectEvent.select(screen.getAllByRole('textbox')[1], 'bar');

    expect(onChange).toBeCalledTimes(4);
    expect(onChange.mock.calls[3]).toEqual([{ ...filterCondition, values: [{ value_id: 1 }, { value_id: 2 }] }]);
  });
});
