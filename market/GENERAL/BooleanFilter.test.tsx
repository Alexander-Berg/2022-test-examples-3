import React from 'react';
import { render, screen } from '@testing-library/react';
import selectEvent from 'react-select-event';

import { CategoryParameterType, FilterCondition, ParameterDtoWithValues } from 'src/java/definitions';
import { BooleanFilter } from './BooleanFilter';

const filterCondition = {
  id: 0,
} as FilterCondition;

const parameter = {
  id: 0,
  type: CategoryParameterType.BOOLEAN,
  hasBoolNo: true,
  values: {
    1: 'TRUE',
    2: 'FALSE',
  } as { [index: string]: string },
} as ParameterDtoWithValues;

describe('<BooleanFilter />', () => {
  it('select true value', async () => {
    const onChange = jest.fn();

    render(<BooleanFilter parameter={parameter} value={filterCondition} onChange={onChange} />);

    await selectEvent.select(screen.getByRole('textbox'), 'Да');

    expect(onChange).toBeCalledTimes(1);
    expect(onChange).toBeCalledWith({ ...filterCondition, values: [{ value_id: 1 }] });
  });

  it('select false value', async () => {
    const onChange = jest.fn();

    render(<BooleanFilter parameter={parameter} value={filterCondition} onChange={onChange} />);

    await selectEvent.select(screen.getByRole('textbox'), 'Нет');

    expect(onChange).toBeCalledTimes(1);
    expect(onChange).toBeCalledWith({ ...filterCondition, values: [{ value_id: 2 }] });
  });

  it("don't has bool no", () => {
    render(
      <BooleanFilter parameter={{ ...parameter, hasBoolNo: false }} value={filterCondition} onChange={() => null} />
    );

    const yesText = screen.getByText('Да');

    expect(yesText).toBeInTheDocument();
  });
});
