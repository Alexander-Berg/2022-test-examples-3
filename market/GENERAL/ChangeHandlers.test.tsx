import { fireEvent, render, screen } from '@testing-library/react';
import React, { ChangeEvent } from 'react';

import { CategoryHistoricalDataDto, CategoryHistoricalDataType } from 'src/java/definitions';
import { ChangesHistory } from './ChangesHistory';

jest.mock('src/components/DatePicker/DatePicker.tsx', () => {
  return {
    DatePicker: ({ onChange }: { onChange: (v: Date) => void }) => {
      function handleChange(event: ChangeEvent<HTMLInputElement>) {
        onChange(new Date(event.target.value));
      }

      return <input data-testid="datepicker" onChange={handleChange} />;
    },
  };
});

jest.mock('src/components/Select/Select.tsx', () => {
  return {
    Select: ({
      options,
      value,
      onChange,
    }: {
      options: Array<{ value: string; label: string }>;
      value: string;
      onChange: (v?: { value: string; label: string }) => void;
    }) => {
      function handleChange(event: ChangeEvent<HTMLSelectElement>) {
        const option = options.find(option => option.value === event.currentTarget.value);
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

describe('<ChangesHistory/> change handlers', () => {
  it('call change handlers', () => {
    let value: CategoryHistoricalDataDto = {
      historicalHid: 123,
      modificationDate: 3456789876,
      comment: 'mycomment',
      empty: false,
      type: CategoryHistoricalDataType.SELECTION,
    };
    const view = render(
      <ChangesHistory
        value={value}
        onChange={v => {
          value = v;
        }}
      />
    );

    fireEvent.change(screen.getByDisplayValue(value.comment), { target: { value: 'new comment' } });
    view.rerender(
      <ChangesHistory
        value={value}
        onChange={v => {
          value = v;
        }}
      />
    );

    fireEvent.change(screen.getByDisplayValue(value.historicalHid.toString()), { target: { value: '' } });
    view.rerender(
      <ChangesHistory
        value={value}
        onChange={v => {
          value = v;
        }}
      />
    );

    fireEvent.change(screen.getByDisplayValue('выделение'), {
      target: { value: CategoryHistoricalDataType.SELECTION },
    });

    view.rerender(
      <ChangesHistory
        value={value}
        onChange={v => {
          value = v;
        }}
      />
    );

    fireEvent.change(screen.getByTestId('datepicker'), {
      target: { value: new Date(2077, 10, 7).toISOString() },
    });

    expect(value).toEqual({
      historicalHid: 0,
      modificationDate: 3403458000000,
      comment: 'new comment',
      empty: false,
      type: CategoryHistoricalDataType.SELECTION,
    });

    view.rerender(
      <ChangesHistory
        value={{ ...value, modificationDate: undefined }}
        onChange={v => {
          value = v;
        }}
      />
    );

    fireEvent.change(screen.getByTestId('datepicker'), {
      target: { value: '' },
    });

    expect(value).toEqual({
      historicalHid: 0,
      modificationDate: undefined,
      comment: 'new comment',
      empty: false,
      type: CategoryHistoricalDataType.SELECTION,
    });
  });
});
