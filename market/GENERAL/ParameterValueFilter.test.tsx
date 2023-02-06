import { ParameterValueFilter } from './ParameterValueFilter';
import { categoryData, parameter } from 'src/test/data';
import { VALUE_SOURCE_TEXT } from 'src/entities/parameter/constants';
import { ValueSource } from 'src/java/definitions';
import { ParameterFilter } from 'src/filters';
import { render } from '@testing-library/react';
import React from 'react';

const sources = [ValueSource.FORMALIZATION, ValueSource.MANUAL, ValueSource.RULE];
const parameterFilter: ParameterFilter = {
  parameterId: parameter.id,
  valueSource: sources,
  searchStr: 'пластик',
};

describe('<ParameterValueFilter />', () => {
  test('show one parameter filters', () => {
    const onChange = jest.fn();

    const app = render(
      <ParameterValueFilter
        onChange={onChange}
        filter={{ parameterFilters: [parameterFilter] }}
        currentCategory={categoryData}
      />
    );

    app.getByText(/пластик/);
    sources.forEach(el => {
      app.getByText(VALUE_SOURCE_TEXT[el], { exact: false });
    });
  });
});
