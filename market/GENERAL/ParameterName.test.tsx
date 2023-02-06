import React from 'react';
import { fireEvent, act, render } from '@testing-library/react';

import { parameter } from 'src/test/data';
import { ParameterName, getFullParameterName } from './ParameterName';
import { ValueType } from 'src/java/definitions';

const numericParameter = { ...parameter, valueType: ValueType.NUMERIC };
describe('<ParameterName />', () => {
  test('render with comments', async () => {
    const app = render(<ParameterName parameter={parameter} />);
    const name = app.getByText(getFullParameterName(parameter));

    act(() => {
      fireEvent.mouseEnter(name);
    });

    await app.findByText(parameter.comment!);
  });

  test('render with min limit', async () => {
    const parameterWithLimits = { ...numericParameter, minValue: 10 };
    const app = render(<ParameterName parameter={parameterWithLimits} />);
    app.getByText('от 10', { exact: false });
  });

  test('render with max limit', async () => {
    const parameterWithLimits = { ...numericParameter, maxValue: 10 };
    const app = render(<ParameterName parameter={parameterWithLimits} />);
    app.getByText('до 10', { exact: false });
  });

  test('render with all limit', async () => {
    const parameterWithLimits = { ...numericParameter, maxValue: 10, minValue: 10 };
    const app = render(<ParameterName parameter={parameterWithLimits} />);
    app.getByText('от 10 до 10', { exact: false });
  });
});
