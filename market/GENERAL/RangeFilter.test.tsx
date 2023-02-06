import userEvent from '@testing-library/user-event';
import React from 'react';
import { render, screen } from '@testing-library/react';

import { CategoryParameterType, FilterCondition, ParameterDtoWithValues } from 'src/java/definitions';
import { RangeFilter } from './RangeFilter';

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

const typeNumber = (numberOfInput: 0 | 1, content: string) => {
  const urlInput = screen.getAllByRole('spinbutton')[numberOfInput];
  expect(urlInput).toBeTruthy();
  userEvent.type(urlInput, content);
};

describe('<RangeFilter />', () => {
  it('type two numbers', async () => {
    const onChange = jest.fn();

    render(<RangeFilter parameter={parameter} value={filterCondition} onChange={onChange} />);

    typeNumber(0, '5');
    typeNumber(1, '10');

    expect(onChange).toBeCalledTimes(3);
    expect(onChange.mock.calls[0]).toEqual([{ ...filterCondition, min_value: 5 }]);
    expect(onChange.mock.calls[2]).toEqual([{ ...filterCondition, max_value: 10 }]);
  });
});
