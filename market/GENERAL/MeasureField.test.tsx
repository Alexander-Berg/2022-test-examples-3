import userEvent from '@testing-library/user-event';
import React from 'react';
import { render, screen } from '@testing-library/react';
import { Form } from 'react-final-form';

import { CategoryParameterDetailsDto, MeasureDto } from 'src/java/definitions';
import { MeasureField } from './MeasureField';

const measures: MeasureDto[] = [
  {
    id: 1,
    defaultUnitId: 10,
    name: 'Measure',
    units: [
      {
        id: 10,
        name: 'Testik',
      },
      {
        id: 11,
        name: 'Testovich',
      },
    ],
  },
  {
    id: 2,
    defaultUnitId: 21,
    name: 'Measure 2',
    units: [
      {
        id: 20,
        name: 'Qwert',
      },
      {
        id: 21,
        name: 'Qwertievich',
      },
    ],
  },
];

describe('<MeasureFieldForm />', () => {
  it('renders empty data', () => {
    render(
      <Form
        onSubmit={() => undefined}
        render={formProps => {
          return (
            <form onSubmit={formProps.handleSubmit}>
              <MeasureField data={formProps.values as CategoryParameterDetailsDto} measures={[]} />
            </form>
          );
        }}
      />
    );
  });

  it('renders without errors', async () => {
    const onSubmit = jest.fn(values => values);

    render(
      <Form
        onSubmit={onSubmit}
        initialValues={{ measureId: 1, unitId: 11 }}
        render={formProps => {
          return (
            <form onSubmit={formProps.handleSubmit}>
              <MeasureField data={formProps.values as CategoryParameterDetailsDto} measures={measures} />
              <input type="submit" value="Сохранить" />
            </form>
          );
        }}
      />
    );

    userEvent.click(screen.getByText('Measure'));

    const measure2 = screen.getByText('Measure 2');

    userEvent.click(measure2);

    userEvent.click(screen.getByText('Сохранить'));
    expect(onSubmit).toHaveLastReturnedWith({ measureId: 2, unitId: 21 });
  });
});
