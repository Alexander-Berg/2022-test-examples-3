import { fireEvent, render, screen } from '@testing-library/react';
import React, { ChangeEvent } from 'react';

import { Language, ReturnPolicy } from 'src/java/definitions';
import { ReturnConditionSelect, ReturnConditionValue } from './ReturnConditionSelect';

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
        const selectedOption = options.find(option => option.value.toString() === event.target.value);
        onChange(selectedOption);
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

describe('<ReturnConditionSelect/>', () => {
  it('renders without errors', () => {
    let value: ReturnConditionValue = {
      policyId: 124,
      regionId: Language.RUSSIAN,
    };

    const policies: ReturnPolicy[] = [
      {
        id: 1,
        guiText: 'политика 1',
        reportText: 'qwertyj',
      },
      {
        id: 2,
        guiText: 'политика 2',
        reportText: 'testik',
      },
    ];

    const view = render(
      <ReturnConditionSelect
        value={value}
        policies={policies}
        onSubmitValue={() => 1}
        onChange={v => {
          value = v;
        }}
      />
    );

    const inheritButton = screen.getByText('Пронаследовать');
    fireEvent.click(inheritButton);

    const [policySelector, regionSelector] = screen.getAllByTestId('select');
    fireEvent.change(policySelector, { target: { value: 1 } });

    expect(value).toEqual({
      policyId: 1,
      regionId: Language.RUSSIAN,
    });

    view.rerender(
      <ReturnConditionSelect
        value={value}
        policies={policies}
        onChange={v => {
          value = v;
        }}
      />
    );

    fireEvent.change(regionSelector, { target: { value: 'ENGLISH' } });
    expect(value).toEqual({
      policyId: 1,
      regionId: 'ENGLISH',
    });
  });
});
