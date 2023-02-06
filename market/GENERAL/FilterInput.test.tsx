import userEvent from '@testing-library/user-event';
import React from 'react';
import { render, screen } from '@testing-library/react';

import { CategoryParameterType, FilterCondition, ParameterDtoWithValues } from 'src/java/definitions';
import { FilterInput } from './FilterInput';

const onChange = jest.fn();
const onDelete = jest.fn();
const filterCondition = {
  id: 0,
} as FilterCondition;

const parameter = {
  id: 0,
  type: CategoryParameterType.NUMERIC,
} as ParameterDtoWithValues;

const typeNumber = (numberOfInput: 0 | 1, content: string) => {
  const urlInput = screen.getAllByRole('spinbutton')[numberOfInput];
  expect(urlInput).toBeTruthy();
  userEvent.type(urlInput, content);
};

describe('<FilterInput />', () => {
  it('type in number filter', () => {
    render(<FilterInput value={filterCondition} parameter={parameter} onDelete={onDelete} onChange={onChange} />);

    typeNumber(0, '5');
    typeNumber(1, '10');

    expect(onChange).toBeCalledTimes(3);
    expect(onChange.mock.calls[0]).toEqual([{ ...filterCondition, min_value: 5 }]);
    expect(onChange.mock.calls[2]).toEqual([{ ...filterCondition, max_value: 10 }]);
  });

  it('delete filter', () => {
    render(<FilterInput value={filterCondition} parameter={parameter} onDelete={onDelete} onChange={onChange} />);

    userEvent.click(screen.getByTitle('удалить'));

    expect(onDelete).toBeCalledTimes(1);
  });
});
